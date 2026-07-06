package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.StarSystemAPI;

public class BotResource implements McpResource {

    private final BotAPI bot;
    private final StarSystemAPI starSystem;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public BotResource(BotAPI bot, StarSystemAPI starSystem) {
        this.bot = bot;
        this.starSystem = starSystem;
    }

    @Override
    public String getUri() {
        return "bot://status";
    }

    @Override
    public String getName() {
        return "Bot Status";
    }

    @Override
    public String getDescription() {
        return "Current bot status: running state, module, map, performance";
    }

    @Override
    public String read(String uri) {
        JsonObject obj = new JsonObject();
        Json.put(obj, "running", bot.isRunning());
        Json.put(obj, "paused", !bot.isRunning());
        obj.addProperty("module_name", bot.getModule() != null ? bot.getModule().getClass().getSimpleName() : "(none)");
        if (starSystem.getCurrentMap() != null) {
            Json.put(obj, "map_id", starSystem.getCurrentMap().getId());
            obj.addProperty("map_name", starSystem.getCurrentMap().getName());
        }
        Json.put(obj, "tick_time_ms", Math.round(bot.getTickTime() * 100.0) / 100.0);
        obj.addProperty("bot_version", bot.getVersion().toString());
        return gson.toJson(obj);
    }
}
