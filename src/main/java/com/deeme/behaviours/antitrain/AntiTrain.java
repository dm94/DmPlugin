package com.deeme.behaviours.antitrain;

import java.util.Collection;
import java.util.Arrays;

import com.deeme.others.CustomSafety;
import com.deeme.types.ConditionsManagement;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;

@Feature(name = "Anti Train", description = "Some options for combating trains")
public class AntiTrain implements Behavior, Configurable<AntiTrainConfig> {
    private final HeroAPI hero;
    private final Collection<? extends Player> players;
    private AntiTrainConfig config;
    private CustomSafety customSafety;
    private ConditionsManagement conditionsManagement;

    public AntiTrain(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class), api.requireAPI(HeroAPI.class), api.requireAPI(HeroItemsAPI.class),
                api.requireAPI(EntitiesAPI.class));
    }

    @Inject
    public AntiTrain(PluginAPI api, AuthAPI auth, HeroAPI hero, HeroItemsAPI heroItems, EntitiesAPI entities) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        VerifierChecker.requireAuthenticity(auth);
        ExtensionsAPI extensionsAPi = api.requireAPI(ExtensionsAPI.class);
        FeatureInfo featureInfo = extensionsAPi.getFeatureInfo(this.getClass());

        Utils.discordCheck(featureInfo, auth.getAuthId());
        Utils.showDonateDialog(featureInfo, auth.getAuthId());

        this.hero = hero;
        this.players = entities.getPlayers();
        this.customSafety = new CustomSafety(api);
        this.conditionsManagement = new ConditionsManagement(api, heroItems);
    }

    @Override
    public void setConfig(ConfigSetting<AntiTrainConfig> arg0) {
        this.config = arg0.getValue();
    }

    @Override
    public void onTickBehavior() {
        if (this.config.maxEnemies <= getNumberOfEnemies()) {
            if (this.config.run) {
                this.customSafety.escapeTick();
            }

            this.conditionsManagement.useKeyWithConditions(this.config.selectable1);
            this.conditionsManagement.useKeyWithConditions(this.config.selectable2);
            this.conditionsManagement.useKeyWithConditions(this.config.selectable3);
            this.conditionsManagement.useKeyWithConditions(this.config.selectable4);
            this.conditionsManagement.useKeyWithConditions(this.config.selectable5);
        }
    }

    private long getNumberOfEnemies() {
        if (this.players == null || this.players.isEmpty()) {
            return 0;
        }

        return this.players.stream()
                .filter(s -> s.getEntityInfo().isEnemy() || s.isBlacklisted())
                .filter(s -> s.distanceTo(this.hero) < this.config.ignoreDistance)
                .count();
    }
}
