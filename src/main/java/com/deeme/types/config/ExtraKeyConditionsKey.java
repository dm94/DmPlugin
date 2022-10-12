package com.deeme.types.config;

import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.types.Condition;

public class ExtraKeyConditionsKey {

    @Option(value = "general.enabled")
    public boolean enable = false;

    @Option(value = "general.key")
    public Character Key;

    @Option(value = "general.name")
    public String name = "";

    @Option(value = "general.condition")
    public Condition condition;
}
