package com.deeme.modules.astral;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;

@Configuration("astral")
public class AstralConfig {
    @Option("astral.min_radius")
    @Number(min = 500, max = 2000, step = 10)
    public int radioMin = 560;

    @Option("general.default_ammo")
    public Character ammoKey;

    @Option("astral.attack_closest")
    public boolean alwaysTheClosestNPC = false;

    @Option("astral.best_ammo")
    @Dropdown
    public BestAmmoConfig useBestAmmoLogic = BestAmmoConfig.ONLY_MARKED;

    @Option("astral.choose_portal")
    public boolean autoChoosePortal = false;

    @Option("astral.choose_item")
    public boolean autoChooseItem = false;

    @Option("astral.cpu_key")
    public Character astralCPUKey;
}