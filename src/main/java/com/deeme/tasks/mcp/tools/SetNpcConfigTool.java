package com.deeme.tasks.mcp.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.NpcInfo;
import eu.darkbot.api.managers.ConfigAPI;

import java.util.Map;

public class SetNpcConfigTool implements McpTool {

    private final ConfigAPI configAPI;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public SetNpcConfigTool(ConfigAPI configAPI) {
        this.configAPI = configAPI;
    }

    @Override
    public String getName() {
        return "set_npc_config";
    }

    @Override
    public String getDescription() {
        return "Configure an NPC by name. Set kill priority, radius, and extra flags for a specific NPC. "
                + "NPCs are stored under 'loot.npc_infos'. "
                + "Use 'remove' action to delete an NPC configuration.";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject nameProp = new JsonObject();
        nameProp.addProperty("type", "string");
        nameProp.addProperty("description", "NPC name to configure.");

        JsonObject actionProp = new JsonObject();
        actionProp.addProperty("type", "string");
        actionProp.addProperty("description", "Action: 'set' (default) or 'remove'.");
        JsonArray actionEnum = new JsonArray();
        actionEnum.add(new JsonPrimitive("set"));
        actionEnum.add(new JsonPrimitive("remove"));
        actionProp.add("enum", actionEnum);

        JsonObject shouldKillProp = new JsonObject();
        shouldKillProp.addProperty("type", "boolean");
        shouldKillProp.addProperty("description", "Whether to kill this NPC.");

        JsonObject priorityProp = new JsonObject();
        priorityProp.addProperty("type", "integer");
        priorityProp.addProperty("description", "Priority (lower = more important).");

        JsonObject radiusProp = new JsonObject();
        radiusProp.addProperty("type", "number");
        radiusProp.addProperty("description", "Distance to stand from NPC.");

        JsonObject props = new JsonObject();
        props.add("name", nameProp);
        props.add("action", actionProp);
        props.add("should_kill", shouldKillProp);
        props.add("priority", priorityProp);
        props.add("radius", radiusProp);

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", props);

        JsonArray required = new JsonArray();
        required.add(new JsonPrimitive("name"));
        schema.add("required", required);

        return schema;
    }

    @Override
    public String call(Map<String, Object> args) {
        String npcName = args.containsKey("name") ? String.valueOf(args.get("name")) : "";
        if (npcName.isEmpty()) {
            return error("Missing required parameter 'name'");
        }

        String action = args.containsKey("action") ? String.valueOf(args.get("action")) : "set";

        ConfigSetting<Map<String, NpcInfo>> configSetting = configAPI.requireConfig("loot.npc_infos");
        Map<String, NpcInfo> npcInfos = configSetting.getValue();

        if ("remove".equals(action)) {
            return removeNpc(npcName, npcInfos, configSetting);
        }

        return setNpcConfig(npcName, args, npcInfos, configSetting);
    }

    private String setNpcConfig(String npcName, Map<String, Object> args,
            Map<String, NpcInfo> npcInfos, ConfigSetting<Map<String, NpcInfo>> configSetting) {
        NpcInfo info = npcInfos.get(npcName);
        boolean isNew = info == null;

        if (isNew) {
            return error("NPC '" + npcName + "' not found. NPCs must be created in DarkBot's UI first.");
        }

        boolean modified = false;

        if (args.containsKey("should_kill")) {
            boolean shouldKill = Boolean.parseBoolean(String.valueOf(args.get("should_kill")));
            info.setShouldKill(shouldKill);
            modified = true;
        }

        if (args.containsKey("priority")) {
            int priority = parseNumber(args.get("priority")).intValue();
            info.setPriority(priority);
            modified = true;
        }

        if (args.containsKey("radius")) {
            double radius = parseNumber(args.get("radius")).doubleValue();
            info.setRadius(radius);
            modified = true;
        }

        if (!modified) {
            return error("No configuration provided. Specify at least one of: should_kill, priority, radius");
        }

        configSetting.setValue(npcInfos);

        JsonObject result = new JsonObject();
        result.addProperty("npc_name", npcName);
        result.addProperty("action", "updated");

        if (args.containsKey("should_kill")) {
            Json.put(result, "should_kill", Boolean.parseBoolean(String.valueOf(args.get("should_kill"))));
        }
        if (args.containsKey("priority")) {
            result.addProperty("priority", parseNumber(args.get("priority")).intValue());
        }
        if (args.containsKey("radius")) {
            result.addProperty("radius", parseNumber(args.get("radius")).doubleValue());
        }

        return gson.toJson(result);
    }

    private String removeNpc(String npcName, Map<String, NpcInfo> npcInfos,
            ConfigSetting<Map<String, NpcInfo>> configSetting) {
        NpcInfo removed = npcInfos.remove(npcName);

        if (removed == null) {
            JsonObject result = new JsonObject();
            result.addProperty("npc_name", npcName);
            result.addProperty("action", "not_found");
            result.addProperty("message", "NPC was not configured");
            return gson.toJson(result);
        }

        configSetting.setValue(npcInfos);

        JsonObject result = new JsonObject();
        result.addProperty("npc_name", npcName);
        result.addProperty("action", "removed");
        return gson.toJson(result);
    }

    private Number parseNumber(Object value) {
        if (value instanceof Number)
            return (Number) value;
        return Double.parseDouble(String.valueOf(value));
    }

    private String error(String message) {
        JsonObject err = new JsonObject();
        err.addProperty("error", message);
        return gson.toJson(err);
    }
}
