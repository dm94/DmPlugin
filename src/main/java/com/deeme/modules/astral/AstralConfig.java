package com.deeme.modules.astral;

import java.util.HashMap;
import java.util.Map;

import com.deeme.behaviours.bestrocket.RocketSupplier;
import com.deemeplus.modules.astral.CustomItemPriority;
import com.deemeplus.modules.astral.PortalInfo;
import com.deemeplus.suppliers.LaserSupplier;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Table;

@Configuration("astral")
public class AstralConfig {
    @Option("astral.min_radius")
    @Number(min = 300, max = 800, step = 10)
    public int radioMin = 560;

    @Option("astral.min_cpus")
    @Number(min = 0, step = 1)
    public int minCPUs = 0;

    @Option("general.default_ammo")
    @Dropdown(options = LaserSupplier.class)
    public String defaultLaser = "ammunition_laser_lcb-10";

    @Option("general.default_rocket")
    @Dropdown(options = RocketSupplier.class)
    public String defaultRocket = "ammunition_rocket_plt-2026";

    @Option("astral.attack_closest")
    public boolean alwaysTheClosestNPC = false;

    @Option("astral.best_ammo")
    @Dropdown
    public BestAmmoConfig useBestAmmoLogic = BestAmmoConfig.ONLY_MARKED;

    @Option("astral.ship_to_choose")
    @Dropdown
    public AvailableShips shipType = AvailableShips.ZEPHYR;

    @Option("astral.auto_choose")
    public boolean autoChoose = false;

    public @Option @Table Map<String, PortalInfo> portalInfos = new HashMap<>();

    @Option("astral.custom_item_priority")
    public CustomItemPriority customItemPriority = new CustomItemPriority();

    @Option("general.min_time_per_action")
    @Number(min = 0, step = 1, max = 60)
    public int minTimePerAction = 5;
}