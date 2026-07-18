package com.deeme.tasks.mcp.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import com.deeme.tasks.mcp.modules.TargetKillerModule;
import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Pet;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.EntitiesAPI;

import java.util.Map;

/**
 * Select an entity by id and order the bot to attack it via a temporary module
 * ({@link TargetKillerModule}) that takes over until the target dies or
 * {@code timeout_seconds} elapses, then resumes the user's previous module.
 *
 * <p>Action {@code stop} aborts an ongoing MCP attack and returns control to the
 * user module. The tool returns immediately (asynchronous); poll
 * {@code mcp://entities} or {@code bot://module} to track progress.</p>
 */
public class AttackEntityTool implements McpTool {

    public static final long DEFAULT_TIMEOUT_SECONDS = 120L;
    public static final long MAX_TIMEOUT_SECONDS = 1800L;

    private static final String ACTION_ATTACK = "attack";
    private static final String ACTION_STOP = "stop";

    private final PluginAPI api;
    private final BotAPI bot;
    private final EntitiesAPI entities;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public AttackEntityTool(PluginAPI api, BotAPI bot, EntitiesAPI entities) {
        this.api = api;
        this.bot = bot;
        this.entities = entities;
    }

    @Override
    public String getName() {
        return "attack_entity";
    }

    @Override
    public String getDescription() {
        return "Select an entity by id and make the bot attack it (via a temporary module) until it dies "
                + "or timeout_seconds elapses, then resume the previous module. "
                + "Action 'stop' aborts the current MCP attack. Returns immediately; "
                + "poll mcp://entities or bot://module for progress.";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject actionProp = new JsonObject();
        actionProp.addProperty("type", "string");
        actionProp.addProperty("description", "'attack' (default) to hunt entity_id, or 'stop' to abort.");
        JsonArray actionEnum = new JsonArray();
        actionEnum.add(new JsonPrimitive(ACTION_ATTACK));
        actionEnum.add(new JsonPrimitive(ACTION_STOP));
        actionProp.add("enum", actionEnum);

        JsonObject idProp = new JsonObject();
        idProp.addProperty("type", "integer");
        idProp.addProperty("description", "Entity id to attack (from mcp://entities). Required for 'attack'.");

        JsonObject timeoutProp = new JsonObject();
        timeoutProp.addProperty("type", "integer");
        timeoutProp.addProperty("description",
                "Max seconds to chase the target before giving up (default 120, 0 = no timeout, max 1800).");

        JsonObject props = new JsonObject();
        props.add("action", actionProp);
        props.add("entity_id", idProp);
        props.add("timeout_seconds", timeoutProp);

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", props);
        return schema;
    }

    @Override
    public String call(Map<String, Object> args) {
        String action = args.containsKey("action") ? String.valueOf(args.get("action")) : ACTION_ATTACK;

        if (ACTION_STOP.equals(action)) {
            return stopAttack();
        }
        if (!ACTION_ATTACK.equals(action)) {
            return error("Unknown action. Use 'attack' or 'stop'.");
        }

        Integer id = parseId(args.get("entity_id"));
        if (id == null) {
            return error("'attack' requires entity_id");
        }

        long timeout = parseTimeout(args.get("timeout_seconds"));
        Entity found = findEntity(id);
        if (found == null) {
            return error("No entity with id " + id + " on the current map");
        }

        abortCurrentHunter();

        TargetKillerModule module = new TargetKillerModule(api, bot, id, timeout);
        bot.setModule(module);

        JsonObject result = new JsonObject();
        result.addProperty("action", ACTION_ATTACK);
        Json.put(result, "entity_id", id);
        result.addProperty("entity_type", typeOf(found));
        Json.put(result, "timeout_seconds", timeout);
        result.addProperty("status", "hunting");
        return gson.toJson(result);
    }

    private String stopAttack() {
        boolean wasHunter = abortCurrentHunter();
        JsonObject result = new JsonObject();
        result.addProperty("action", ACTION_STOP);
        Json.put(result, "was_hunting", wasHunter);
        result.addProperty("status", wasHunter ? "aborted" : "idle");
        return gson.toJson(result);
    }

    /** If a {@link TargetKillerModule} is active, stop it. Returns true if one was stopped. */
    private boolean abortCurrentHunter() {
        Module current = bot.getModule();
        if (current instanceof TargetKillerModule) {
            ((TargetKillerModule) current).stop();
            return true;
        }
        return false;
    }

    private Entity findEntity(int id) {
        for (Npc n : entities.getNpcs()) {
            if (n.getId() == id) {
                return n;
            }
        }
        for (Player p : entities.getPlayers()) {
            if (p.getId() == id) {
                return p;
            }
        }
        for (Pet p : entities.getPets()) {
            if (p.getId() == id) {
                return p;
            }
        }
        return null;
    }

    private static String typeOf(Entity e) {
        if (e instanceof Npc) {
            return "npc";
        }
        if (e instanceof Player) {
            return "player";
        }
        if (e instanceof Pet) {
            return "pet";
        }
        return e.getClass().getSimpleName();
    }

    private static Integer parseId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long parseTimeout(Object value) {
        if (value == null) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
        try {
            long t = Long.parseLong(String.valueOf(value).trim());
            if (t < 0L) {
                return 0L;
            }
            return Math.min(t, MAX_TIMEOUT_SECONDS);
        } catch (NumberFormatException e) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
    }

    private String error(String message) {
        JsonObject err = new JsonObject();
        err.addProperty("error", message);
        return gson.toJson(err);
    }
}
