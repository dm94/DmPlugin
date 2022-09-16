package com.deeme.types.config;

import com.github.manolo8.darkbot.config.Config.PercentRange;
import com.github.manolo8.darkbot.config.types.Option;

import eu.darkbot.api.config.types.Condition;

@Option("Extra Conditions")
public class ExtraKeyConditions {

    @Option(value = "Enable")
    public boolean enable = false;

    @Option(value = "Key", description = "Often this is not necessary")
    public Character Key;

    @Option(value = "Health", description = "If health between")
    public PercentRange HEALTH_RANGE = new PercentRange(0.5, 0.95);

    @Option(value = "Enemy Health", description = "If the enemy's health is between")
    public PercentRange HEALTH_ENEMY_RANGE = new PercentRange(0.2, 0.4);

    @Option("Condition")
    public Condition CONDITION;
}
