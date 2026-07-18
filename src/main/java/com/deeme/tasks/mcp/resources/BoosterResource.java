package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.managers.BoosterAPI;

/**
 * Active ship boosters: type, displayed name, amount (%), and remaining
 * time (seconds). One booster may aggregate several sub-boosters.
 */
public class BoosterResource implements McpResource {

    private final BoosterAPI boosters;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public BoosterResource(BoosterAPI boosters) {
        this.boosters = boosters;
    }

    @Override
    public String getUri() {
        return "mcp://boosters";
    }

    @Override
    public String getName() {
        return "Active Boosters";
    }

    @Override
    public String getDescription() {
        return "Currently active ship boosters with amount (%) and remaining time (seconds).";
    }

    @Override
    public String read(String uri) {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();

        for (BoosterAPI.Booster b : boosters.getBoosters()) {
            if (b == null) continue;
            JsonObject o = new JsonObject();
            o.addProperty("category", b.getCategory());
            o.addProperty("name", b.getName());
            Json.put(o, "amount_percent", Math.round(b.getAmount() * 10000.0) / 100.0);
            Json.put(o, "remaining_seconds", Math.round(b.getRemainingTime() * 100.0) / 100.0);
            if (b.getType() != null) {
                o.addProperty("type", b.getType().name());
                o.addProperty("short", b.getSmall());
            }
            arr.add(o);
        }

        Json.put(obj, "count", Json.size(arr));
        obj.add("boosters", arr);
        return gson.toJson(obj);
    }
}
