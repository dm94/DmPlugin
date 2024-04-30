package com.deeme.types.config;

import java.util.HashMap;
import java.util.Map;

import com.deeme.types.gui.JDayChangeTable;

import com.github.manolo8.darkbot.config.types.Editor;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;

@Configuration("weekly_schedule")
public class WeeklyConfig {
    @Option("general.enabled")
    public boolean activate = false;

    @Option("general.next_check_time_minutes")
    @Number(min = 1, max = 60, step = 1)
    public int timeToCheck = 5;

    @Option("weekly_schedule.change_hangar")
    public boolean changeHangar = false;

    @Option("weekly_schedule.thirty_minutes")
    public boolean thirtyMinutes = false;

    @Option("weekly_schedule.p1")
    public Profile profile1 = new Profile();

    @Option("weekly_schedule.p2")
    public Profile profile2 = new Profile();

    @Option("weekly_schedule.p3")
    public Profile profile3 = new Profile();

    @Option("weekly_schedule.p4")
    public Profile profile4 = new Profile();

    @Option()
    @Editor(value = JDayChangeTable.class, shared = true)
    public Map<String, Hour> Hours_Changes = new HashMap<>();
}
