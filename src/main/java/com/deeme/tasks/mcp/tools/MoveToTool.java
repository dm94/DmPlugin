package com.deeme.tasks.mcp.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.managers.MovementAPI;

import java.util.Map;

/**
 * Movement control: set destination by coordinates, move to a random spot,
 * or stop the ship. Useful for testing navigation modules and unsticking.
 */
public class MoveToTool implements McpTool {

    private final MovementAPI movement;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public MoveToTool(MovementAPI movement) {
        this.movement = movement;
    }

    @Override
    public String getName() {
        return "move";
    }

    @Override
    public String getDescription() {
        return "Move the hero. Actions: 'goto' (requires x,y), 'random', or 'stop'. "
                + "Use stop_hard=true with 'stop' to forcibly halt in place.";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject actionProp = new JsonObject();
        actionProp.addProperty("type", "string");
        actionProp.addProperty("description", "Movement action.");
        com.google.gson.JsonArray actionEnum = new com.google.gson.JsonArray();
        actionEnum.add(new com.google.gson.JsonPrimitive("goto"));
        actionEnum.add(new com.google.gson.JsonPrimitive("random"));
        actionEnum.add(new com.google.gson.JsonPrimitive("stop"));
        actionProp.add("enum", actionEnum);

        JsonObject xProp = new JsonObject();
        xProp.addProperty("type", "number");
        xProp.addProperty("description", "Target X coordinate. Required for 'goto'.");

        JsonObject yProp = new JsonObject();
        yProp.addProperty("type", "number");
        yProp.addProperty("description", "Target Y coordinate. Required for 'goto'.");

        JsonObject stopHardProp = new JsonObject();
        stopHardProp.addProperty("type", "boolean");
        stopHardProp.addProperty("description", "For 'stop': true to hard-stop in current location (default false).");

        JsonObject props = new JsonObject();
        props.add("action", actionProp);
        props.add("x", xProp);
        props.add("y", yProp);
        props.add("stop_hard", stopHardProp);

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", props);
        com.google.gson.JsonArray required = new com.google.gson.JsonArray();
        required.add(new com.google.gson.JsonPrimitive("action"));
        schema.add("required", required);
        return schema;
    }

    @Override
    public String call(Map<String, Object> args) {
        String action = args.containsKey("action") ? String.valueOf(args.get("action")) : "";
        switch (action) {
            case "goto":
                return doGoto(args);
            case "random":
                movement.moveRandom();
                return resultJson("random", null, null);
            case "stop":
                boolean hard = args.containsKey("stop_hard")
                        && Boolean.parseBoolean(String.valueOf(args.get("stop_hard")));
                movement.stop(hard);
                return resultJson("stop", null, null);
            default:
                return error("Unknown or missing action. Use: goto, random, stop");
        }
    }

    private String doGoto(Map<String, Object> args) {
        if (!args.containsKey("x") || !args.containsKey("y"))
            return error("'goto' requires both x and y");
        try {
            double x = parseNumber(args.get("x")).doubleValue();
            double y = parseNumber(args.get("y")).doubleValue();
            if (!movement.canMove(x, y))
                return error("Cannot move to (" + x + "," + y + "): out of map or inside an obstacle");
            movement.moveTo(x, y);
            return resultJson("goto", x, y);
        } catch (NumberFormatException e) {
            return error("Invalid coordinates: " + e.getMessage());
        }
    }

    private String resultJson(String action, Double x, Double y) {
        JsonObject o = new JsonObject();
        o.addProperty("action", action);
        if (x != null) Json.put(o, "x", Math.round(x * 100.0) / 100.0);
        if (y != null) Json.put(o, "y", Math.round(y * 100.0) / 100.0);
        JsonObject dest = new JsonObject();
        Json.put(dest, "x", Math.round(movement.getCurrentLocation().getX() * 100.0) / 100.0);
        Json.put(dest, "y", Math.round(movement.getCurrentLocation().getY() * 100.0) / 100.0);
        o.add("current_location", dest);
        Json.put(o, "moving", movement.isMoving());
        return gson.toJson(o);
    }

    private Number parseNumber(Object value) {
        if (value instanceof Number) return (Number) value;
        return Double.parseDouble(String.valueOf(value));
    }

    private String error(String message) {
        JsonObject err = new JsonObject();
        err.addProperty("error", message);
        return gson.toJson(err);
    }
}
