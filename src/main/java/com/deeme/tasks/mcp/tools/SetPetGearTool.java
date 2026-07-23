package com.deeme.tasks.mcp.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.game.enums.PetGear;
import eu.darkbot.api.managers.PetAPI;
import eu.darkbot.api.utils.ItemNotEquippedException;

import java.util.Map;

/**
 * Enable/disable the hero PET and/or change its active gear.
 * Use {@code enabled} alone, {@code gear} alone, or both together.
 * Pass {@code gear=null} to fall back to the user-configured gear.
 */
public class SetPetGearTool implements McpTool {

    private final PetAPI pet;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public SetPetGearTool(PetAPI pet) {
        this.pet = pet;
    }

    @Override
    public String getName() {
        return "set_pet";
    }

    @Override
    public String getDescription() {
        return "Control the hero PET: enable/disable and/or set its gear. "
                + "Gear is the PetGear enum name (e.g. PASSIVE, GUARD, LOOTER, ENEMY_LOCATOR).";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject enabledProp = new JsonObject();
        enabledProp.addProperty("type", "boolean");
        enabledProp.addProperty("description", "true to enable PET, false to disable.");

        JsonObject gearProp = new JsonObject();
        gearProp.addProperty("type", "string");
        gearProp.addProperty("description", "PetGear enum name. Pass null/empty to reset to user-configured gear.");
        JsonArray gearEnum = new JsonArray();
        for (PetGear g : PetGear.values()) {
            gearEnum.add(new JsonPrimitive(g.name()));
        }
        gearProp.add("enum", gearEnum);

        JsonObject props = new JsonObject();
        props.add("enabled", enabledProp);
        props.add("gear", gearProp);

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", props);
        return schema;
    }

    @Override
    public String call(Map<String, Object> args) {
        boolean hasEnabled = args.containsKey("enabled");
        boolean hasGear = args.containsKey("gear") && !isNullish(args.get("gear"));

        if (!hasEnabled && !hasGear) {
            return error("Provide at least one of: enabled, gear");
        }

        boolean prevEnabled = pet.isEnabled();
        PetGear prevGear = pet.getGear();

        if (hasEnabled) {
            pet.setEnabled(Boolean.parseBoolean(String.valueOf(args.get("enabled"))));
        }

        if (hasGear) {
            String gearStr = String.valueOf(args.get("gear")).trim();
            try {
                if (gearStr.equalsIgnoreCase("null") || gearStr.isEmpty()) {
                    pet.setGear((Integer) null);
                } else {
                    PetGear gear = PetGear.valueOf(gearStr);
                    if (!pet.hasGear(gear)) {
                        return error("PET gear not equipped: " + gearStr);
                    }
                    pet.setGear(gear);
                }
            } catch (IllegalArgumentException e) {
                return error("Unknown PetGear: " + gearStr);
            } catch (ItemNotEquippedException e) {
                return error("Gear not equipped: " + e.getMessage());
            }
        }

        JsonObject result = new JsonObject();
        Json.put(result, "previous_enabled", prevEnabled);
        Json.put(result, "current_enabled", pet.isEnabled());
        if (prevGear != null) {
            result.addProperty("previous_gear", prevGear.name());
        }
        if (pet.getGear() != null) {
            result.addProperty("current_gear", pet.getGear().name());
        }
        return gson.toJson(result);
    }

    private boolean isNullish(Object v) {
        return v == null || "null".equalsIgnoreCase(String.valueOf(v));
    }

    private String error(String message) {
        JsonObject err = new JsonObject();
        err.addProperty("error", message);
        return gson.toJson(err);
    }
}
