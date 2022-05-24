package com.deeme.types.config;

import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.config.types.Option;

@Option("Extra Conditions")
public class ExtraKeyConditions {

    @Option(value = "Enable")
    public boolean enable = false;

    @Option("Key")
    public Character Key;

    @Option(value = "Health", description ="If health between")
    public Config.PercentRange HEALTH_RANGE = new Config.PercentRange(0.5, 0.95);

    @Option(value = "Enemy Health", description ="If the enemy's health is between")
    public Config.PercentRange HEALTH_ENEMY_RANGE = new Config.PercentRange(0.2, 0.4);
}
