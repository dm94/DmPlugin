package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;

import java.util.HashMap;
import java.util.Map;

public class ConfigValueResource implements McpResource {

    private final ConfigAPI configAPI;
    private final BotAPI bot;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public ConfigValueResource(ConfigAPI configAPI, BotAPI bot) {
        this.configAPI = configAPI;
        this.bot = bot;
    }

    @Override
    public String getUri() {
        return "bot://config/value";
    }

    @Override
    public String getName() {
        return "Config Value";
    }

    @Override
    public String getDescription() {
        return "Read a config value by dot-separated path (e.g. 'general.working_map'). Returns current module status if no path given.";
    }

    @Override
    public String read(String uri) {
        String path = extractPath(uri);

        if (path.isEmpty()) {
            JsonObject result = new JsonObject();
            result.addProperty("module",
                    bot.getModule() != null ? bot.getModule().getClass().getSimpleName() : "(none)");
            Json.put(result, "running", bot.isRunning());
            return gson.toJson(result);
        }

        try {
            Object value = configAPI.getConfigValue(path);
            JsonObject result = new JsonObject();
            result.addProperty("path", path);
            result.add("value", gson.toJsonTree(value));
            return result.toString();
        } catch (Exception e) {
            JsonObject err = new JsonObject();
            err.addProperty("error", "Path not found: " + path);
            err.addProperty("details", e.getMessage());
            return err.toString();
        }
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
}
