package com.deeme.tasks.mcp.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import com.deeme.tasks.mcp.util.Json;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;

public class ValidateConditionTool implements McpTool {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String DARKBOT_PKG = "com.github.manolo8.darkbot.config.actions";

    @Override
    public String getName() {
        return "validate_condition";
    }

    @Override
    public String getDescription() {
        return "Validate a DarkBot condition DSL string. " +
                "Parses the condition using DarkBot's own parser and returns detailed error information if invalid. " +
                "Use this to verify conditions before using them in SAB config or Extra Actions.";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject conditionProp = new JsonObject();
        conditionProp.addProperty("type", "string");
        conditionProp.addProperty("description",
                "The condition DSL string to validate. " +
                        "Examples: 'all(has-quest(), if(health(hero()) > number(5000)))', " +
                        "'any(has-effect(singularity, target()), has-formation(chevron, hero()))'");

        JsonObject props = new JsonObject();
        props.add("condition", conditionProp);

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add(new JsonPrimitive("condition"));
        schema.add("required", required);

        addExamples(schema);

        return schema;
    }

    private void addExamples(JsonObject schema) {
        JsonArray examples = new JsonArray();

        JsonObject ex1 = new JsonObject();
        ex1.addProperty("condition", "all(has-quest(), if(health(hero()) > number(5000)))");
        examples.add(ex1);

        JsonObject ex2 = new JsonObject();
        ex2.addProperty("condition", "any(has-effect(singularity, target()), has-formation(chevron, hero()))");
        examples.add(ex2);

        JsonObject ex3 = new JsonObject();
        ex3.addProperty("condition", "after(7.5, has-quest())");
        examples.add(ex3);

        JsonObject ex4 = new JsonObject();
        ex4.addProperty("condition", "boolean(true)");
        examples.add(ex4);

        schema.add("_examples", examples);
    }

    @Override
    public String call(Map<String, Object> args) throws Exception {
        String condition = args.containsKey("condition") ? String.valueOf(args.get("condition")) : "";
        if (condition == null || condition.trim().isEmpty()) {
            return error("Missing required parameter 'condition'");
        }

        condition = condition.trim();

        try {
            Class<?> valueParserClass = Class.forName(DARKBOT_PKG + ".parser.ValueParser");
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodHandle parseCondition = findParseCondition(lookup, valueParserClass);
            if (parseCondition == null) {
                return error("DarkBot parser API not found: could not locate parseCondition method");
            }

            parseCondition.invoke(condition);

            JsonObject result = new JsonObject();
            Json.put(result, "valid", true);
            result.addProperty("condition", condition);
            result.addProperty("message", "Condition is valid");
            return GSON.toJson(result);

        } catch (ClassNotFoundException e) {
            return error("DarkBot parser not available: " + e.getMessage());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return error("DarkBot parser API changed: " + e.getMessage());
        } catch (Throwable e) {
            return handleParseError(condition, e);
        }
    }

    private MethodHandle findParseCondition(MethodHandles.Lookup lookup, Class<?> parserClass) {
        Class<?>[] returnTypes;
        try {
            Class<?> conditionClass = Class.forName(DARKBOT_PKG + ".Condition");
            returnTypes = new Class<?>[] { Object.class, conditionClass, void.class, boolean.class, String.class };
        } catch (ClassNotFoundException e) {
            returnTypes = new Class<?>[] { Object.class, void.class, boolean.class, String.class };
        }

        for (String name : new String[] { "parseCondition", "parse", "evaluate" }) {
            for (Class<?> ret : returnTypes) {
                try {
                    return lookup.findStatic(parserClass, name,
                            MethodType.methodType(ret, String.class));
                } catch (NoSuchMethodException | IllegalAccessException ignored) {
                }
            }
            for (Class<?> ret : returnTypes) {
                try {
                    return lookup.findVirtual(parserClass, name,
                            MethodType.methodType(ret, String.class));
                } catch (NoSuchMethodException | IllegalAccessException ignored) {
                }
            }
        }
        return null;
    }

    private String handleParseError(String condition, Throwable cause) {
        JsonObject result = new JsonObject();
        Json.put(result, "valid", false);
        result.addProperty("condition", condition);
        result.addProperty("error", cause.getMessage());

        if (cause.getClass().getName().equals(DARKBOT_PKG + ".SyntaxException")) {
            extractErrorDetails(cause, result);
        }

        return GSON.toJson(result);
    }

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

    @SuppressWarnings("unchecked")
    private void extractErrorDetails(Object cause, JsonObject result) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> causeClass = cause.getClass();

            MethodHandle getAt = lookup.findVirtual(causeClass, "getAt",
                    MethodType.methodType(String.class));
            String at = invoke(getAt, cause);
            if (at != null && !at.isEmpty()) {
                result.addProperty("position", at);
            }

            MethodHandle getExpected = lookup.findVirtual(causeClass, "getExpected",
                    MethodType.methodType(Object.class));
            Object expected = invoke(getExpected, cause);
            if (expected instanceof String[]) {
                String[] expectedArr = (String[]) expected;
                if (expectedArr.length > 0) {
                    JsonArray arr = new JsonArray();
                    for (String e : expectedArr) {
                        arr.add(e);
                    }
                    result.add("expected", arr);
                }
            }

            MethodHandle getMetadata = lookup.findVirtual(causeClass, "getMetadata",
                    MethodType.methodType(Object.class));
            Object metadatas = invoke(getMetadata, cause);
            if (metadatas instanceof java.util.List) {
                java.util.List<Object> metaList = (java.util.List<Object>) metadatas;
                if (!metaList.isEmpty()) {
                    JsonArray suggestions = new JsonArray();
                    for (Object meta : metaList) {
                        try {
                            MethodHandle getVd = lookup.findGetter(meta.getClass(), "valueData", Object.class);
                            Object vd = invoke(getVd, meta);

                            MethodHandle getName = lookup.findVirtual(vd.getClass(), "name",
                                    MethodType.methodType(String.class));
                            MethodHandle getDesc = lookup.findVirtual(vd.getClass(), "description",
                                    MethodType.methodType(String.class));
                            String name = invoke(getName, vd);
                            String desc = invoke(getDesc, vd);
                            JsonObject s = new JsonObject();
                            s.addProperty("name", name);
                            s.addProperty("description", desc);
                            suggestions.add(s);
                        } catch (Exception ignored) {
                        }
                    }
                    if (suggestions.size() > 0) {
                        result.add("suggestions", suggestions);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String error(String message) {
        JsonObject err = new JsonObject();
        Json.put(err, "valid", false);
        err.addProperty("error", message);
        return GSON.toJson(err);
    }
}
