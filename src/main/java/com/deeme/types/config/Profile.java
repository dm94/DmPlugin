package com.deeme.types.config;

import com.deeme.types.gui.ConfigSupplier;
import com.deeme.types.gui.ShipSupplier;
import com.github.manolo8.darkbot.config.ConfigManager;
import com.github.manolo8.darkbot.config.types.Option;

import eu.darkbot.api.config.annotations.Dropdown;

@Option("Profile")
public class Profile {
    @Option(value = "Hangar ID", description = "Hangar to use. Must be in favourites.")
    @Dropdown(options = ShipSupplier.class)
    public Integer hangarId = null;

    @Option(value = "Bot profile to set")
    @Dropdown(options = ConfigSupplier.class)
    public String BOT_PROFILE = ConfigManager.DEFAULT;
}