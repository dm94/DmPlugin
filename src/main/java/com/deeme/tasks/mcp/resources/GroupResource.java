package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.game.group.GroupMember;
import eu.darkbot.api.managers.GroupAPI;

/**
 * In-game group (outfit) status: id, size, leader flag, open flag,
 * and list of members with their location and state.
 */
public class GroupResource implements McpResource {

    private final GroupAPI group;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public GroupResource(GroupAPI group) {
        this.group = group;
    }

    @Override
    public String getUri() {
        return "mcp://group";
    }

    @Override
    public String getName() {
        return "Group Status";
    }

    @Override
    public String getDescription() {
        return "In-game group: id, size, leader flag, open flag, invites count, and members list.";
    }

    @Override
    public String read(String uri) {
        JsonObject obj = new JsonObject();
        Json.put(obj, "in_group", group.hasGroup());
        if (!group.hasGroup()) {
            return gson.toJson(obj);
        }

        Json.put(obj, "id", group.getId());
        Json.put(obj, "size", group.getSize());
        Json.put(obj, "max_size", group.getMaxSize());
        Json.put(obj, "is_leader", group.isLeader());
        Json.put(obj, "is_open", group.isOpen());
        Json.put(obj, "can_invite", group.canInvite());

        JsonArray members = new JsonArray();
        for (GroupMember m : group.getMembers()) {
            if (m == null) continue;
            JsonObject mo = new JsonObject();
            Json.put(mo, "id", m.getId());
            mo.addProperty("username", m.getUsername());
            Json.put(mo, "map_id", m.getMapId());
            Json.put(mo, "level", m.getLevel());
            Json.put(mo, "leader", m.isLeader());
            Json.put(mo, "dead", m.isDead());
            Json.put(mo, "attacked", m.isAttacked());
            Json.put(mo, "cloaked", m.isCloaked());
            Json.put(mo, "locked", m.isLocked());
            if (m.getLocation() != null) {
                JsonObject loc = new JsonObject();
                Json.put(loc, "x", Math.round(m.getLocation().getX() * 100.0) / 100.0);
                Json.put(loc, "y", Math.round(m.getLocation().getY() * 100.0) / 100.0);
                mo.add("location", loc);
            }
            members.add(mo);
        }
        obj.add("members", members);

        JsonArray invites = new JsonArray();
        for (GroupMember.Invite inv : group.getInvites()) {
            if (inv == null) continue;
            JsonObject io = new JsonObject();
            Json.put(io, "incoming", inv.isIncoming());
            Json.put(io, "valid", inv.isValid());
            if (inv.getInviter() != null)
                io.addProperty("inviter", inv.getInviter().getUsername());
            if (inv.getInvited() != null)
                io.addProperty("invited", inv.getInvited().getUsername());
            invites.add(io);
        }
        obj.add("invites", invites);

        return gson.toJson(obj);
    }
}
