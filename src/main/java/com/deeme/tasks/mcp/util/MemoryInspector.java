package com.deeme.tasks.mcp.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;

/**
 * Address-based object inspector.
 *
 * Mirrors the DarkBot ObjectInspector address box: given a memory address
 * (hex "0x..." or decimal), reads the object's slots directly out of
 * process memory and returns a JSON snapshot. Used by
 * {@code bot://inspect?address=...}.
 *
 * Access to DarkBot's internal classes is done through
 * {@link java.lang.invoke.MethodHandle} exclusively, because
 * PluginClassLoader blocks {@code java.lang.reflect.*} references in
 * compiled bytecode.
 *
 * DarkBot classes (resolved lazily, never referenced by name in
 * compiled code beyond {@code Class.forName}):
 * <ul>
 * <li>{@code com.github.manolo8.darkbot.Main} – exposes static field
 * {@code API}</li>
 * <li>{@code com.github.manolo8.darkbot.core.IDarkBotAPI} –
 * readInt/readLong/…</li>
 * <li>{@code com.github.manolo8.darkbot.utils.debug.ObjectInspector} – slot
 * enumeration</li>
 * <li>{@code com.github.manolo8.darkbot.utils.debug.ObjectInspector.Slot} –
 * slot data</li>
 * <li>{@code com.github.manolo8.darkbot.core.utils.ByteUtils} –
 * readObjectNameDirect/readStringDirect/ATOM_MASK</li>
 * </ul>
 */
public final class MemoryInspector {

    private static final String MAIN_CLASS = "com.github.manolo8.darkbot.Main";
    private static final String IDARK_BOT_API_CLASS = "com.github.manolo8.darkbot.core.IDarkBotAPI";
    private static final String OBJECT_INSPECTOR_CLASS = "com.github.manolo8.darkbot.utils.debug.ObjectInspector";
    private static final String SLOT_CLASS = "com.github.manolo8.darkbot.utils.debug.ObjectInspector$Slot";
    private static final String SLOT_TYPE_CLASS = "com.github.manolo8.darkbot.utils.debug.ObjectInspector$Slot$Type";
    private static final String BYTE_UTILS_CLASS = "com.github.manolo8.darkbot.core.utils.ByteUtils";

    /** Maximum slots returned in one snapshot. Keeps payload bounded. */
    private static final int MAX_SLOTS = 256;

    private final Resolved resolved;

    public MemoryInspector() {
        this.resolved = Resolved.lazyResolve();
    }

    /**
     * Inspect the object at the given memory address.
     *
     * @param addressText hex ("0x1234"), decimal, or null/empty
     * @return JSON result, or an error node if the address is invalid or
     *         the DarkBot classes cannot be reached.
     */
    public JsonObject inspect(String addressText) {
        JsonObject result = new JsonObject();
        result.addProperty("root", "address");
        result.addProperty("path", addressText == null ? "" : addressText.trim());

        Optional<Long> parsed = parseAddress(addressText);
        if (!parsed.isPresent()) {
            result.addProperty("type", "error");
            result.add("error", errorNode("invalid_address",
                    "Address must be hex (0x...) or decimal, got: " + addressText));
            return result;
        }

        long raw = parsed.get();
        result.addProperty("address", String.format("0x%x", raw));

        if (resolved == null) {
            result.addProperty("type", "error");
            result.add("error", errorNode("darkbot_unavailable",
                    "DarkBot internal classes are not reachable from this plugin"));
            return result;
        }

        try {
            long address = raw & resolved.atomMask;
            String name = (String) resolved.readObjectNameDirect.invoke(address);
            if (name == null || "ERROR".equals(name)) {
                name = "Unknown";
            }
            result.addProperty("type", name);

            Object slotsObj = resolved.getObjectSlots.invoke(address);
            if (!(slotsObj instanceof List)) {
                result.add("value", errorNode("no_slots",
                        "Object at " + result.get("address").getAsString() + " has no readable traits"));
                return result;
            }

            result.add("value", buildSlots((List<?>) slotsObj, address));
        } catch (Throwable t) {
            result.add("value", errorNode("read_error", t.getMessage()));
        }
        return result;
    }

    private JsonElement buildSlots(List<?> slots, long baseAddress) {
        JsonArray arr = new JsonArray();
        int included = 0;
        int total = slots.size();
        for (Object slot : slots) {
            if (included >= MAX_SLOTS) {
                break;
            }
            try {
                arr.add(slotToJson(slot, baseAddress));
            } catch (Throwable t) {
                arr.add(errorNode("slot_error", t.getMessage()));
            }
            included++;
        }
        if (total > MAX_SLOTS) {
            JsonObject marker = new JsonObject();
            marker.addProperty("truncated", true);
            marker.addProperty("included", included);
            marker.addProperty("total", total);
            marker.addProperty("reason", "max_slots");
            arr.add(marker);
        }
        return arr;
    }

