package com.deeme.tasks.mcp.conditions;

import com.deeme.tasks.mcp.util.Json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.deeme.tasks.mcp.resources.McpResource;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;

public class ConditionSchemaResource implements McpResource {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String DARKBOT_PKG = "com.github.manolo8.darkbot.config.actions";
    private static final String DESCRIPTION_KEY = "description";

    private volatile String cachedSchema;
    private volatile String cachedError;

    @Override
    public String getUri() {
        return "conditions://schema";
    }

    @Override
    public String getName() {
        return "Condition Schema";
    }

    @Override
    public String getDescription() {
        return "Complete schema of all available DarkBot condition types, values, and enums for the condition DSL";
    }

    @Override
    public String read(String uri) {
        if (cachedSchema != null)
            return cachedSchema;
        if (cachedError != null)
            return cachedError;

        try {
            JsonObject schema = buildSchema();
            String json = GSON.toJson(schema);
            cachedSchema = json;
            return json;
        } catch (Exception e) {
            JsonObject err = new JsonObject();
            Json.put(err, "error", "Failed to load condition schema: " + e.getMessage());
            String json = GSON.toJson(err);
            cachedError = json;
            return json;
        }
    }

    private JsonObject buildSchema() throws Exception {
        JsonObject schema = new JsonObject();
        Json.put(schema, "format", "Condition DSL uses function-call syntax: name(arg1, arg2, ...)");
        Json.put(schema, DESCRIPTION_KEY,
                "Conditions are used in SAB config and Extra Actions. " +
                        "They return ALLOW, DENY, or ABSTAIN (three-valued logic). " +
                        "ABSTAIN acts as neutral (not DENY).");

        JsonObject results = new JsonObject();
        Json.put(results, "ALLOW", "Condition met (true)");
        Json.put(results, "DENY", "Condition failed (false)");
        Json.put(results, "ABSTAIN", "Neutral - data unavailable, treated as not-DENY");
        schema.add("result_types", results);

        Map<String, Object> valuesMap = getValuesMap();

        JsonArray conditions = new JsonArray();
        JsonArray valueTypes = new JsonArray();
        JsonObject enums = new JsonObject();

        for (Map.Entry<String, Object> entry : valuesMap.entrySet()) {
            try {
                JsonObject item = describeMeta(entry.getValue(), enums);
                if (item != null) {
                    String returnType = item.get("returnType").getAsString();
                    if ("Condition.Result".equals(returnType)) {
                        conditions.add(item);
                    } else {
                        valueTypes.add(item);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        schema.add("conditions", conditions);
        schema.add("values", valueTypes);
        schema.add("enums", enums);
        return schema;
    }

    private Map<String, Object> getValuesMap() throws Exception {
        Class<?> valuesClass = Class.forName(DARKBOT_PKG + ".parser.Values");
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        try {
            MethodHandle getter = lookup.findStaticGetter(valuesClass, "VALUES", Map.class);
            return invoke(getter);
        } catch (IllegalAccessException e) {
            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(valuesClass, lookup);
            MethodHandle getter = privateLookup.findStaticGetter(valuesClass, "VALUES", Map.class);
            return invoke(getter);
        }
    }

    private JsonObject describeMeta(Object meta, JsonObject enums) throws Exception {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<?> metaClass = meta.getClass();

        MethodHandle getVal = lookup.findGetter(metaClass, "valueData", Object.class);
        MethodHandle getCls = lookup.findGetter(metaClass, "clazz", Class.class);
        MethodHandle getTyp = lookup.findGetter(metaClass, "type", Class.class);
        MethodHandle getPar = lookup.findGetter(metaClass, "params", Object.class);

        Object valueData = invoke(getVal, meta);
        Class<?> clazz = (Class<?>) invoke(getCls, meta);
        Class<?> type = (Class<?>) invoke(getTyp, meta);
        Object params = invoke(getPar, meta);

        String name = callAnnotationMethod(valueData, "name", lookup);
        String description = callAnnotationMethod(valueData, DESCRIPTION_KEY, lookup);
        String example = callAnnotationMethod(valueData, "example", lookup);

        JsonObject item = new JsonObject();
        Json.put(item, "name", name);
        Json.put(item, DESCRIPTION_KEY, description);
        Json.put(item, "example", example);
        Json.put(item, "returnType", type.getSimpleName());
        Json.put(item, "className", clazz.getSimpleName());

        Class<?> parserClass = Class.forName(DARKBOT_PKG + ".Parser");
        Json.put(item, "customParser", parserClass.isAssignableFrom(clazz));

        JsonArray paramList = new JsonArray();
        if (params != null) {
            Object[] paramArray = (Object[]) params;
            for (Object param : paramArray) {
                JsonObject p = describeParam(param, enums, lookup);
                if (p != null)
                    paramList.add(p);
            }
        }
        item.add("parameters", paramList);

        for (Class<?> inner : clazz.getDeclaredClasses()) {
            if (inner.isEnum()) {
                JsonArray values = new JsonArray();
                for (Object constant : inner.getEnumConstants()) {
                    values.add(constant.toString());
                }
                if (values.size() > 0) {
                    enums.add(inner.getSimpleName(), values);
                }
            }
        }

        return item;
    }

    private JsonObject describeParam(Object param, JsonObject enums, MethodHandles.Lookup lookup) throws Exception {
        Class<?> paramClass = param.getClass();
        MethodHandle getPType = lookup.findGetter(paramClass, "type", Class.class);
        MethodHandle getField = lookup.findGetter(paramClass, "field", Object.class);

        Class<?> pType = (Class<?>) invoke(getPType, param);
        Object field = invoke(getField, param);

        MethodHandle getName = lookup.findVirtual(field.getClass(), "getName",
                MethodType.methodType(String.class));
        String fieldName = (String) invoke(getName, field);

        JsonObject p = new JsonObject();
        Json.put(p, "name", fieldName);
        Json.put(p, "type", pType.getSimpleName());

        if (pType.isEnum()) {
            JsonArray values = new JsonArray();
            for (Object constant : pType.getEnumConstants()) {
                values.add(constant.toString());
            }
            if (values.size() > 0) {
                p.add("enumValues", values);
                String enumName = pType.getSimpleName();
                if (!enums.has(enumName)) {
                    enums.add(enumName, values);
                }
            }
        }

        return p;
    }

    private String callAnnotationMethod(Object annotation, String methodName, MethodHandles.Lookup lookup)
            throws Exception {
        MethodHandle mh = lookup.findVirtual(annotation.getClass(), methodName,
                MethodType.methodType(Object.class));
        Object result = invoke(mh, annotation);
        return result != null ? result.toString() : "";
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(MethodHandle mh, Object... args) throws Exception {
        try {
            return (T) mh.invokeWithArguments(args);
        } catch (Throwable t) {
            if (t instanceof Error)
                throw (Error) t;
            if (t instanceof Exception)
                throw (Exception) t;
            throw new RuntimeException(t);
        }
    }
}
