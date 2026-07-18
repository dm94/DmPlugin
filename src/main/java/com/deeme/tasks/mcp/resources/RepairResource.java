package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.game.enums.ReviveLocation;
import eu.darkbot.api.managers.RepairAPI;

/**
 * Hero repair state: death count, destroyed flag, last destroyer name,
 * last death time/location, and revive-location availability.
 */
public class RepairResource implements McpResource {

    private final RepairAPI repair;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public RepairResource(RepairAPI repair) {
        this.repair = repair;
    }

    @Override
    public String getUri() {
        return "mcp://repair";
    }

    @Override
    public String getName() {
        return "Repair Status";
    }

    @Override
    public String getDescription() {
        return "Hero deaths, destroyed flag, last destroyer, last death time/location, revive locations.";
    }

    @Override
    public String read(String uri) {
        JsonObject obj = new JsonObject();
        Json.put(obj, "deaths", repair.getDeathAmount());
        Json.put(obj, "destroyed", repair.isDestroyed());

        String destroyer = repair.getLastDestroyerName();
        if (destroyer != null)
            obj.addProperty("last_destroyer", destroyer);

        if (repair.getLastDeathTime() != null) {
            obj.addProperty("last_death_time", repair.getLastDeathTime().toString());
            Json.put(obj, "last_death_epoch", repair.getLastDeathTime().getEpochSecond());
        }

        if (repair.getLastDeathLocation() != null) {
            JsonObject loc = new JsonObject();
            Json.put(loc, "x", Math.round(repair.getLastDeathLocation().getX() * 100.0) / 100.0);
            Json.put(loc, "y", Math.round(repair.getLastDeathLocation().getY() * 100.0) / 100.0);
            obj.add("last_death_location", loc);
        }

        JsonObject revive = new JsonObject();
        for (ReviveLocation rl : ReviveLocation.values()) {
            int secs = repair.isAvailableIn(rl);
            if (secs >= 0) {
                JsonObject r = new JsonObject();
                Json.put(r, "available_in_seconds", secs);
                Json.put(r, "ready", secs == 0);
                revive.add(rl.name(), r);
            }
        }
        obj.add("revive_locations", revive);

        return gson.toJson(obj);
    }
}
