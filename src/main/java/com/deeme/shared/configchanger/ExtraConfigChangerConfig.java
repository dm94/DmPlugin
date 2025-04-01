package com.deeme.shared.configchanger;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;

@Configuration("extra_config_changer")
public class ExtraConfigChangerConfig {
    @Option("extra_config_changer.default")
    @Dropdown
    public ConfigOptionsEnum defaultConfig = ConfigOptionsEnum.OFFENSIVE;

    @Option("extra_config_changer.condition")
    public ConfigWithCondition configCondition1 = new ConfigWithCondition();

    @Option("extra_config_changer.condition")
    public ConfigWithCondition configCondition2 = new ConfigWithCondition();

    @Option("extra_config_changer.condition")
    public ConfigWithCondition configCondition3 = new ConfigWithCondition();

    @Option("extra_config_changer.condition")
    public ConfigWithCondition configCondition4 = new ConfigWithCondition();

    @Option("extra_config_changer.condition")
    public ConfigWithCondition configCondition5 = new ConfigWithCondition();
}
