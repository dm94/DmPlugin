package com.deeme.behaviours.defense;

import eu.darkbot.api.config.annotations.Option;

import com.github.manolo8.darkbot.config.Config.Loot.Sab;

import eu.darkbot.api.config.annotations.Configuration;

@Configuration("ammo_config")
public class AmmoConfig {
    @Option("ammo_config.enable")
    public boolean enableAmmoConfig = false;

    @Option("general.default_ammo")
    public Character ammoKey;

    @Option("general.rsb")
    public boolean useRSB = false;

    @Option("config.loot.sab")
    public Sab sab = new Sab();
}
