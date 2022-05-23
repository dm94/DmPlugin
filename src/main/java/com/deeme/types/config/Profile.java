package com.deeme.types.config;

import com.deeme.types.gui.ConfigSupplier;
import com.deeme.types.gui.ShipSupplier;
import com.github.manolo8.darkbot.config.ConfigManager;
import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.config.types.Options;
import com.github.manolo8.darkbot.gui.tree.components.JListField;

@Option("Profile")
public class Profile {
    @Option(value = "Hangar ID", description = "Hangar to use. Must be in favourites.")
    @Editor(JListField.class)
    @Options(ShipSupplier.class)
    public Integer hangarId = null;

    @Option(value = "Bot profile to set")
    @Editor(JListField.class)
    @Options(ConfigSupplier.class)
    public String BOT_PROFILE = ConfigManager.DEFAULT;
}