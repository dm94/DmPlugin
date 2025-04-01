package com.deeme.shared.configchanger;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.Condition;
import eu.darkbot.api.config.types.ShipMode;
import eu.darkbot.api.managers.ConfigAPI;

public class ExtraCChangerLogic {
    private final PluginAPI api;

    private ExtraConfigChangerConfig config;

    private final ConfigSetting<ShipMode> configOffensive;
    private final ConfigSetting<ShipMode> configRun;
    private final ConfigSetting<ShipMode> configRoam;

    public ExtraCChangerLogic(PluginAPI api, ExtraConfigChangerConfig config) {
        this.api = api;
        this.config = config;

        ConfigAPI configApi = api.requireAPI(ConfigAPI.class);
        this.configOffensive = configApi.requireConfig("general.offensive");
        this.configRun = configApi.requireConfig("general.run");
        this.configRoam = configApi.requireConfig("general.roam");
    }

    public ShipMode getShipMode() {
        ConfigOptionsEnum configToUse = getConfigToUse();

        switch (configToUse) {
            case RUN:
                return configRun.getValue();
            case ROAM:
                return configRoam.getValue();
            case OFFENSIVE:
            default:
                return configOffensive.getValue();
        }

    }

    private ConfigOptionsEnum getConfigToUse() {
        if (checkCondition(config.configCondition1.condition)) {
            return config.configCondition1.config;
        }

        if (checkCondition(config.configCondition2.condition)) {
            return config.configCondition2.config;
        }

        if (checkCondition(config.configCondition3.condition)) {
            return config.configCondition3.config;
        }

        if (checkCondition(config.configCondition4.condition)) {
            return config.configCondition4.config;
        }

        if (checkCondition(config.configCondition5.condition)) {
            return config.configCondition5.config;
        }

        return config.defaultConfig;
    }

    private boolean checkCondition(Condition condition) {
        if (condition == null) {
            return false;
        }

        return condition.get(api).allows();
    }
}
