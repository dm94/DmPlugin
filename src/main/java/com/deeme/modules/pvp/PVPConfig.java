package com.deeme.modules.pvp;

import com.deeme.types.config.AutoCloak;
import com.deeme.types.config.ExtraKeyConditions;

import com.github.manolo8.darkbot.config.Config.Loot.Sab;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;

@Configuration("PVP Config")
public class PVPConfig {
    @Option("pvp_module.movement")
    public boolean move = true;

    @Option("pvp_module.recharge_shields")
    public boolean rechargeShields = true;

    @Option("pvp_module.enable_collector")
    public boolean collectorActive = false;

    @Option("pvp_module.change_config")
    public boolean changeConfig = true;

    @Option("pvp_module.run_config")
    public boolean useRunConfig = true;

    @Option("auto_attack.max_range")
    @Number(min = 0, max = 1000, step = 50)
    public int rangeForEnemies = 500;

    @Option("pvp_module.max_range_enemy_attacked")
    @Number(min = 1000, max = 4000, step = 100)
    public int rangeForAttackedEnemy = 2000;

    @Option("general.rsb")
    public boolean useRSB = false;

    @Option("config.loot.sab")
    public Sab SAB = new Sab();

    @Option("general.ability")
    public ExtraKeyConditions ability = new ExtraKeyConditions();

    @Option("general.ish")
    public ExtraKeyConditions ISH = new ExtraKeyConditions();

    @Option("general.smb")
    public ExtraKeyConditions SMB = new ExtraKeyConditions();

    @Option("general.pem")
    public ExtraKeyConditions PEM = new ExtraKeyConditions();

    @Option("general.auto_cloak")
    public AutoCloak autoCloak = new AutoCloak();

    @Option("anti_push")
    public AntiPush antiPush = new AntiPush();
}
