package com.deeme.types.config;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.types.Condition;

@Configuration("extra_condition")
public class ExtraKeyConditionsKey {

    @Option("general.enabled")
    public boolean enable = false;

    @Option("general.key")
    public Character Key;

    @Option("general.name")
    public String name = "";

    @Option("general.condition")
    public Condition condition;
}
