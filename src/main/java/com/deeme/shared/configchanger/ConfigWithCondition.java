package com.deeme.shared.configchanger;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.types.Condition;

@Configuration("config_conditions")
public class ConfigWithCondition {
    @Option("config_conditions.config_mode")
    @Dropdown
    public ConfigOptionsEnum config = ConfigOptionsEnum.OFFENSIVE;

    @Option("general.condition")
    public Condition condition;
}
