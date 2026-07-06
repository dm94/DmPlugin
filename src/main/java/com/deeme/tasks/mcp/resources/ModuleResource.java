package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.StarSystemAPI;

public class ModuleResource implements McpResource {

    private final BotAPI bot;
    private final StarSystemAPI starSystem;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public ModuleResource(BotAPI bot, StarSystemAPI starSystem) {
        this.bot = bot;
        this.starSystem = starSystem;
    }

    @Override
    public String getUri() {
        return "bot://module";
    }

    @Override
    public String getName() {
        return "Module Status";
    }

    @Override
    public String getDescription() {
        return "Current module status: name, status text, running state";
    }

    @Override
    public String read(String uri) {
        JsonObject obj = new JsonObject();
        Module module = bot.getModule();
        Json.put(obj, "running", bot.isRunning());

        if (module != null) {
            obj.addProperty("module_name", module.getClass().getSimpleName());
            obj.addProperty("module_class", module.getClass().getName());

            String status = module.getStatus();
            if (status != null)
                obj.addProperty("status", status);

            String stoppedStatus = module.getStoppedStatus();
            if (stoppedStatus != null)
                obj.addProperty("stopped_status", stoppedStatus);
        } else {
            obj.addProperty("module_name", "(none)");
        }

        if (starSystem.getCurrentMap() != null) {
            Json.put(obj, "map_id", starSystem.getCurrentMap().getId());
            obj.addProperty("map_name", starSystem.getCurrentMap().getName());
        }

        return gson.toJson(obj);
    }
}
