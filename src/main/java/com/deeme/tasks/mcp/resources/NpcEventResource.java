package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.managers.NpcEventAPI;

/**
 * Status of NPC events (generic + special). Reports ACTIVE/INACTIVE,
 * remaining time, NPC/boss counts left, event name/id/description.
 */
public class NpcEventResource implements McpResource {

    private final NpcEventAPI npcEvents;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public NpcEventResource(NpcEventAPI npcEvents) {
        this.npcEvents = npcEvents;
    }

    @Override
    public String getUri() {
        return "mcp://npc_event";
    }

    @Override
    public String getName() {
        return "NPC Event";
    }

    @Override
    public String getDescription() {
        return "NPC event status (generic + agatus): active flag, remaining time, NPCs/bosses left.";
    }

    @Override
    public String read(String uri) {
        JsonObject obj = new JsonObject();
        obj.add("generic", eventJson(npcEvents.getEvent(NpcEventAPI.EventType.GENERIC)));
        obj.add("agatus", eventJson(npcEvents.getEvent(NpcEventAPI.EventType.AGATUS)));
        return gson.toJson(obj);
    }

    private JsonObject eventJson(NpcEventAPI.NpcEvent ev) {
        JsonObject o = new JsonObject();
        if (ev == null) {
            Json.put(o, "available", false);
            return o;
        }
        Json.put(o, "available", true);
        if (ev.getStatus() != null)
            o.addProperty("status", ev.getStatus().name());
        Json.put(o, "remaining_seconds", Math.round(ev.getRemainingTime() * 100.0) / 100.0);
        if (ev.getEventName() != null)
            o.addProperty("name", ev.getEventName());
        if (ev.getEventId() != null)
            o.addProperty("event_id", ev.getEventId());
        if (ev.getEventDescription() != null)
            o.addProperty("description", ev.getEventDescription());
        Json.put(o, "npc_left", ev.npcLeft());
        Json.put(o, "boss_npc_left", ev.bossNpcLeft());
        return o;
    }
}
