package com.deeme.tasks.mcp.tools;

import com.google.gson.JsonObject;
import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.managers.BotAPI;

import java.util.Map;

public class BotControlTool implements McpTool {

    private final BotAPI bot;

    public BotControlTool(BotAPI bot) {
        this.bot = bot;
    }

    @Override
    public String getName() {
        return "toggle_pause";
    }

    @Override
    public String getDescription() {
        return "Toggle bot running state between paused and running";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        Json.put(schema, "type", "object");
        schema.add("properties", new JsonObject());
        return schema;
    }

    @Override
    public String call(Map<String, Object> args) {
        boolean wasRunning = bot.isRunning();
        bot.setRunning(!wasRunning);
        JsonObject result = new JsonObject();
        Json.put(result, "previous_state", wasRunning ? "running" : "paused");
        Json.put(result, "current_state", bot.isRunning() ? "running" : "paused");
        return result.toString();
    }
}
