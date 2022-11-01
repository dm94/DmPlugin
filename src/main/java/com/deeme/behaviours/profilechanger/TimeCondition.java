package com.deeme.behaviours.profilechanger;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

@Configuration("profile_changer.time_condition")
public class TimeCondition {
    @Option("general.enabled")
    public boolean active = false;

    @Option("profile_changer.time_condition.hour")
    @Number(min = 0, max = 23, step = 1)
    public int hour = 0;

    @Option("profile_changer.time_condition.minute")
    @Number(min = 0, max = 60, step = 1)
    public int minute = 0;
}
