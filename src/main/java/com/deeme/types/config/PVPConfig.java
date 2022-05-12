package com.deeme.types.config;

import com.github.manolo8.darkbot.config.types.Option;

@Option("Defense")
public class PVPConfig {
    @Option(value = "Use the run configuration", description = "Will use the run setting if enemies flee")
    public boolean useRunConfig = true;

    @Option(value = "Auto choose the best rocket", description = "Automatically switches missiles")
    public boolean useBestRocket = true;

    @Option(value = "Auto choose the best formation", description = "Automatically switches formations")
    public boolean useBestFormation = true;

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
