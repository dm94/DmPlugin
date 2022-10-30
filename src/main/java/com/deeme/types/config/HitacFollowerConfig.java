package com.deeme.types.config;

import com.github.manolo8.darkbot.core.manager.StarManager.MapOptions;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;

@Configuration("hitac_follower")
public class HitacFollowerConfig {
    @Option("general.enabled")
    public boolean enable = false;

    @Option("hitac_follower.go_pvp")
    public boolean goToPVP = true;

    @Option("hitac_follower.return_waiting_map")
    public boolean returnToWaitingMap = true;

    @Option("hitac_follower.waiting_map")
    @Dropdown(options = MapOptions.class)
    public int WAIT_MAP = 8;
}
