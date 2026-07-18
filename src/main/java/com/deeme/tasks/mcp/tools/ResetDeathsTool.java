package com.deeme.tasks.mcp.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.managers.RepairAPI;

import java.util.Map;

/**
 * Reset the hero's death counter to 0. Useful between farming sessions
 * or to silence death-based safety triggers during testing.
 */
public class ResetDeathsTool implements McpTool {

    private final RepairAPI repair;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public ResetDeathsTool(RepairAPI repair) {
        this.repair = repair;
    }

    @Override
    public String getName() {
        return "reset_deaths";
    }

    @Override
    public String getDescription() {
        return "Reset the hero's death counter to 0.";
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
        int previous = repair.getDeathAmount();
        repair.resetDeaths();

        JsonObject result = new JsonObject();
        Json.put(result, "previous_deaths", previous);
        Json.put(result, "current_deaths", repair.getDeathAmount());
        return gson.toJson(result);
    }
}
