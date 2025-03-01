package com.deeme.modules.pvp;

import com.deeme.behaviours.defense.AmmoConfig;
import com.deeme.modules.sentinel.Humanizer;
import com.deeme.types.config.ExtraKeyConditions;
import com.deemeplus.general.configchanger.ExtraConfigChangerConfig;
import com.deeme.shared.movement.MovementConfig;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;

@Configuration("PVP Config")
public class PVPConfig {
    @Option("pvp_module.movement")
    public boolean move = true;

    @Option("pvp_module.enable_collector")
    public boolean collectorActive = false;

    @Option("pvp_module.change_config")
    public boolean changeConfig = true;

    @Option("pvp_module.ignore_invisible")
    public boolean ignoreInvisible = false;

    @Option("auto_attack.max_range")
    @Number(min = 0, max = 1000, step = 50)
    public int rangeForEnemies = 500;

    @Option("defense.max_time_out")
    @Number(min = 0, max = 180, step = 1)
    public int maxSecondsTimeOut = 10;

    @Option("pvp_module.max_range_enemy_attacked")
    @Number(min = 1000, max = 4000, step = 100)
    public int rangeForAttackedEnemy = 2000;

    @Option("extra_movement_conditions")
    public MovementConfig movementConfig = new MovementConfig();

    @Option("extra_config_changer")
    public ExtraConfigChangerConfig extraConfigChangerConfig = new ExtraConfigChangerConfig();

    @Option("ammo_config")
    public AmmoConfig ammoConfig = new AmmoConfig();

    @Option("general.ability")
    public ExtraKeyConditions ability = new ExtraKeyConditions();

    @Option("general.ish")
    public ExtraKeyConditions ISH = new ExtraKeyConditions();

    @Option("general.smb")
    public ExtraKeyConditions SMB = new ExtraKeyConditions();

    @Option("general.pem")
    public ExtraKeyConditions PEM = new ExtraKeyConditions();

    @Option("anti_push")
    public AntiPush antiPush = new AntiPush();

    @Option("humanizer")
    public Humanizer humanizer = new Humanizer();
}
