package com.deeme.types.config;

import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.config.types.Option;

@Option("Defense")
public class PVPConfig {
    @Option(value = "Use the run configuration", description = "Will use the run setting if enemies flee")
    public boolean useRunConfig = true;

    @Option(value = "RSB-75", description = "Use RSB-75")
    public boolean useRSB = false;

    @Option(value = "Auto choose the best rocket", description = "Automatically switches missiles")
    public boolean useBestRocket = true;

    @Option(value = "Auto choose the best formation", description = "Automatically switches formations")
    public boolean useBestFormation = true;

    @Option(value = "Ammo", description = "Only used if UCB-100 cannot be used")
    public Character ammoKey;

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
}
