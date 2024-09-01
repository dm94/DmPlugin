package com.deeme.behaviours.urgentdetector;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.shared.config.ProfileNames;
import eu.darkbot.api.config.annotations.Number;

@Configuration("urgent_detector")
public class UrgentDetectorConfig {
    @Option("general.next_check_time")
    @Number(max = 300, step = 1)
    public int timeToCheck = 5;

    @Option("urgent_detector.take_quest")
    public boolean takeQuest = true;

    @Option("urgent_detector.bot_profile")
    @Dropdown(options = ProfileNames.class)
    public String botProfile = "";
}
