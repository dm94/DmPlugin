package com.deeme.types.config;

import java.util.HashMap;
import java.util.Map;

import com.deeme.types.gui.JDayChangeTable;
import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.config.types.Option;

public class WeeklyConfig {
    @Option("Activate")
    public boolean activate = false;

    @Option()
    @Editor(value = JDayChangeTable.class, shared = true)
    public Map<String, Hour> Hours_Changes = new HashMap<>();

    @Option(value = "Change hangar", description = "It'll change to the hangar you've put in each profile. Do not use with other modules that change hangar.")
    public boolean changeHangar = false;

    @Option(value = "Profile 1", description = "To use this profile P1 in the schedule")
    public Profile profile1 = new Profile();

    @Option(value = "Profile 2", description = "To use this profile P2 in the schedule")
    public Profile profile2 = new Profile();

    @Option(value = "Profile 3", description = "To use this profile P3 in the schedule")
    public Profile profile3 = new Profile();

    @Option(value = "Profile 4", description = "To use this profile P4 in the schedule")
    public Profile profile4 = new Profile();
}
