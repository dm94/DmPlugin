package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.managers.InventoryAPI;

/**
 * Inventory contents: loot id, readable name, and amount per stacked item.
 * The {@code minWaitMs} argument is exposed as query string {@code ?wait_ms=}.
 */
public class InventoryResource implements McpResource {

    private static final int DEFAULT_WAIT_MS = 0;

    private final InventoryAPI inventory;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public InventoryResource(InventoryAPI inventory) {
        this.inventory = inventory;
    }

    @Override
    public String getUri() {
        return "mcp://inventory";
    }

    @Override
    public String getName() {
        return "Inventory";
    }

    @Override
    public String getDescription() {
        return "Items in inventory. Optional ?wait_ms=N to wait up to N ms for fresh data (default 0).";
    }

    @Override
    public String read(String uri) {
        int waitMs = parseInt(parseQuery(uri).get("wait_ms"), DEFAULT_WAIT_MS);

        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        for (InventoryAPI.Item item : inventory.getItems(waitMs)) {
            if (item == null) continue;
            JsonObject o = new JsonObject();
            o.addProperty("loot_id", item.getLootId());
            o.addProperty("name", item.getName());
            Json.put(o, "amount", Math.round(item.getAmount() * 100.0) / 100.0);
            arr.add(o);
        }

        Json.put(obj, "item_count", Json.size(arr));
        obj.add("items", arr);
        return gson.toJson(obj);
    }

    private static java.util.Map<String, String> parseQuery(String uri) {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        int q = uri.indexOf('?');
        if (q < 0) return params;
        for (String pair : uri.substring(q + 1).split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) params.put(pair.substring(0, eq), pair.substring(eq + 1));
        }
        return params;
    }

    private static int parseInt(String s, int fallback) {
        if (s == null || s.isEmpty()) return fallback;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
