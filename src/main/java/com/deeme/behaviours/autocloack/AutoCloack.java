package com.deeme.behaviours.autocloack;

import java.util.Arrays;
import java.util.Optional;

import com.deeme.types.ConditionsManagement;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.api.game.items.SelectableItem.Cpu;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "Auto Cloak", description = "Auto Cloak additional config")
public class AutoCloack implements Behavior, Configurable<AutoCloackConfig> {
    private static final long CLOAK_ATTEMPT_INTERVAL_MS = 750L;
    private final HeroAPI heroApi;

    private ConditionsManagement conditionsManagement;

    private long lastTimeAttack = System.currentTimeMillis();
    private long nextCloakAttempt = 0;
    private AutoCloackConfig config;

    public AutoCloack(PluginAPI api) {
        this(api, api.requireAPI(HeroAPI.class),
                api.requireAPI(AuthAPI.class), api.requireAPI(HeroItemsAPI.class));
    }

    @Inject
    public AutoCloack(PluginAPI api, HeroAPI hero, AuthAPI auth, HeroItemsAPI items) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.requireAuthenticity(auth);

        ExtensionsAPI extensionsAPi = api.requireAPI(ExtensionsAPI.class);
        FeatureInfo<?> featureInfo = extensionsAPi.getFeatureInfo(this.getClass());

        Utils.discordCheck(featureInfo, auth.getAuthId());
        Utils.showDonateDialog(featureInfo, auth.getAuthId());

        this.heroApi = hero;
        this.conditionsManagement = new ConditionsManagement(api, items);
    }

    @Override
    public void setConfig(ConfigSetting<AutoCloackConfig> arg0) {
        this.config = arg0.getValue();
        this.lastTimeAttack = System.currentTimeMillis();
    }

    @Override
    public void onTickBehavior() {
        if (config == null) {
            return;
        }

        if (heroApi.isAttacking()) {
            lastTimeAttack = System.currentTimeMillis();
            return;
        }

        if (!shouldTryToCloak()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime < nextCloakAttempt) {
            return;
        }

        nextCloakAttempt = currentTime + CLOAK_ATTEMPT_INTERVAL_MS;
        conditionsManagement.useKeyWithConditions(config.condition, Cpu.CL04K);
    }

    private boolean shouldTryToCloak() {
        return config.autoCloakShip && !heroApi.isInvisible() && isWaitingTimeSatisfied() && isMapAllowed();
    }

    private boolean isWaitingTimeSatisfied() {
        return lastTimeAttack < (System.currentTimeMillis() - (config.secondsOfWaiting * 1000L));
    }

    private boolean isMapAllowed() {
        if (!config.onlyPvpMaps) {
            return true;
        }

        return !Optional.ofNullable(heroApi.getMap()).map(map -> map.isPvp()).orElse(false);
    }

}
