package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import eu.darkbot.api.managers.ConfigAPI;

import java.util.List;

public class ProfileListResource implements McpResource {

    private final ConfigAPI configAPI;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public ProfileListResource(ConfigAPI configAPI) {
        this.configAPI = configAPI;
    }

    @Override
    public String getUri() {
        return "bot://profiles";
    }

    @Override
    public String getName() {
        return "Config Profiles";
    }

    @Override
    public String getDescription() {
        return "List all available config profiles.";
    }

    @Override
    public String read(String uri) {
        List<String> profiles = configAPI.getConfigProfiles();

        JsonArray arr = new JsonArray();
        for (String p : profiles)
            arr.add(new JsonPrimitive(p));

        JsonObject result = new JsonObject();
        result.add("profiles", arr);
        result.addProperty("count", profiles.size());
        return gson.toJson(result);
    }
}
