package com.deeme.behaviours.defense;

import eu.darkbot.api.config.annotations.Option;

import com.deemeplus.gui.suppliers.LaserSupplier;
import com.github.manolo8.darkbot.config.Config.Loot.Sab;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;

@Configuration("ammo_config")
public class AmmoConfig {
    @Option("ammo_config.enable")
    public boolean enableAmmoConfig = false;

    @Option("general.default_ammo")
    @Dropdown(options = LaserSupplier.class)
    public String defaultLaser = "ammunition_laser_ucb-100";

    @Option("general.rsb")
    public boolean useRSB = false;

    @Option("config.loot.sab")
    public Sab sab = new Sab();
}
