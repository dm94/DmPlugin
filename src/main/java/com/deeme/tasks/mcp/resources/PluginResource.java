package com.deeme.tasks.mcp.resources;

import com.deeme.tasks.mcp.util.Json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.darkbot.api.extensions.PluginInfo;
import eu.darkbot.api.managers.ExtensionsAPI;

public class PluginResource implements McpResource {

    private final ExtensionsAPI extensions;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public PluginResource(ExtensionsAPI extensions) {
        this.extensions = extensions;
    }

    @Override
    public String getUri() {
        return "bot://plugins";
    }

    @Override
    public String getName() {
        return "Plugin List";
    }

    @Override
    public String getDescription() {
        return "List of loaded plugins and their status";
    }

    @Override
    public String read(String uri) {
        JsonObject obj = new JsonObject();

        JsonArray loaded = new JsonArray();
        for (PluginInfo pl : extensions.getPluginInfos()) {
            JsonObject p = new JsonObject();
            Json.put(p, "name", pl.getName());
            Json.put(p, "author", pl.getAuthor());
            if (pl.getVersion() != null)
                Json.put(p, "version", pl.getVersion().toString());
            loaded.add(p);
        }
        obj.add("loaded", loaded);

        return gson.toJson(obj);
    }
}
