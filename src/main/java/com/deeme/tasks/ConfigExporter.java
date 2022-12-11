package com.deeme.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JComponent;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.config.utils.ByteArrayToBase64TypeAdapter;
import com.github.manolo8.darkbot.config.utils.ColorAdapter;
import com.github.manolo8.darkbot.config.utils.ConditionTypeAdapterFactory;
import com.github.manolo8.darkbot.config.utils.FontAdapter;
import com.github.manolo8.darkbot.config.utils.PlayerTagTypeAdapterFactory;
import com.github.manolo8.darkbot.config.utils.SpecialTypeAdapter;
import com.github.manolo8.darkbot.gui.tree.editors.FileEditor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.types.PercentRange;
import eu.darkbot.api.config.types.ShipMode;

import java.awt.*;

import eu.darkbot.api.events.Listener;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.extensions.ExtraMenus;

@Feature(name = "Config Exporter", description = "Export your actual config")
public class ConfigExporter implements Task, Listener, ExtraMenus {
    public final Path exportFolder = Paths.get("configexp");
    protected final Main main;
    protected final PluginAPI api;

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setLenient()
            .registerTypeAdapter(byte[].class, new ByteArrayToBase64TypeAdapter())
            .registerTypeAdapter(Color.class, new ColorAdapter())
            .registerTypeAdapter(File.class, new FileEditor.JsonAdapter())
            .registerTypeAdapter(Font.class, new FontAdapter())
            .registerTypeAdapter(ShipMode.class, (InstanceCreator<ShipMode>) type -> new Config.ShipConfig())
            .registerTypeAdapter(PercentRange.class, (InstanceCreator<PercentRange>) type -> new Config.PercentRange())
            .registerTypeAdapterFactory(new SpecialTypeAdapter())
            .registerTypeAdapterFactory(new ConditionTypeAdapterFactory())
            .registerTypeAdapterFactory(new PlayerTagTypeAdapterFactory())
            .create();

    public ConfigExporter(Main main, PluginAPI api) {
        this.main = main;
        this.api = api;

        try {
            if (!Files.exists(exportFolder)) {
                Files.createDirectory(exportFolder);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTickTask() {
        // Nothing to do
    }

    @Override
    public Collection<JComponent> getExtraMenuItems(PluginAPI pluginAPI) {
        return Arrays.asList(
                create("Export config", e -> exportConfig()));
    }

    private void exportConfig() {
        try {
            Config config = getClearConfig();

            File f = new File("configexp", this.main.configManager.getConfigName() + ".json");
            Writer writer = new FileWriter(f);
            GSON.toJson(config, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Config getClearConfig() {
        Config config = this.main.configManager.getConfig();
        config.PLAYER_INFOS.clear();

        cleanDisabledPlugins(config);

        config = deletePluginInfo(config, "eu.darkbot.ter.dks.tasks.RemoteStats");

        return config;
    }

    private Config cleanDisabledPlugins(Config config) {
        ArrayList<String> disabledFeatures = new ArrayList<String>();

        config.PLUGIN_INFOS.forEach((key, plConfig) -> {
            for (String plName : plConfig.DISABLED_FEATURES) {
                disabledFeatures.add(plName);
            }
            plConfig.DISABLED_FEATURES.clear();
        });

        for (String plName : disabledFeatures) {
            config = deletePluginInfo(config, plName);
        }

        return config;
    }

    private Config deletePluginInfo(Config config, String pluginFeature) {
        Object featureConf = config.CUSTOM_CONFIGS.get(pluginFeature);
        if (featureConf != null) {
            config.CUSTOM_CONFIGS.remove(pluginFeature);
        }

        return config;
    }

}
