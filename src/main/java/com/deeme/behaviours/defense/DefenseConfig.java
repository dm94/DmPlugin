package com.deeme.behaviours.defense;

import com.deeme.types.config.ExtraKeyConditions;
import com.github.manolo8.darkbot.config.Config.Loot.Sab;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Percentage;
import eu.darkbot.api.config.types.ShipMode;
import eu.darkbot.api.game.items.SelectableItem.Formation;
import eu.darkbot.api.managers.HeroAPI;

@Configuration("defense")
public class DefenseConfig {

    @Option("defense.respond_attacks")
    public boolean respondAttacks = true;

    @Option("defense.help_allies")
    public boolean helpAllies = true;

    @Option("defense.help_group")
    public boolean helpGroup = true;

    @Option("defense.go_to_group")
    public boolean goToGroup = true;

    @Option("defense.help_everyone")
    public boolean helpEveryone = true;

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

    @Option("defense.use_alternative_config")
    public boolean useAlternativeConfig = true;

    @Option("defense.alternative_config_min_health")
    @Percentage
    public double healthToChange = 0.2;

    @Option("defense.alternative_config")
    public ShipMode alternativeConfig = ShipMode.of(HeroAPI.Configuration.FIRST, Formation.STANDARD);

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
