package com.deeme.tasks.mcp;

import java.util.Arrays;

import com.deeme.tasks.mcp.conditions.ConditionSchemaResource;
import com.deeme.tasks.mcp.resources.BotResource;
import com.deeme.tasks.mcp.resources.ConfigTreeResource;
import com.deeme.tasks.mcp.resources.ConfigValueResource;
import com.deeme.tasks.mcp.resources.HeroResource;
import com.deeme.tasks.mcp.resources.InspectResource;
import com.deeme.tasks.mcp.resources.ModuleResource;
import com.deeme.tasks.mcp.resources.NpcConfigResource;
import com.deeme.tasks.mcp.resources.PluginResource;
import com.deeme.tasks.mcp.resources.ProfileListResource;
import com.deeme.tasks.mcp.resources.StatsResource;
import com.deeme.tasks.mcp.server.McpHttpServer;
import com.deeme.tasks.mcp.server.McpProtocol;
import com.deeme.tasks.mcp.tools.BotControlTool;
import com.deeme.tasks.mcp.tools.BuildConditionTool;
import com.deeme.tasks.mcp.tools.PluginReloadTool;
import com.deeme.tasks.mcp.tools.ResourceTool;
import com.deeme.tasks.mcp.tools.SetConfigTool;
import com.deeme.tasks.mcp.tools.SetNpcConfigTool;
import com.deeme.tasks.mcp.tools.SetProfileTool;
import com.deeme.tasks.mcp.tools.ValidateConditionTool;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.api.extensions.Installable;
import eu.darkbot.api.extensions.PluginInfo;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.managers.StatsAPI;

@Feature(name = "MCP Bridge", description = "Exposes bot state & control via MCP protocol for AI integration")
public class McpBridge implements Task, Configurable<McpConfig>, Installable {

    private McpHttpServer server;
    private boolean started;

    public static volatile McpHttpServer liveServer;
    private static volatile PluginAPI pluginAPI;

    public static PluginAPI getPluginAPI() {
        return pluginAPI;
    }

    public McpBridge(PluginAPI api, BotAPI bot, HeroAPI hero, StatsAPI stats,
            ExtensionsAPI extensions, StarSystemAPI starSystem,
            ConfigAPI configAPI, AuthAPI auth) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        VerifierChecker.requireAuthenticity(auth);
        ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
        FeatureInfo<?> feature = extensionsAPI.getFeatureInfo(this.getClass());
        Utils.discordCheck(feature, auth.getAuthId());
        Utils.showDonateDialog(feature, auth.getAuthId());

        String pName = "MCP Bridge";
        String pVersion = "1.0.0";
        for (PluginInfo pi : extensions.getPluginInfos()) {
            if ("com.deeme".equals(pi.getBasePackage())) {
                pName = pi.getName();
                if (pi.getVersion() != null)
                    pVersion = pi.getVersion().toString();
                break;
            }
        }
        McpProtocol protocol = new McpProtocol(pName, pVersion);
        protocol.registerResource(new BotResource(bot, starSystem));
        protocol.registerResource(new HeroResource(hero, starSystem));
        protocol.registerResource(new StatsResource(stats, bot));
        protocol.registerResource(new ModuleResource(bot, starSystem));
        protocol.registerResource(new PluginResource(extensions));
        protocol.registerResource(new ProfileListResource(configAPI));
        protocol.registerResource(new ConfigValueResource(configAPI, bot));
        protocol.registerResource(new ConfigTreeResource(configAPI));
        protocol.registerResource(new InspectResource(bot, hero, stats, extensions, starSystem, configAPI));
        protocol.registerResource(new NpcConfigResource(configAPI));
        protocol.registerResource(new ConditionSchemaResource());
        protocol.registerTool(new ResourceTool(protocol.getResources()));
        protocol.registerTool(new BotControlTool(bot));
        protocol.registerTool(new PluginReloadTool(bot));
        protocol.registerTool(new SetConfigTool(configAPI));
        protocol.registerTool(new SetNpcConfigTool(configAPI));
        protocol.registerTool(new SetProfileTool(configAPI));
        protocol.registerTool(new ValidateConditionTool());
        protocol.registerTool(new BuildConditionTool());

        this.server = new McpHttpServer(new McpConfig(), protocol);
        liveServer = this.server;
    }

    @Override
    public void onTickTask() {
        onBackgroundTick();
    }

    @Override
    public void onBackgroundTick() {
        if (!started) {
            started = true;
            server.start();
        }
    }

    @Override
    public void setConfig(ConfigSetting<McpConfig> setting) {
        McpConfig cfg = setting.getValue();
        if (server != null && (!cfg.host.equals(server.getHost()) || cfg.port != server.getPort())) {
            server.stop();
            server = new McpHttpServer(cfg, server.getProtocol());
            server.start();
            liveServer = server;
        }
    }

    @Override
    public void install(PluginAPI pluginAPI) {
        McpBridge.pluginAPI = pluginAPI;
    }

    @Override
    public void uninstall() {
        if (server != null)
            server.stop();
    }
}
