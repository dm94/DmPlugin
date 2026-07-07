package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.NpcInfo;
import eu.darkbot.api.managers.ConfigAPI;

import java.util.HashMap;
import java.util.Map;

public class NpcConfigResource implements McpResource {

    private final ConfigAPI configAPI;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public NpcConfigResource(ConfigAPI configAPI) {
        this.configAPI = configAPI;
    }

    @Override
    public String getUri() {
        return "bot://config/npc";
    }

    @Override
    public String getName() {
        return "NPC Config";
    }

    @Override
    public String getDescription() {
        return "Get NPC configuration. Use empty path to list all NPCs, "
                + "or provide an NPC name to get its specific configuration. "
                + "NPCs are stored under 'loot.npc_infos'.";
    }

    @Override
    public String read(String uri) {
        String npcName = extractNpcName(uri);

        ConfigSetting<Map<String, NpcInfo>> configSetting = configAPI.requireConfig("loot.npc_infos");
        Map<String, NpcInfo> npcInfos = configSetting.getValue();

        if (npcInfos == null) {
            JsonObject result = new JsonObject();
            result.addProperty("npc_count", 0);
            result.add("npcs", new JsonArray());
            return gson.toJson(result);
        }

        if (npcName.isEmpty()) {
            return listAllNpcs(npcInfos);
        }

        return getNpcDetails(npcName, npcInfos);
    }

    private String listAllNpcs(Map<String, NpcInfo> npcInfos) {
        JsonObject result = new JsonObject();
        result.addProperty("npc_count", npcInfos.size());

        JsonArray arr = new JsonArray();
        for (Map.Entry<String, NpcInfo> entry : npcInfos.entrySet()) {
            JsonObject item = new JsonObject();
            item.addProperty("name", entry.getKey());

            NpcInfo info = entry.getValue();
            if (info != null) {
                Json.put(item, "should_kill", info.getShouldKill());
                item.addProperty("priority", info.getPriority());
                item.addProperty("radius", info.getRadius());
                if (info.getAmmo().isPresent()) {
                    item.addProperty("ammo", info.getAmmo().get().getId());
                }
                if (info.getFormation().isPresent()) {
                    item.addProperty("formation", info.getFormation().get().getId());
                }
            }

            arr.add(item);
        }

        result.add("npcs", arr);
        return gson.toJson(result);
    }

    private String getNpcDetails(String npcName, Map<String, NpcInfo> npcInfos) {
        NpcInfo info = npcInfos.get(npcName);

        JsonObject result = new JsonObject();
        result.addProperty("name", npcName);

        if (info == null) {
            Json.put(result, "configured", false);
            result.addProperty("message", "NPC not found in configuration. Create it in DarkBot's UI first.");
            return gson.toJson(result);
        }

        Json.put(result, "configured", true);
        Json.put(result, "should_kill", info.getShouldKill());
        result.addProperty("priority", info.getPriority());
        result.addProperty("radius", info.getRadius());

        info.getAmmo().ifPresent(ammo -> {
            result.addProperty("ammo", ammo.getId());
        });

        info.getFormation().ifPresent(formation -> {
            result.addProperty("formation", formation.getId());
        });

        result.add("map_ids", gson.toJsonTree(info.getMapIds()));

        return gson.toJson(result);
    }

    private String extractNpcName(String uri) {
        Map<String, String> query = parseQuery(uri);
        if (query.containsKey("name"))
            return query.get("name");

        String prefix = getUri();
        if (uri.startsWith(prefix + "/")) {
            String raw = uri.substring(prefix.length() + 1);
            int qmark = raw.indexOf('?');
            return qmark >= 0 ? raw.substring(0, qmark) : raw;
        }
        return "";
    }

    private Map<String, String> parseQuery(String uri) {
        Map<String, String> params = new HashMap<>();
        int qmark = uri.indexOf('?');
        if (qmark < 0)
            return params;
        String query = uri.substring(qmark + 1);
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                params.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return params;
    }
}
