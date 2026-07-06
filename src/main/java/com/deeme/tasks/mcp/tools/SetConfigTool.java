package com.deeme.tasks.mcp.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.managers.ConfigAPI;

import java.util.Map;

public class SetConfigTool implements McpTool {

    private final ConfigAPI configAPI;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public SetConfigTool(ConfigAPI configAPI) {
        this.configAPI = configAPI;
    }

    @Override
    public String getName() {
        return "set_config";
    }

    @Override
    public String getDescription() {
        return "Update a bot config value by dot-separated path (e.g. 'general.working_map'). "
                + "The value is converted to the setting's type (boolean, number, string or enum).";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject pathProp = new JsonObject();
        pathProp.addProperty("type", "string");
        pathProp.addProperty("description", "Dot-separated config path (e.g. general.working_map).");

        JsonObject valueProp = new JsonObject();
        valueProp.addProperty("description", "New value for the setting (boolean, number or string).");

        JsonObject props = new JsonObject();
        props.add("path", pathProp);
        props.add("value", valueProp);

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add(new JsonPrimitive("path"));
        required.add(new JsonPrimitive("value"));
        schema.add("required", required);
        return schema;
    }

    @Override
    public String call(Map<String, Object> args) {
        String path = args.containsKey("path") ? String.valueOf(args.get("path")) : "";
        if (path.isEmpty())
            return error("Missing required parameter 'path'");
        if (!args.containsKey("value"))
            return error("Missing required parameter 'value'");

        ConfigSetting<Object> setting = configAPI.getConfig(path);
        if (setting == null)
            return error("Config path not found: " + path);

        Object converted;
        try {
            converted = convert(args.get("value"), setting.getType());
        } catch (IllegalArgumentException e) {
            return error("Invalid value for type " + setting.getType().getSimpleName() + ": " + e.getMessage());
        }

        Object previous = setting.getValue();
        setting.setValue(converted);

        JsonObject result = new JsonObject();
        result.addProperty("path", path);
        result.addProperty("type", setting.getType().getSimpleName());
        result.add("previous_value", gson.toJsonTree(previous));
        result.add("new_value", gson.toJsonTree(converted));
        return gson.toJson(result);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object convert(Object value, Class<?> type) {
        if (type.isInstance(value))
            return value;

        String str = String.valueOf(value);

        if (type == Boolean.class || type == boolean.class)
            return Boolean.parseBoolean(str);
        if (type == Integer.class || type == int.class)
            return parseNumber(value).intValue();
        if (type == Long.class || type == long.class)
            return parseNumber(value).longValue();
        if (type == Double.class || type == double.class)
            return parseNumber(value).doubleValue();
        if (type == Float.class || type == float.class)
            return parseNumber(value).floatValue();
        if (type == String.class)
            return str;
        if (type.isEnum())
            return Enum.valueOf((Class) type, str);

        throw new IllegalArgumentException("Unsupported config type: " + type.getName());
    }

    private Number parseNumber(Object value) {
        if (value instanceof Number)
            return (Number) value;
        return Double.parseDouble(String.valueOf(value));
    }

    private String error(String message) {
        JsonObject err = new JsonObject();
        err.addProperty("error", message);
        return gson.toJson(err);
    }
}
