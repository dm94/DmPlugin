package com.deeme.behaviours;

import java.util.Arrays;

import com.deeme.types.ConditionsManagement;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deeme.types.config.CustomEventsConfig;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "CustomEvents", description = "Another custom events")
public class CustomEvents implements Behavior, Configurable<CustomEventsConfig> {
    private CustomEventsConfig config;
    protected long clickDelay;
    private ConditionsManagement conditionsManagement;

    public CustomEvents(PluginAPI api) {
        this(api,
                api.requireAPI(AuthAPI.class),
                api.requireAPI(HeroItemsAPI.class));
    }

    @Inject
    public CustomEvents(PluginAPI api, AuthAPI auth, HeroItemsAPI heroItems) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog(auth.getAuthId());

        this.conditionsManagement = new ConditionsManagement(api, heroItems);
    }

    @Override
    public void setConfig(ConfigSetting<CustomEventsConfig> arg0) {
        this.config = arg0.getValue();
    }

    @Override
    public void onStoppedBehavior() {
        if (config.tickStopped) {
            onTickBehavior();
        }
    }

    @Override
    public void onTickBehavior() {
        conditionsManagement.useKeyWithConditions(config.otherKey);
        conditionsManagement.useKeyWithConditions(config.otherKey2);
        conditionsManagement.useKeyWithConditions(config.otherKey3);
        conditionsManagement.useKeyWithConditions(config.otherKey4);
        conditionsManagement.useKeyWithConditions(config.otherKey5);
        conditionsManagement.useKeyWithConditions(config.selectable1);
        conditionsManagement.useKeyWithConditions(config.selectable2);
        conditionsManagement.useKeyWithConditions(config.selectable3);
        conditionsManagement.useKeyWithConditions(config.selectable4);
        conditionsManagement.useKeyWithConditions(config.selectable5);
        conditionsManagement.useKeyWithConditions(config.selectable6);
        conditionsManagement.useKeyWithConditions(config.selectable7);
        conditionsManagement.useKeyWithConditions(config.selectable8);
        conditionsManagement.useKeyWithConditions(config.selectable9);
        conditionsManagement.useKeyWithConditions(config.selectable10);
    }
}
