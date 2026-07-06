package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.StarSystemAPI;

public class HeroResource implements McpResource {

    private final HeroAPI hero;
    private final StarSystemAPI starSystem;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public HeroResource(HeroAPI hero, StarSystemAPI starSystem) {
        this.hero = hero;
        this.starSystem = starSystem;
    }

    @Override
    public String getUri() {
        return "bot://hero";
    }

    @Override
    public String getName() {
        return "Hero Info";
    }

    @Override
    public String getDescription() {
        return "Player ship info: health, shield, position, config, formation, target";
    }

    @Override
    public String read(String uri) {
        JsonObject obj = new JsonObject();
        Json.put(obj, "id", hero.getId());
        Json.put(obj, "ship_id", hero.getShipId());

        JsonObject health = new JsonObject();
        Json.put(health, "hp", hero.getHealth().getHp());
        Json.put(health, "max_hp", hero.getHealth().getMaxHp());
        Json.put(health, "hull", hero.getHealth().getHull());
        Json.put(health, "max_hull", hero.getHealth().getMaxHull());
        Json.put(health, "shield", hero.getHealth().getShield());
        Json.put(health, "max_shield", hero.getHealth().getMaxShield());
        Json.put(health, "hp_percent", Math.round(hero.getHealth().hpPercent() * 10000.0) / 100.0);
        Json.put(health, "shield_percent", Math.round(hero.getHealth().shieldPercent() * 10000.0) / 100.0);
        obj.add("health", health);

        JsonObject pos = new JsonObject();
        Json.put(pos, "x", Math.round(hero.getX() * 100.0) / 100.0);
        Json.put(pos, "y", Math.round(hero.getY() * 100.0) / 100.0);
        obj.add("position", pos);

        if (starSystem.getCurrentMap() != null) {
            Json.put(obj, "map_id", starSystem.getCurrentMap().getId());
            obj.addProperty("map_name", starSystem.getCurrentMap().getName());
        }

        if (hero.getConfiguration() != null) {
            Json.put(obj, "config", hero.getConfiguration().ordinal());
            obj.addProperty("config_name", hero.getConfiguration().name());
        }
        if (hero.getFormation() != null) {
            obj.addProperty("formation", hero.getFormation().name());
        }

        Lockable target = hero.getLocalTarget();
        if (target != null && target.isValid()) {
            JsonObject t = new JsonObject();
            Json.put(t, "id", target.getId());
            Json.put(t, "x", Math.round(target.getX() * 100.0) / 100.0);
            Json.put(t, "y", Math.round(target.getY() * 100.0) / 100.0);
            obj.add("target", t);
        }

        Json.put(obj, "has_pet", hero.hasPet());
        Json.put(obj, "invisible", hero.isInvisible());
        Json.put(obj, "is_moving", hero.isMoving());

        return gson.toJson(obj);
    }
}
