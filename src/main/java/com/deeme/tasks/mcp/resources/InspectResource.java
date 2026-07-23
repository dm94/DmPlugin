package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.deeme.tasks.mcp.util.MemoryInspector;
import com.deeme.tasks.mcp.util.ObjectInspector;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.managers.StatsAPI;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class InspectResource implements McpResource {

    private static final int DEFAULT_MAX_DEPTH = 2;
    private static final int DEFAULT_MAX_ITEMS = 25;
    private static final int MAX_DEPTH_LIMIT = 5;
    private static final int MAX_ITEMS_LIMIT = 100;

    private final Map<String, Object> roots = new LinkedHashMap<>();
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public InspectResource(BotAPI bot, HeroAPI hero, StatsAPI stats,
            ExtensionsAPI extensions, StarSystemAPI starSystem, ConfigAPI configAPI) {
        roots.put("bot", bot);
        roots.put("hero", hero);
        roots.put("stats", stats);
        roots.put("extensions", extensions);
        roots.put("star_system", starSystem);
        roots.put("config", configAPI);
    }

    @Override
    public String getUri() {
        return "bot://inspect";
    }

    @Override
    public String getName() {
        return "Object Inspector";
    }

    @Override
    public String getDescription() {
        return "Inspect DarkBot runtime objects by reflection, similar to Object.inspector. "
                + "Use root=...&path=... to navigate from a known root, or address=0x... to "
                + "read an object at a specific memory address (like the DarkBot inspector's "
                + "address box). Useful for AI-assisted debugging and discovery.";
    }

    @Override
    public String read(String uri) {
        Map<String, String> params = parseQuery(uri);

        String address = params.get("address");
        if (address != null && !address.isEmpty()) {
            return readByAddress(address);
        }

        String rootName = params.get("root");
        if (rootName == null || rootName.isEmpty()) {
            JsonObject err = new JsonObject();
            err.addProperty("error", "Missing 'root' or 'address' parameter");
            err.add("available_roots", availableRoots());
            return gson.toJson(err);
        }

        String path = params.getOrDefault("path", "");
        int maxDepth = boundedInteger(params.get("max_depth"), DEFAULT_MAX_DEPTH, 0, MAX_DEPTH_LIMIT);
        int maxItems = boundedInteger(params.get("max_items"), DEFAULT_MAX_ITEMS, 1, MAX_ITEMS_LIMIT);

        Object root = roots.get(rootName);
        if (root == null) {
            JsonObject err = new JsonObject();
            err.addProperty("error", "Unknown root: " + rootName);
            err.add("available_roots", availableRoots());
            return gson.toJson(err);
        }

        ObjectInspector inspector = new ObjectInspector(maxDepth, maxItems);
        JsonObject result = inspector.inspect(rootName, root, path);
        result.add("available_roots", availableRoots());
        return gson.toJson(result);
    }

    private String readByAddress(String address) {
        JsonObject result = new MemoryInspector().inspect(address);
        if (result.get("available_roots") == null) {
            result.add("available_roots", availableRoots());
        }
        return gson.toJson(result);
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

    private JsonArray availableRoots() {
        JsonArray array = new JsonArray();
        for (String rootName : roots.keySet()) {
            array.add(new JsonPrimitive(rootName));
        }
        return array;
    }

    private int boundedInteger(String value, int defaultValue, int minimum, int maximum) {
        if (value == null || value.isEmpty())
            return defaultValue;
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < minimum || parsed > maximum)
                return defaultValue;
            return parsed;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
