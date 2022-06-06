package com.deeme.types.config;

import com.github.manolo8.darkbot.config.types.Option;

import eu.darkbot.api.config.types.Condition;

public class ExtraKeyConditionsWithoutHealth {

    @Option("Enable")
    public boolean enable = false;

    @Option("Key")
    public Character Key;

    @Option("Condition")
    public Condition CONDITION;
}
