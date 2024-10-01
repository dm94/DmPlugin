package com.deeme.types.config;

import com.deeme.types.gui.ShipSupplier;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.shared.config.ProfileNames;
import eu.darkbot.api.config.annotations.Dropdown;

@Configuration("general.profile")
public class Profile {
    @Option("general.hangar_id")
    @Dropdown(options = ShipSupplier.class)
    public Integer hangarId = null;

    @Option("general.bot_profile")
    @Dropdown(options = ProfileNames.class)
    public String BOT_PROFILE = "config";
}