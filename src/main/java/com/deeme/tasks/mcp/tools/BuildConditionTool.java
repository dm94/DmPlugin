package com.deeme.tasks.mcp.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import com.deeme.tasks.mcp.util.Json;

import java.util.List;
import java.util.Map;

public class BuildConditionTool implements McpTool {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    @Override
    public String getName() {
        return "build_condition";
    }

    @Override
    public String getDescription() {
        return "Build a DarkBot condition DSL string from a structured JSON tree. " +
                "Use this when you want to construct conditions programmatically " +
                "without memorizing the exact DSL syntax. See conditions://schema for details.";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject conditionProp = new JsonObject();
        conditionProp.addProperty("type", "object");
        conditionProp.addProperty("description",
                "Condition tree node with 'type' and type-specific fields. " +
                        "See conditions://schema for all types, parameters, and enum values.");
        JsonObject typeProp = new JsonObject();
        typeProp.addProperty("type", "string");
        typeProp.addProperty("description",
                "Condition type: all, any, none, one, if, equal, has-effect, " +
                        "has-formation, has-relation, has-quest, after, until, boolean");
        conditionProp.add("properties", new JsonObject());
        conditionProp.add("required", new JsonArray());
        conditionProp.add("additionalProperties", new JsonPrimitive(true));

        JsonObject props = new JsonObject();
        props.add("condition", conditionProp);

        JsonArray examples = new JsonArray();

        JsonObject ex1 = new JsonObject();
        ex1.addProperty("condition",
                "{\"type\":\"all\",\"children\":[{\"type\":\"has-quest\"},{\"type\":\"if\"," +
                        "\"a\":{\"type\":\"health\",\"ship\":{\"type\":\"hero\"}},\"operation\":\">\"," +
                        "\"b\":{\"type\":\"number\",\"value\":5000}}]}");
        examples.add(ex1);

        JsonObject ex2 = new JsonObject();
        ex2.addProperty("condition",
                "{\"type\":\"any\",\"children\":[" +
                        "{\"type\":\"has-effect\",\"effect\":\"singularity\",\"ship\":{\"type\":\"target\"}}," +
                        "{\"type\":\"has-formation\",\"formation\":\"chevron\",\"ship\":{\"type\":\"hero\"}}]}");
        examples.add(ex2);

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add(new JsonPrimitive("condition"));
        schema.add("required", required);
        schema.add("_examples", examples);

        return schema;
    }

