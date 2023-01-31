package com.deeme.behaviours.profilechanger;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.types.Condition;

@Configuration("general.condition")
public class NormalCondition {
    @Option("general.enabled")
    public boolean active = false;

    @Option("general.condition")
    public Condition condition;
}