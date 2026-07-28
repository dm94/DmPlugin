package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Generic raw-memory peeker: reads one or more primitive values at fixed
 * offsets from a memory address. Complements {@code bot://inspect}, which
 * only walks <em>named</em> AS3 trait slots — native fields like
 * {@code Array.length} (offset 40) or {@code Vector.size} (offset 56) have
 * no trait and need a raw read.
 *
 * <p>
 * URI: {@code bot://peek?address=0x...&offsets=OFFSET:TYPE[;OFFSET:TYPE...]}
 * <br>
 * Types: {@code int uint long double bool string atom}.
 * For {@code string}/{@code atom} the value at {@code address+offset} is
 * treated as a pointer (atom-masked for {@code atom}) and dereferenced.
 * </p>
 *
 * <p>
 * Uses {@link java.lang.invoke.MethodHandle} exclusively — the DarkBot
 * {@code PluginClassLoader} blocks {@code java.lang.reflect.*}.
 * </p>
 */
public class PeekResource implements McpResource {

    private static final String MAIN_CLASS = "com.github.manolo8.darkbot.Main";
    private static final String IDARK_BOT_API_CLASS = "com.github.manolo8.darkbot.core.IDarkBotAPI";
    private static final String BYTE_UTILS_CLASS = "com.github.manolo8.darkbot.core.utils.ByteUtils";

    private static final long ATOM_MASK = resolveAtomMask();

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final Resolved resolved = Resolved.lazyResolve();

    @Override
    public String getUri() {
        return "bot://peek";
    }

    @Override
    public String getName() {
        return "Memory Peek";
    }

    @Override
    public String getDescription() {
        return "Read raw primitive values at fixed offsets from a memory "
                + "address. Use for native AS3 fields with no trait slot "
                + "(e.g. Array.length at +40). "
                + "URI: bot://peek?address=0x...&offsets=40:int;56:int";
    }

    @Override
    public String read(String uri) {
        JsonObject result = new JsonObject();
        Map<String, String> params = parseQuery(uri);
        Optional<Long> parsed = parseAddress(params.get("address"));
        if (!parsed.isPresent()) {
            result.addProperty("error", "Missing or invalid 'address'");
            return gson.toJson(result);
        }
        long address = parsed.get();
        result.addProperty("address", String.format("0x%x", address));

        String offsets = params.get("offsets");
        if (offsets == null || offsets.isEmpty() || resolved == null) {
            result.addProperty("error",
                    offsets == null || offsets.isEmpty()
                            ? "Missing 'offsets' (e.g. 40:int;56:int)"
                            : "DarkBot API not reachable");
            return gson.toJson(result);
        }

        JsonArray values = new JsonArray();
        for (String spec : offsets.split("[;,]")) {
            values.add(readOne(spec.trim(), address));
        }
        result.add("values", values);
        return gson.toJson(result);
    }

    private JsonObject readOne(String spec, long base) {
        JsonObject out = new JsonObject();
        out.addProperty("spec", spec);
        String[] parts = spec.split(":");
        if (parts.length != 2) {
            return error(out, "Bad spec, use offset:type");
        }
        long offset;
        try {
            offset = parts[0].startsWith("0x") || parts[0].startsWith("0X")
                    ? Long.decode(parts[0])
                    : Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            return error(out, "Bad offset: " + parts[0]);
        }
        long addr = base + offset;
        try {
            switch (parts[1].toLowerCase()) {
                case "int":
                    out.add("value", new JsonPrimitive((int) resolved.readInt.invoke(resolved.api, addr)));
                    break;
                case "uint":
                    out.add("value", new JsonPrimitive(
                            Integer.toUnsignedString((int) resolved.readInt.invoke(resolved.api, addr))));
                    break;
                case "long":
                    out.add("value", new JsonPrimitive(
                            String.format("0x%x", (long) resolved.readLong.invoke(resolved.api, addr))));
                    break;
                case "double":
                    out.add("value", new JsonPrimitive((double) resolved.readDouble.invoke(resolved.api, addr)));
                    break;
                case "bool":
                case "boolean":
                    out.add("value", new JsonPrimitive((int) resolved.readInt.invoke(resolved.api, addr) != 0));
                    break;
                case "atom": {
                    long ptr = (long) resolved.readLong.invoke(resolved.api, addr) & ATOM_MASK;
                    out.add("value", ptr == 0 ? new JsonPrimitive("null")
                            : new JsonPrimitive(String.format("0x%x", ptr)));
                    break;
                }
                case "string": {
                    long ptr = (long) resolved.readLong.invoke(resolved.api, addr);
                    if (ptr == 0) {
                        out.add("value", new JsonPrimitive((String) null));
                    } else {
                        String s = (String) resolved.readStringDirect.invoke(ptr);
                        out.add("value", new JsonPrimitive(s == null ? "null" : s));
                    }
                    break;
                }
                default:
                    return error(out, "Unknown type: " + parts[1]);
            }
        } catch (Throwable t) {
            return error(out, "read_error: " + t.getMessage());
        }
        return out;
    }

    private JsonObject error(JsonObject out, String message) {
        out.addProperty("error", message);
        return out;
    }

    private Map<String, String> parseQuery(String uri) {
        Map<String, String> params = new HashMap<>();
        int qmark = uri.indexOf('?');
        if (qmark < 0) return params;
        for (String pair : uri.substring(qmark + 1).split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) params.put(pair.substring(0, eq), pair.substring(eq + 1));
        }
        return params;
    }

    /** Parse hex (0x...) or decimal address string. */
    private static Optional<Long> parseAddress(String text) {
        if (text == null) return Optional.empty();
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return Optional.empty();
        try {
            if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
                return Optional.of(Long.parseUnsignedLong(trimmed.substring(2), 16));
            }
            return Optional.of(Long.parseUnsignedLong(trimmed, 10));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static long resolveAtomMask() {
        try {
            Class<?> byteUtils = Class.forName(BYTE_UTILS_CLASS);
            return (long) MethodHandles.lookup().findStaticGetter(byteUtils, "ATOM_MASK", long.class).invoke();
        } catch (Throwable t) {
            return ~7L;
        }
    }

    private static final class Resolved {
        final Object api;
        final MethodHandle readInt;
        final MethodHandle readLong;
        final MethodHandle readDouble;
        final MethodHandle readStringDirect;

        private Resolved(Object api, MethodHandle readInt, MethodHandle readLong,
                MethodHandle readDouble, MethodHandle readStringDirect) {
            this.api = api;
            this.readInt = readInt;
            this.readLong = readLong;
            this.readDouble = readDouble;
            this.readStringDirect = readStringDirect;
        }

        static Resolved lazyResolve() {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                Class<?> mainClass = Class.forName(MAIN_CLASS);
                Class<?> apiClass = Class.forName(IDARK_BOT_API_CLASS);
                Class<?> byteUtils = Class.forName(BYTE_UTILS_CLASS);
                Object api = lookup.findStaticGetter(mainClass, "API", apiClass).invoke();
                if (api == null)
                    return null;
                return new Resolved(api,
                        findVirtual(lookup, apiClass, "readInt", int.class),
                        findVirtual(lookup, apiClass, "readLong", long.class),
                        findVirtual(lookup, apiClass, "readDouble", double.class),
                        findStatic(lookup, byteUtils, "readStringDirect",
                                MethodType.methodType(String.class, long.class)));
            } catch (Throwable t) {
                return null;
            }
        }

        private static MethodHandle findVirtual(MethodHandles.Lookup l, Class<?> c, String n, Class<?> ret)
                throws NoSuchMethodException, IllegalAccessException {
            MethodType t = MethodType.methodType(ret, long.class);
            try {
                return l.findVirtual(c, n, t);
            } catch (Throwable e) {
                return MethodHandles.privateLookupIn(c, l).findVirtual(c, n, t);
            }
        }

        private static MethodHandle findStatic(MethodHandles.Lookup l, Class<?> c, String n, MethodType t)
                throws NoSuchMethodException, IllegalAccessException {
            try {
                return l.findStatic(c, n, t);
            } catch (Throwable e) {
                return MethodHandles.privateLookupIn(c, l).findStatic(c, n, t);
            }
        }
    }
}
