package com.deeme.behaviours.profilechanger;

import com.github.manolo8.darkbot.core.manager.StarManager.MapOptions;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

@Configuration("profile_changer.map_timmer_condition")
public class MapTimmerCondition {
    public transient long mapTimeStart = 0;

    @Option("general.enabled")
    public boolean active = false;

    @Option("profile_changer.map_timmer_condition.time_in_map")
    @Number(min = 0, max = 100000, step = 1)
    public int timeInMap = 1;

    @Option("config.general.working_map")
    @Dropdown(options = MapOptions.class)
    public int map = 8;
}
