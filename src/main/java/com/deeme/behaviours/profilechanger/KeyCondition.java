package com.deeme.behaviours.profilechanger;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.game.stats.Stats.BootyKey;

@Configuration("profile_changer.key_condition")
public class KeyCondition {
    @Option("general.enabled")
    public boolean active = false;

    @Option("profile_changer.key_condition.key_to_check")
    @Dropdown(options = BootyKeySupplier.class)
    public BootyKey key = BootyKey.GREEN;
}
