package com.deeme.modules.pvp;

import com.deeme.types.config.AutoCloak;
import com.deeme.types.config.ExtraKeyConditions;
import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;

@Option("PVP Config")
public class PVPConfig {
    @Option(value = "Movement", description = "The ship will move")
    public boolean move = true;

    @Option(value = "Recharge shields", description = "Recharge the shield for both Attack and Run configurations")
    public boolean rechargeShields = true;

    @Option(value = "Enable collector", description = "It will use the collector module when it is not doing anything.")
    public boolean collectorActive = false;

    @Option(value = "Auto change config", description = "It will change configuration automatically")
    public boolean changeConfig = true;

    @Option(value = "Use the run configuration", description = "Will use the run setting if enemies flee")
    public boolean useRunConfig = true;

    @Option(value = "Maximum range for enemies", description = "Enemies above this range will not be attacked")
    @Num(min = 0, max = 1000, step = 50)
    public int rangeForEnemies = 500;

    @Option(value = "Maximum range for the enemy attacked", description = "Above this range the enemy is considered to have escaped.")
    @Num(min = 1000, max = 4000, step = 100)
    public int rangeForAttackedEnemy = 2000;

    @Option(value = "RCB-140 | RSB-75", description = "Use RCB-140 | RSB-75")
    public boolean useRSB = false;

    public @Option(key = "config.loot.sab") Config.Loot.Sab SAB = new Config.Loot.Sab();

    public @Option(value = "Ability", description = "Ability Conditions") ExtraKeyConditions ability = new ExtraKeyConditions();

    public @Option(value = "ISH-01", description = "ISH-01 Conditions") ExtraKeyConditions ISH = new ExtraKeyConditions();

    public @Option(value = "SMB-01", description = "SMB-01 Conditions") ExtraKeyConditions SMB = new ExtraKeyConditions();

    public @Option(value = "PEM-01", description = "PEM-01 Conditions") ExtraKeyConditions PEM = new ExtraKeyConditions();

    public @Option(value = "Auto Cloak", description = "It will automatically camouflage") AutoCloak autoCloak = new AutoCloak();

    public @Option(value = "Anti Push") AntiPush antiPush = new AntiPush();
}
