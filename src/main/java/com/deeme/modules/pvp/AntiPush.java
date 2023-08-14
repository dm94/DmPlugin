package com.deeme.modules.pvp;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

@Configuration("anti_push")
public class AntiPush {

    @Option("general.enabled")
    public boolean enable = false;

    @Option("anti_push.max_kills")
    @Number(min = 0, max = 20, step = 1)
    public int maxKills = 4;
}
