package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.managers.ConfigAPI;

import java.util.HashMap;
import java.util.Map;

public class ConfigTreeResource implements McpResource {

    private static final int MAX_DISPLAY_ITEMS = 200;

    private final ConfigAPI configAPI;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public ConfigTreeResource(ConfigAPI configAPI) {
        this.configAPI = configAPI;
    }

    @Override
    public String getUri() {
        return "bot://config/tree";
    }

    @Override
    public String getName() {
        return "Config Tree";
    }

    @Override
    public String getDescription() {
        return "Navigate the bot configuration tree. "
                + "Use empty path for root categories (general, loot, misc, etc.), "
                + "drill down with dot-separated paths like 'general' or 'general.safety'. "
                + "Returns children list if path is a category, full metadata if path is a setting.";
    }

    @Override
    public String read(String uri) {
        String path = extractPath(uri);

        ConfigSetting<?> setting;
        if (path.isEmpty()) {
            setting = configAPI.getConfigRoot();
        } else {
            setting = configAPI.getConfig(path);
        }

        if (setting == null) {
            JsonObject err = new JsonObject();
            err.addProperty("error", "Config path not found: " + path);
            return gson.toJson(err);
        }

        if (setting instanceof ConfigSetting.Parent) {
            return listChildren(path, (ConfigSetting.Parent<?>) setting);
        }
        return describeLeaf(path, setting);
    }

    private String extractPath(String uri) {
        Map<String, String> query = parseQuery(uri);
        if (query.containsKey("path"))
            return query.get("path");

        String prefix = getUri();
        if (uri.startsWith(prefix + "/")) {
            String raw = uri.substring(prefix.length() + 1);
            int qmark = raw.indexOf('?');
            return qmark >= 0 ? raw.substring(0, qmark) : raw;
        }
        return "";
    }

    private Map<String, String> parseQuery(String uri) {
        Map<String, String> params = new HashMap<>();
        int qmark = uri.indexOf('?');
        if (qmark < 0)
            return params;
        String query = uri.substring(qmark + 1);
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                params.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return params;
    }

    @SuppressWarnings("rawtypes")
    private String listChildren(String path, ConfigSetting.Parent<?> parent) {
        Map<String, ConfigSetting<?>> children = parent.getChildren();

        JsonObject result = new JsonObject();
        result.addProperty("path", path.isEmpty() ? "(root)" : path);
        result.addProperty("type", "parent");

        if (children == null || children.isEmpty()) {
            result.addProperty("child_count", 0);
            result.add("children", new JsonArray());
            return gson.toJson(result);
        }

        result.addProperty("child_count", children.size());

        JsonArray arr = new JsonArray();
        int count = 0;
        for (Map.Entry<String, ConfigSetting<?>> entry : children.entrySet()) {
            if (count >= MAX_DISPLAY_ITEMS)
                break;
            count++;

            ConfigSetting<?> child = entry.getValue();
            boolean isParent = child instanceof ConfigSetting.Parent;

            JsonObject item = new JsonObject();
            item.addProperty("name", entry.getKey());
            item.addProperty("type", isParent ? "parent" : friendlyTypeName(child.getType()));

            if (isParent) {
                Map<String, ConfigSetting<?>> sub = ((ConfigSetting.Parent) child).getChildren();
                item.addProperty("child_count", sub != null ? sub.size() : 0);
            } else {
                Object val = child.getValue();
                if (val != null) {
                    item.add("current_value", gson.toJsonTree(val));
                }
            }

            String desc = child.getDescription();
            if (desc != null && !desc.isEmpty()) {
                item.addProperty("description", desc);
            }

            arr.add(item);
        }

        if (children.size() > MAX_DISPLAY_ITEMS) {
            JsonObject truncated = new JsonObject();
            Json.put(truncated, "truncated", true);
            truncated.addProperty("included", count);
            truncated.addProperty("total", children.size());
            arr.add(truncated);
        }

        result.add("children", arr);
        return gson.toJson(result);
    }

    private String describeLeaf(String path, ConfigSetting<?> setting) {
        JsonObject result = new JsonObject();
        result.addProperty("path", path);
        result.addProperty("type", friendlyTypeName(setting.getType()));

        Object value = setting.getValue();
        if (value != null) {
            result.add("current_value", gson.toJsonTree(value));
        }

        String desc = setting.getDescription();
        if (desc != null && !desc.isEmpty()) {
            result.addProperty("description", desc);
        }

        Class<?> type = setting.getType();
        if (type.isEnum()) {
            JsonArray allowed = new JsonArray();
            for (Object c : type.getEnumConstants()) {
                allowed.add(new JsonPrimitive(c.toString()));
            }
            result.add("allowed_values", allowed);
        }

        if (type == Integer.class || type == int.class
                || type == Long.class || type == long.class
                || type == Double.class || type == double.class
                || type == Float.class || type == float.class) {
            JsonObject constraints = extractConstraints(setting);
            if (constraints != null) {
                result.add("constraints", constraints);
            }
        }

        return gson.toJson(result);
    }

    private JsonObject extractConstraints(ConfigSetting<?> setting) {
        try {
            JsonObject c = new JsonObject();
            boolean hasAny = false;

            Object min = setting.getMetadata("min");
            if (min instanceof Number) {
                c.addProperty("min", ((Number) min).doubleValue());
                hasAny = true;
            }

            Object max = setting.getMetadata("max");
            if (max instanceof Number) {
                c.addProperty("max", ((Number) max).doubleValue());
                hasAny = true;
            }

            Object step = setting.getMetadata("step");
            if (step instanceof Number) {
                c.addProperty("step", ((Number) step).doubleValue());
                hasAny = true;
            }

            return hasAny ? c : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String friendlyTypeName(Class<?> type) {
        if (type == Integer.class || type == int.class)
            return "int";
        if (type == Long.class || type == long.class)
            return "long";
        if (type == Double.class || type == double.class)
            return "double";
        if (type == Float.class || type == float.class)
            return "float";
        if (type == Boolean.class || type == boolean.class)
            return "boolean";
        if (type == String.class)
            return "string";
        if (type == Character.class || type == char.class)
            return "char";
        if (type.isEnum())
            return "enum (" + type.getSimpleName() + ")";
        return type.getSimpleName();
    }
}
