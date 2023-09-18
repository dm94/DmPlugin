package com.deeme.types.config;

import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Configuration;

@Configuration("auto_cloack")
public class AutoCloak {
    @Option("general.enabled")
    public boolean autoCloakShip = false;

    @Option("auto_cloack.waiting_time")
    @Number(min = 0, max = 1000, step = 1)
    public int secondsOfWaiting = 10;

    @Option("auto_cloack.only_pvp")
    public boolean onlyPvpMaps = false;
}