    @Override
    public String call(Map<String, Object> args) throws Exception {
        Object conditionObj = args.get("condition");
        if (conditionObj == null) {
            return error("Missing required parameter 'condition'");
        }
        if (!(conditionObj instanceof Map)) {
            return error("'condition' must be a JSON object with at least a 'type' field");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> condition = (Map<String, Object>) conditionObj;

        try {
            String dsl = buildNode(condition);
            JsonObject result = new JsonObject();
            result.addProperty("condition", dsl);
            Json.put(result, "valid", true);
            return GSON.toJson(result);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String buildNode(Object node) {
        if (node instanceof String)
            return (String) node;
        if (node instanceof Number)
            return "number(" + node + ")";
        if (node instanceof Boolean)
            return "boolean(" + node + ")";
        if (!(node instanceof Map)) {
            throw new IllegalArgumentException(
                    "Invalid node type: " + (node == null ? "null" : node.getClass().getName()));
        }

        Map<String, Object> map = (Map<String, Object>) node;
        String type = (String) map.get("type");
        if (type == null) {
            throw new IllegalArgumentException("Every node must have a 'type' field");
        }

        switch (type) {
            case "all":
                return "all(" + buildChildren(getList(map, "children")) + ")";
            case "any":
                return "any(" + buildChildren(getList(map, "children")) + ")";
            case "one":
                return "one(" + buildChildren(getList(map, "children")) + ")";
            case "none":
                return "none(" + buildChildren(getList(map, "children")) + ")";

            case "if":
                return "if(" + buildNode(get(map, "a")) + " " +
                        getString(map, "operation") + " " +
                        buildNode(get(map, "b")) + ")";

            case "equal":
                return "equal(" + buildNode(get(map, "a")) + ", " + buildNode(get(map, "b")) + ")";

            case "has-effect":
                return "has-effect(" + getString(map, "effect") + ", " +
                        buildNode(get(map, "ship")) + ")";

            case "has-formation":
                return "has-formation(" + getString(map, "formation") + ", " +
                        buildNode(get(map, "ship")) + ")";

            case "has-relation":
                return "has-relation(" + getString(map, "type") + ", " +
                        buildNode(get(map, "ship")) + ")";

            case "has-quest":
                return "has-quest()";

            case "after":
                return "after(" + getNumber(map, "seconds") + ", " +
                        buildNode(get(map, "condition")) + ")";

            case "until":
                return "until(" + buildNode(get(map, "from")) + ", " +
                        buildNode(get(map, "until")) + ")";

            case "boolean":
                Object bv = map.get("value");
                return "boolean(" + (bv == null ? "null" : String.valueOf(bv)) + ")";

            case "number":
                return "number(" + getNumber(map, "value") + ")";

            case "percent":
                return "percent(" + getNumber(map, "value") + ")";

            case "string":
                String sv = map.containsKey("value") ? String.valueOf(map.get("value")) : "";
                return "string(" + sv.replace(")", "\\)") + ")";

            case "location":
                return "location(" + getNumber(map, "x") + ", " + getNumber(map, "y") + ")";

            case "map":
                return "map(" + getString(map, "value") + ")";

            case "health":
                return "health(" + buildNode(get(map, "ship")) + ")";

            case "hp-type":
                return "hp-type(" + getString(map, "type") + ", " +
                        buildNode(get(map, "health")) + ")";

            case "distance":
                return "distance(" + buildNode(get(map, "a")) + ", " +
                        buildNode(get(map, "b")) + ")";

            case "ship-loc":
                return "ship-loc(" + buildNode(get(map, "ship")) + ")";

            case "name":
                return "name(" + buildNode(get(map, "ship")) + ")";

            case "stat-type":
                return "stat-type(" + getString(map, "key") + "," +
                        getString(map, "dataType") + ")";

            case "hero":
                return "hero()";

            case "target":
                return "target()";

            case "hero-map":
                return "hero-map()";

            default:
                throw new IllegalArgumentException("Unknown type: '" + type + "'. " +
                        "See conditions://schema for available types.");
        }
    }

    private String buildChildren(List<Object> children) {
        if (children == null || children.isEmpty()) {
            throw new IllegalArgumentException("Combinator conditions need at least one child");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < children.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(buildNode(children.get(i)));
        }
        return sb.toString();
    }

    private Object get(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null && !map.containsKey(key)) {
            throw new IllegalArgumentException("Missing required field '" + key + "'");
        }
        return val;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = get(map, key);
        return String.valueOf(val);
    }

    private String getNumber(Map<String, Object> map, String key) {
        Object val = get(map, key);
        if (val instanceof Number) {
            Number n = (Number) val;
            if (n.doubleValue() == n.longValue() && !Double.isInfinite(n.doubleValue())) {
                return String.valueOf(n.longValue());
            }
            return String.valueOf(n.doubleValue());
        }
        return String.valueOf(val);
    }

    @SuppressWarnings("unchecked")
    private List<Object> getList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) {
            throw new IllegalArgumentException("Missing required field '" + key + "'");
        }
        if (val instanceof List) {
            return (List<Object>) val;
        }
        throw new IllegalArgumentException("Field '" + key + "' must be an array");
    }

    private String error(String message) {
        JsonObject err = new JsonObject();
        Json.put(err, "valid", false);
        err.addProperty("error", message);
        return GSON.toJson(err);
    }
}
