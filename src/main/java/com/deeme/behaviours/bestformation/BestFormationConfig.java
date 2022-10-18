package com.deeme.behaviours.bestformation;

import java.util.EnumSet;
import java.util.Set;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

@Configuration("best_formation")
public class BestFormationConfig {
    @Option(value = "general.npc_enabled")
    public boolean npcEnabled = true;

    @Option(value = "best_formation.use_veteran")
    public boolean useVeteran = true;

    @Option(value = "general.next_check_time")
    @Number(max = 300, step = 1)
    public int timeToCheck = 5;

    @Option(value = "best_formation.supported_formations")
    @Dropdown(multi = true)
    public transient Set<SupportedFormations> supportedFormations = EnumSet.allOf(SupportedFormations.class);
}