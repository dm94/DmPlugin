package com.deeme.tasks.mcp.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import eu.darkbot.api.managers.BotAPI;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;

public class PluginReloadTool implements McpTool {

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final BotAPI bot;

    public PluginReloadTool(BotAPI bot) {
        this.bot = bot;
    }

    @Override
    public String getName() {
        return "reload_plugins";
    }

    @Override
    public String getDescription() {
        return "Reload all plugins. Useful for applying configuration changes or fixing plugin issues without restarting the bot. The MCP needs to be reset";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        return schema;
    }

    @Override
    public String call(Map<String, Object> args) {
        try {
            Class<?> handlerClass = Class.forName("com.github.manolo8.darkbot.extensions.plugins.PluginHandler");
            Class<?> mainClass = bot.getClass();

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle getter = lookup.findGetter(mainClass, "pluginHandler", handlerClass);
            Object pluginHandler = getter.invoke(bot);

            MethodHandle updater = lookup.findVirtual(handlerClass, "updatePlugins", MethodType.methodType(void.class));
            updater.invoke(pluginHandler);

            JsonObject result = new JsonObject();
            result.add("success", new JsonPrimitive(true));
            result.addProperty("message", "Plugin reload triggered successfully");
            return gson.toJson(result);
        } catch (Throwable e) {
            JsonObject result = new JsonObject();
            result.add("success", new JsonPrimitive(false));
            result.addProperty("error", "Failed to reload plugins: " + e.getMessage());
            return gson.toJson(result);
        }
    }
}
