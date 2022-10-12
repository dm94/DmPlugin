package com.deeme.types.config;

import com.github.manolo8.darkbot.config.Config.PercentRange;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.types.Condition;

@Configuration("extra_condition")
public class ExtraKeyConditions {

    @Option(value = "general.enabled")
    public boolean enable = false;

    @Option(value = "general.key")
    public Character Key;

    public PercentRange HEALTH_RANGE = new PercentRange(0.5, 0.95);

    public PercentRange HEALTH_ENEMY_RANGE = new PercentRange(0.2, 0.4);

    @Option(value = "general.condition")
    public Condition CONDITION;
}
