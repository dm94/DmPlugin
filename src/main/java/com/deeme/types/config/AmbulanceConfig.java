package com.deeme.types.config;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Percentage;

@Configuration("ambulance")
public class AmbulanceConfig {
    @Option("general.enabled")
    public boolean enable = false;

    @Option("ambulance.ship_type")
    @Dropdown
    public AvailableShips shipType = AvailableShips.AEGIS;

    @Option("general.next_check_time")
    @Number(min = 1, max = 300, step = 1)
    public int timeToCheck = 20;

    @Option("ambulance.max_health")
    @Percentage
    public double healthToRepair = 0.5;

    @Option("ambulance.repair_shield")
    public boolean repairShield = true;
}
