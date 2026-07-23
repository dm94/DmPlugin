package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.config.types.NpcInfo;
import eu.darkbot.api.game.enums.PetGear;
import eu.darkbot.api.managers.PetAPI;

/**
 * Hero PET status: enabled/active/repaired, current gear, repair count,
 * locator NPC list + ping location, and fuel/HP/shield/XP/heat stats.
 */
public class PetResource implements McpResource {

    private final PetAPI pet;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public PetResource(PetAPI pet) {
        this.pet = pet;
    }

    @Override
    public String getUri() {
        return "mcp://pet";
    }

    @Override
    public String getName() {
        return "PET Status";
    }

    @Override
    public String getDescription() {
        return "Hero PET status: enabled, active, repaired, current gear, repair count, locator targets, and stats.";
    }

    @Override
    public String read(String uri) {
        JsonObject obj = new JsonObject();
        Json.put(obj, "enabled", pet.isEnabled());
        Json.put(obj, "active", pet.isActive());
        Json.put(obj, "repaired", pet.isRepaired());
        Json.put(obj, "repair_count", pet.getRepairCount());

        PetGear gear = pet.getGear();
        if (gear != null) {
            obj.addProperty("gear_id", gear.getId());
            obj.addProperty("gear_name", gear.getName());
        }

        pet.getLocatorNpcLoc().ifPresent(loc -> {
            JsonObject p = new JsonObject();
            Json.put(p, "x", Math.round(loc.getX() * 100.0) / 100.0);
            Json.put(p, "y", Math.round(loc.getY() * 100.0) / 100.0);
            obj.add("locator_ping", p);
        });

        JsonArray locatorNpcs = new JsonArray();
        for (NpcInfo info : pet.getLocatorNpcs()) {
            JsonObject n = new JsonObject();
            if (info != null) {
                Json.put(n, "should_kill", info.getShouldKill());
                Json.put(n, "priority", info.getPriority());
            }
            locatorNpcs.add(n);
        }
        obj.add("locator_npcs", locatorNpcs);

        JsonObject stats = new JsonObject();
        addStat(stats, "hp", PetAPI.Stat.HP);
        addStat(stats, "shield", PetAPI.Stat.SHIELD);
        addStat(stats, "fuel", PetAPI.Stat.FUEL);
        addStat(stats, "xp", PetAPI.Stat.XP);
        addStat(stats, "heat", PetAPI.Stat.HEAT);
        obj.add("stats", stats);

        return gson.toJson(obj);
    }

    private void addStat(JsonObject parent, String key, PetAPI.Stat stat) {
        PetAPI.PetStat s = pet.getStat(stat);
        if (s == null)
            return;
        JsonObject o = new JsonObject();
        Json.put(o, "current", Math.round(s.getCurrent() * 100.0) / 100.0);
        Json.put(o, "total", Math.round(s.getTotal() * 100.0) / 100.0);
        parent.add(key, o);
    }
}
