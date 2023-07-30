package com.deeme.modules.astral;

import com.deeme.behaviours.bestrocket.RocketSupplier;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;

@Configuration("astral")
public class AstralConfig {
    @Option("astral.min_radius")
    @Number(min = 300, max = 800, step = 10)
    public int radioMin = 560;

    @Option("astral.min_cpus")
    @Number(min = 0, step = 1)
    public int minCPUs = 0;

    @Option("general.default_ammo")
    public Character ammoKey;

    @Option("general.default_rocket")
    @Dropdown(options = RocketSupplier.class)
    public String defaultRocket = "";

    @Option("astral.attack_closest")
    public boolean alwaysTheClosestNPC = false;

    @Option("astral.best_ammo")
    @Dropdown
    public BestAmmoConfig useBestAmmoLogic = BestAmmoConfig.ONLY_MARKED;

    @Option("astral.choose_portal")
    public boolean autoChoosePortal = false;

    @Option("astral.choose_item")
    public boolean autoChooseItem = false;

    @Option("astral.display_warning")
    public boolean displayWarning = false;
}