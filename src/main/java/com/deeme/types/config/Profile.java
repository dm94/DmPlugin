package com.deeme.types.config;

import com.deeme.types.gui.ConfigSupplier;
import com.deeme.types.gui.ShipSupplier;
import com.github.manolo8.darkbot.config.ConfigManager;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Dropdown;

@Configuration("general.profile")
public class Profile {
    @Option("general.hangar_id")
    @Dropdown(options = ShipSupplier.class)
    public Integer hangarId = null;

    @Option("general.bot_profile")
    @Dropdown(options = ConfigSupplier.class)
    public String BOT_PROFILE = ConfigManager.DEFAULT;
}