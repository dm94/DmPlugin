package com.deeme.modules.genericgate;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

@Configuration("generic_gate")
public class Config {
    @Option("travel_map")
    public TravelMap travelMap = new TravelMap();

    @Option("generic_gate.min_radius")
    @Number(min = 300, max = 1000, step = 10)
    public int radioMin = 560;

    @Option("generic_gate.away_distance")
    @Number(min = 600, max = 6000, step = 10)
    public int awayDistance = 600;

    @Option("generic_gate.repair")
    public boolean repairLogic = false;

    @Option("generic_gate.repair_config")
    public boolean useRepairConfigWhenNeedRepair = false;

    @Option("generic_gate.attack_closest")
    public boolean alwaysTheClosestNPC = false;

    @Option("generic_gate.travel_to_next")
    public boolean travelToNextMap = false;

    @Option("generic_gate.enable_collector")
    public boolean collectorActive = false;

    @Option("generic_gate.roaming")
    public boolean roaming = false;

    @Option("generic_gate.attack_all_npcs")
    public boolean attackAllNpcs = false;
}