    private JsonElement slotToJson(Object slot, long baseAddress) throws Throwable {
        JsonObject obj = new JsonObject();
        obj.addProperty("offset", ((Number) resolved.slotOffset.invoke(slot)).longValue());
        obj.addProperty("name", (String) resolved.slotName.invoke(slot));
        obj.addProperty("type", (String) resolved.slotTypeName.invoke(slot));
        Object template = resolved.slotTemplateType.invoke(slot);
        if (template != null) {
            obj.addProperty("template_type", template.toString());
        }
        obj.addProperty("size", ((Number) resolved.slotSize.invoke(slot)).longValue());
        Object typeEnum = resolved.slotType.invoke(slot);
        obj.addProperty("slot_type", typeEnum == null ? "OBJECT" : typeEnum.toString());
        obj.add("value", readSlotValue(slot, baseAddress, typeEnum));
        return obj;
    }

    private JsonElement readSlotValue(Object slot, long baseAddress, Object typeEnum) throws Throwable {
        long offset = ((Number) resolved.slotOffset.invoke(slot)).longValue();
        long addr = baseAddress + offset;
        String typeName = typeEnum == null ? "OBJECT" : typeEnum.toString();
        switch (typeName) {
            case "INT":
            case "UINT":
            case "BOOLEAN":
                int i = (int) resolved.readInt.invoke(resolved.api, addr);
                return new JsonPrimitive(i);
            case "DOUBLE":
                double d = (double) resolved.readDouble.invoke(resolved.api, addr);
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return new JsonPrimitive(Double.doubleToLongBits(d));
                }
                return new JsonPrimitive(d);
            case "STRING": {
                long ptr = (long) resolved.readLong.invoke(resolved.api, addr);
                if (ptr == 0) {
                    return new JsonPrimitive((String) null);
                }
                String s = (String) resolved.readStringDirect.invoke(ptr);
                return s == null ? new JsonPrimitive("null") : new JsonPrimitive(s);
            }
            default: {
                long ptr = (long) resolved.readLong.invoke(resolved.api, addr);
                if (ptr == 0) {
                    return new JsonPrimitive("null");
                }
                JsonObject o = new JsonObject();
                o.addProperty("address", String.format("0x%x", ptr));
                o.addProperty("is_pointer", true);
                return o;
            }
        }
    }

    private static JsonObject errorNode(String kind, String message) {
        JsonObject node = new JsonObject();
        node.addProperty("error", kind);
        node.addProperty("message", message == null ? "" : message);
        return node;
    }

    /**
     * Parse a hex ("0x..." / "0X...") or decimal address string. Visible
     * for testing and for the self-check runner.
     */
    public static Optional<Long> parseAddress(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        try {
            if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
                return Optional.of(Long.parseUnsignedLong(trimmed.substring(2), 16));
            }
            return Optional.of(Long.parseUnsignedLong(trimmed, 10));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Holds MethodHandles for every DarkBot call site. Resolved once,
     * re-used on every inspect() call.
     */
    private static final class Resolved {
        final Object api;
        final long atomMask;
        final MethodHandle getObjectSlots;
        final MethodHandle readObjectNameDirect;
        final MethodHandle readStringDirect;
        final MethodHandle readInt;
        final MethodHandle readLong;
        final MethodHandle readDouble;
        final MethodHandle slotOffset;
        final MethodHandle slotName;
        final MethodHandle slotTypeName;
        final MethodHandle slotTemplateType;
        final MethodHandle slotSize;
        final MethodHandle slotType;

        private Resolved(Object api, long atomMask, MethodHandle getObjectSlots,
                MethodHandle readObjectNameDirect, MethodHandle readStringDirect,
                MethodHandle readInt, MethodHandle readLong, MethodHandle readDouble,
                MethodHandle slotOffset, MethodHandle slotName,
                MethodHandle slotTypeName, MethodHandle slotTemplateType,
                MethodHandle slotSize, MethodHandle slotType) {
            this.api = api;
            this.atomMask = atomMask;
            this.getObjectSlots = getObjectSlots;
            this.readObjectNameDirect = readObjectNameDirect;
            this.readStringDirect = readStringDirect;
            this.readInt = readInt;
            this.readLong = readLong;
            this.readDouble = readDouble;
            this.slotOffset = slotOffset;
            this.slotName = slotName;
            this.slotTypeName = slotTypeName;
            this.slotTemplateType = slotTemplateType;
            this.slotSize = slotSize;
            this.slotType = slotType;
        }

        static Resolved lazyResolve() {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();

                Class<?> mainClass = Class.forName(MAIN_CLASS);
                Class<?> idarkBotApiClass = Class.forName(IDARK_BOT_API_CLASS);
                Class<?> inspectorClass = Class.forName(OBJECT_INSPECTOR_CLASS);
                Class<?> slotClass = Class.forName(SLOT_CLASS);
                Class<?> byteUtilsClass = Class.forName(BYTE_UTILS_CLASS);
                Class<?> slotTypeEnum = Class.forName(SLOT_TYPE_CLASS);

                MethodHandle apiGetter = findStaticOrPrivate(
                        lookup, mainClass, "API", idarkBotApiClass);
                Object api = apiGetter.invoke();
                if (api == null) {
                    return null;
                }

                MethodHandle atomMaskGetter = findStaticOrPrivate(
                        lookup, byteUtilsClass, "ATOM_MASK", long.class);
                long atomMask = (long) atomMaskGetter.invoke();

                MethodHandle getObjectSlots = findStaticOrPrivate(
                        lookup, inspectorClass, "getObjectSlots",
                        MethodType.methodType(List.class, long.class));
                MethodHandle readObjectNameDirect = findStaticOrPrivate(
                        lookup, byteUtilsClass, "readObjectNameDirect",
                        MethodType.methodType(String.class, long.class));
                MethodHandle readStringDirect = findStaticOrPrivate(
                        lookup, byteUtilsClass, "readStringDirect",
                        MethodType.methodType(String.class, long.class));
                MethodHandle readInt = findVirtualOrPrivate(
                        lookup, idarkBotApiClass, "readInt",
                        MethodType.methodType(int.class, long.class));
                MethodHandle readLong = findVirtualOrPrivate(
                        lookup, idarkBotApiClass, "readLong",
                        MethodType.methodType(long.class, long.class));
                MethodHandle readDouble = findVirtualOrPrivate(
                        lookup, idarkBotApiClass, "readDouble",
                        MethodType.methodType(double.class, long.class));

                MethodHandle slotOffset = findGetterOrPrivate(
                        lookup, slotClass, "offset", long.class);
                MethodHandle slotName = findGetterOrPrivate(
                        lookup, slotClass, "name", String.class);
                MethodHandle slotTypeName = findGetterOrPrivate(
                        lookup, slotClass, "type", String.class);
                MethodHandle slotTemplateType = findGetterOrPrivate(
                        lookup, slotClass, "templateType", String.class);
                MethodHandle slotSize = findGetterOrPrivate(
                        lookup, slotClass, "size", long.class);
                MethodHandle slotType = findGetterOrPrivate(
                        lookup, slotClass, "slotType", slotTypeEnum);

                return new Resolved(api, atomMask, getObjectSlots, readObjectNameDirect,
                        readStringDirect, readInt, readLong, readDouble,
                        slotOffset, slotName, slotTypeName, slotTemplateType, slotSize, slotType);
            } catch (Throwable t) {
                return null;
            }
        }

        private static MethodHandle findStaticOrPrivate(MethodHandles.Lookup lookup,
                Class<?> refc, String name, Class<?> type) {
            try {
                return lookup.findStaticGetter(refc, name, type);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                try {
                    return MethodHandles.privateLookupIn(refc, lookup)
                            .findStaticGetter(refc, name, type);
                } catch (IllegalAccessException | NoSuchFieldException ex) {
                    throw new IllegalStateException(
                            "Cannot access " + refc.getName() + "." + name, ex);
                }
            }
        }

        private static MethodHandle findStaticOrPrivate(MethodHandles.Lookup lookup,
                Class<?> refc, String name, MethodType type) {
            try {
                return lookup.findStatic(refc, name, type);
            } catch (IllegalAccessException | NoSuchMethodException e) {
                try {
                    return MethodHandles.privateLookupIn(refc, lookup)
                            .findStatic(refc, name, type);
                } catch (IllegalAccessException | NoSuchMethodException ex) {
                    throw new IllegalStateException(
                            "Cannot access " + refc.getName() + "." + name, ex);
                }
            }
        }

        private static MethodHandle findVirtualOrPrivate(MethodHandles.Lookup lookup,
                Class<?> refc, String name, MethodType type) {
            try {
                return lookup.findVirtual(refc, name, type);
            } catch (IllegalAccessException | NoSuchMethodException e) {
                try {
                    return MethodHandles.privateLookupIn(refc, lookup)
                            .findVirtual(refc, name, type);
                } catch (IllegalAccessException | NoSuchMethodException ex) {
                    throw new IllegalStateException(
                            "Cannot access " + refc.getName() + "." + name, ex);
                }
            }
        }

        private static MethodHandle findGetterOrPrivate(MethodHandles.Lookup lookup,
                Class<?> refc, String name, Class<?> type) {
            try {
                return lookup.findGetter(refc, name, type);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                try {
                    return MethodHandles.privateLookupIn(refc, lookup)
                            .findGetter(refc, name, type);
                } catch (IllegalAccessException | NoSuchFieldException ex) {
                    throw new IllegalStateException(
                            "Cannot access " + refc.getName() + "." + name, ex);
                }
            }
        }
    }
}
