package com.deeme.tasks.mcp.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.managers.ConfigAPI;

import java.util.List;
import java.util.Map;

public class SetProfileTool implements McpTool {

    private final ConfigAPI configAPI;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public SetProfileTool(ConfigAPI configAPI) {
        this.configAPI = configAPI;
    }

    @Override
    public String getName() {
        return "set_profile";
    }

    @Override
    public String getDescription() {
        return "Switch to a different config profile.";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject profileProp = new JsonObject();
        profileProp.addProperty("type", "string");
        profileProp.addProperty("description", "Name of the config profile to switch to.");

        JsonObject props = new JsonObject();
        props.add("profile", profileProp);

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add(new JsonPrimitive("profile"));
        schema.add("required", required);
        return schema;
    }

    @Override
    public String call(Map<String, Object> args) {
        String profile = args.containsKey("profile") ? String.valueOf(args.get("profile")) : "";
        if (profile.isEmpty())
            return error("Missing required parameter 'profile'");

        List<String> profiles = configAPI.getConfigProfiles();
        if (!profiles.contains(profile))
            return error("Profile not found: " + profile + ". Available: " + String.join(", ", profiles));

        configAPI.setConfigProfile(profile);

        JsonObject result = new JsonObject();
        result.addProperty("profile", profile);
        Json.put(result, "success", true);
        return gson.toJson(result);
    }

    private String error(String message) {
        JsonObject err = new JsonObject();
        err.addProperty("error", message);
        return gson.toJson(err);
    }
}
