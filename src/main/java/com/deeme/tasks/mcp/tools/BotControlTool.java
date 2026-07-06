package com.deeme.tasks.mcp.tools;

import com.google.gson.JsonObject;
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
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        return schema;
    }

    @Override
    public String call(Map<String, Object> args) {
        boolean wasRunning = bot.isRunning();
        bot.setRunning(!wasRunning);
        JsonObject result = new JsonObject();
        result.addProperty("previous_state", wasRunning ? "running" : "paused");
        result.addProperty("current_state", bot.isRunning() ? "running" : "paused");
        return result.toString();
    }
}
