package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.game.entities.Box;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Mine;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Pet;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.managers.EntitiesAPI;

/**
 * Live snapshot of every entity on the current map: npcs, players, pets,
 * boxes, mines and portals. Supports a {@code ?type=} filter and
 * {@code ?limit=}
 * to cap payload size. The most useful resource for situational awareness.
 */
public class EntitiesResource implements McpResource {

  private static final int DEFAULT_LIMIT = 50;

  private final EntitiesAPI entities;
  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  public EntitiesResource(EntitiesAPI entities) {
    this.entities = entities;
  }

  @Override
  public String getUri() {
    return "mcp://entities";
  }

  @Override
  public String getName() {
    return "Map Entities";
  }

  @Override
  public String getDescription() {
    return "Live entities on the current map. Optional query: ?type=npcs|players|pets|boxes|mines|portals "
        + "and ?limit=N (default 50). Returns id, position and key fields per type.";
  }

  @Override
  public String read(String uri) {
    String type = parseQuery(uri).getOrDefault("type", "");
    int limit = parseInt(parseQuery(uri).get("limit"), DEFAULT_LIMIT);

    JsonObject obj = new JsonObject();
    JsonArray arr = new JsonArray();
    int added = 0;

    if (type.isEmpty() || "npcs".equals(type)) {
      addCount(obj, "npc_count", entities.getNpcs());
      for (Npc e : entities.getNpcs()) {
        if (added >= limit)
          break;
        arr.add(shipJson(e, "npc", npcExtra(e)));
        added++;
      }
    }
    if (type.isEmpty() || "players".equals(type)) {
      addCount(obj, "player_count", entities.getPlayers());
      for (Player e : entities.getPlayers()) {
        if (added >= limit)
          break;
        arr.add(shipJson(e, "player", playerExtra(e)));
        added++;
      }
    }
    if (type.isEmpty() || "pets".equals(type)) {
      addCount(obj, "pet_count", entities.getPets());
      for (Pet e : entities.getPets()) {
        if (added >= limit)
          break;
        arr.add(petJson(e));
        added++;
      }
    }
    if (type.isEmpty() || "boxes".equals(type)) {
      addCount(obj, "box_count", entities.getBoxes());
      for (Box e : entities.getBoxes()) {
        if (added >= limit)
          break;
        arr.add(boxJson(e));
        added++;
      }
    }
    if (type.isEmpty() || "mines".equals(type)) {
      addCount(obj, "mine_count", entities.getMines());
      for (Mine e : entities.getMines()) {
        if (added >= limit)
          break;
        arr.add(entityJson(e, "mine"));
        added++;
      }
    }
    if (type.isEmpty() || "portals".equals(type)) {
      addCount(obj, "portal_count", entities.getPortals());
      for (Portal e : entities.getPortals()) {
        if (added >= limit)
          break;
        arr.add(portalJson(e));
        added++;
      }
    }

    obj.add("entities", arr);
    Json.put(obj, "returned", added);
    return gson.toJson(obj);
  }

  private JsonObject entityJson(Entity e, String type) {
    JsonObject o = baseEntity(e, type);
    return o;
  }

  private JsonObject shipJson(Ship s, String type, JsonObject extra) {
    JsonObject o = baseEntity(s, type);
    Json.put(o, "ship_id", s.getShipId());
    Json.put(o, "invisible", s.isInvisible());
    Json.put(o, "moving", s.isMoving());
    if (extra != null)
      o.add("details", extra);
    return o;
  }

  private JsonObject npcExtra(Npc n) {
    JsonObject o = new JsonObject();
    Json.put(o, "npc_id", n.getNpcId());
    return o;
  }

  private JsonObject playerExtra(Player p) {
    JsonObject o = new JsonObject();
    o.addProperty("ship_type", p.getShipType());
    Json.put(o, "has_pet", p.hasPet());
    if (p.getFormation() != null)
      o.addProperty("formation", p.getFormation().name());
    return o;
  }

  private JsonObject petJson(Pet p) {
    JsonObject o = baseEntity(p, "pet");
    Json.put(o, "level", p.getLevel());
    Json.put(o, "owner_id", p.getOwnerId());
    return o;
  }

  private JsonObject boxJson(Box b) {
    JsonObject o = baseEntity(b, "box");
    o.addProperty("type_name", b.getTypeName());
    o.addProperty("hash", b.getHash());
    return o;
  }

  private JsonObject portalJson(Portal p) {
    JsonObject o = baseEntity(p, "portal");
    Json.put(o, "type_id", p.getTypeId());
    if (p.getPortalType() != null)
      o.addProperty("portal_type", p.getPortalType().name());
    p.getTargetMap().ifPresent(m -> {
      Json.put(o, "target_map_id", m.getId());
      o.addProperty("target_map_name", m.getName());
    });
    Json.put(o, "jumping", p.isJumping());
    return o;
  }

  private JsonObject baseEntity(Entity e, String type) {
    JsonObject o = new JsonObject();
    o.addProperty("type", type);
    Json.put(o, "id", e.getId());
    Json.put(o, "x", round(e.getX()));
    Json.put(o, "y", round(e.getY()));
    Json.put(o, "valid", e.isValid());
    return o;
  }

  private static double round(double v) {
    return Math.round(v * 100.0) / 100.0;
  }

  private void addCount(JsonObject obj, String key, java.util.Collection<?> col) {
    Json.put(obj, key, col == null ? 0 : col.size());
  }

  private static java.util.Map<String, String> parseQuery(String uri) {
    java.util.Map<String, String> params = new java.util.HashMap<>();
    int q = uri.indexOf('?');
    if (q < 0)
      return params;
    for (String pair : uri.substring(q + 1).split("&")) {
      int eq = pair.indexOf('=');
      if (eq > 0)
        params.put(pair.substring(0, eq), pair.substring(eq + 1));
    }
    return params;
  }

  private static int parseInt(String s, int fallback) {
    if (s == null || s.isEmpty())
      return fallback;
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}
