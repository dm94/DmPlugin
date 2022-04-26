package com.deeme.types.config;

import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.config.types.Options;
import com.github.manolo8.darkbot.gui.tree.components.JListField;
import com.github.manolo8.darkbot.gui.tree.components.JPercentField;
import com.github.manolo8.darkbot.gui.tree.components.JShipConfigField;

@Option("Defense")
public class Defense {

    @Option(value = "Respond Attacks", description = "If you want the ship to respond to attacks.")
    public boolean respondAttacks = true;

    @Option(value = "Help Clan Members", description = "Help clan members defend themselves")
    public boolean helpAllies = true;

    @Option(value = "Help Group Members", description = "Help group members defend themselves")
    public boolean helpGroup = true;

    @Option(value = "Go to Group Members", description = "If any member is attacked on your map it goes to the member.")
    public boolean goToGroup = false;

    @Option(value = "Help everyone", description = "Help all those who are not enemies")
    public boolean helpEveryone = true;

    @Option(value = "Help to Attack", description = "Help to attack")
    public boolean helpAttack = false;

    @Option(value = "Movement Mode", description = "Choose how to move in defense mode")
    @Editor(JListField.class)
    @Options(MovementMode.class)
    public int movementMode = 0;

    @Option("Ammo")
    public Character ammoKey;

    @Option(value = "RSB-75", description = "Use RSB-75")
    public boolean useRSB = false;

    public @Option(key = "config.loot.sab") Config.Loot.Sab SAB = new Config.Loot.Sab();

    public @Option(value = "Ability", description = "Ability Conditions")
    ExtraKeyConditions ability = new ExtraKeyConditions();

    public @Option(value = "ISH-01", description = "ISH-01 Conditions")
    ExtraKeyConditions ISH = new ExtraKeyConditions();

    public @Option(value = "SMB-01", description = "SMB-01 Conditions")
    ExtraKeyConditions SMB = new ExtraKeyConditions();

    public @Option(value = "PEM-01", description = "PEM-01 Conditions")
    ExtraKeyConditions PEM = new ExtraKeyConditions();

    public @Option(value = "Other Key", description = "Other Key Conditions")
    ExtraKeyConditions otherKey = new ExtraKeyConditions();

    @Option(value = "Use second config", description = "Use the second config, if there is no shield and you cannot use sab")
    public boolean useSecondConfig = true;

    @Option(value = "Min health to change", description ="If health comes down from")
    @Editor(JPercentField.class)
    public double healthToChange = 0.2;

    @Option(value = "Second config", description = "Used when attack config is exhausted")
    @Editor(JShipConfigField.class)
    public Config.ShipConfig secondConfig = new Config.ShipConfig();

}
