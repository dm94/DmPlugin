package com.deeme.behaviours.defense;

import java.util.EnumSet;
import java.util.Set;

import com.deeme.types.config.ExtraKeyConditions;
import com.github.manolo8.darkbot.config.Config.Loot.Sab;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Percentage;

@Configuration("defense")
public class DefenseConfig {

    @Option("defense.respond_attacks")
    public boolean respondAttacks = true;

    @Option("defense.help_list")
    @Dropdown(multi = true)
    public Set<HelpList> helpList = EnumSet.of(HelpList.ALLY, HelpList.CLAN, HelpList.GROUP);

    @Option("defense.go_to_group")
    public boolean goToGroup = true;

    @Option("defense.help_attack")
    public boolean helpAttack = true;

    @Option("defense.movement_mode")
    @Dropdown
    public MovementMode movementMode = MovementMode.VSSAFETY;

    @Option("defense.ignore_enemies")
    public boolean ignoreEnemies = true;

    @Option("general.default_ammo")
    public Character ammoKey;

    @Option("general.rsb")
    public boolean useRSB = false;

    @Option("defense.run_config_min_health")
    @Percentage
    public double healthToChange = 0.0;

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
}
