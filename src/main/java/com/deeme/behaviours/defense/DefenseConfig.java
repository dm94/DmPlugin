package com.deeme.behaviours.defense;

import java.util.EnumSet;
import java.util.Set;

import com.deeme.modules.pvp.AntiPush;
import com.deeme.modules.sentinel.Humanizer;
import com.deeme.types.config.ExtraKeyConditions;
import com.deeme.types.config.ExtraKeyConditionsSelectable;
import com.deemeplus.general.configchanger.ExtraConfigChangerConfig;
import com.deeme.shared.movement.MovementConfig;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

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

    @Option("defense.defend_even_are_not_enemies")
    public boolean defendEvenAreNotEnemies = false;

    @Option("defense.max_time_out")
    @Number(min = 0, max = 180, step = 1)
    public int maxSecondsTimeOut = 10;

    @Option("pvp_module.max_range_enemy_attacked")
    @Number(min = 1000, max = 4000, step = 100)
    public int rangeForAttackedEnemy = 1500;

    @Option("defense.ignore_enemies")
    public boolean ignoreEnemies = true;

    @Option("extra_movement_conditions")
    public MovementConfig movementConfig = new MovementConfig();

    @Option("ammo_config")
    public AmmoConfig ammoConfig = new AmmoConfig();

    @Option("extra_config_changer")
    public ExtraConfigChangerConfig extraConfigChangerConfig = new ExtraConfigChangerConfig();

    @Option("general.ability")
    public ExtraKeyConditions ability = new ExtraKeyConditions();

    @Option("general.ish")
    public ExtraKeyConditions ISH = new ExtraKeyConditions();

    @Option("general.smb")
    public ExtraKeyConditions SMB = new ExtraKeyConditions();

    @Option("general.pem")
    public ExtraKeyConditions PEM = new ExtraKeyConditions();

    @Option("general.item_condition")
    public ExtraKeyConditionsSelectable selectable1 = new ExtraKeyConditionsSelectable();

    @Option("general.item_condition")
    public ExtraKeyConditionsSelectable selectable2 = new ExtraKeyConditionsSelectable();

    @Option("general.item_condition")
    public ExtraKeyConditionsSelectable selectable3 = new ExtraKeyConditionsSelectable();

    @Option("general.item_condition")
    public ExtraKeyConditionsSelectable selectable4 = new ExtraKeyConditionsSelectable();

    @Option("general.item_condition")
    public ExtraKeyConditionsSelectable selectable5 = new ExtraKeyConditionsSelectable();

    @Option("anti_push")
    public AntiPush antiPush = new AntiPush();

    @Option("humanizer")
    public Humanizer humanizer = new Humanizer();
}
