package com.deeme.behaviours.profilechanger;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

@Configuration("profile_changer.deaths_condition")
public class DeathsCondition {

    @Option("general.enabled")
    public boolean active = false;

    @Option("profile_changer.deaths_condition.max_deaths")
    @Number(min = 0, max = 100000, step = 1)
    public int maxDeaths = 10;
}
