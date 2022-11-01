package com.deeme.behaviours;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import com.deeme.behaviours.profilechanger.ProfileChangerConfig;
import com.deeme.behaviours.profilechanger.ResourceSupplier;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.github.manolo8.darkbot.Main;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.BoxInfo;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Box;
import eu.darkbot.api.game.enums.EntityEffect;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "ProfileChanger", description = "Change the profile to another one when completing a task")
public class ProfileChanger implements Behavior, Configurable<ProfileChangerConfig> {

    protected final PluginAPI api;
    protected final BotAPI bot;
    protected final HeroAPI hero;
    private ProfileChangerConfig config;
    private Main main;

    private long nextCheck = 0;

    private boolean resourceListUpdated = false;
    private final ConfigSetting<Map<String, BoxInfo>> boxInfos;
    protected Collection<? extends Box> boxes;

    public ProfileChanger(Main main, PluginAPI api) {
        this(main, api, api.requireAPI(AuthAPI.class),
                api.requireAPI(BotAPI.class),
                api.requireAPI(HeroAPI.class));
    }

    @Inject
    public ProfileChanger(Main main, PluginAPI api, AuthAPI auth, BotAPI bot, HeroAPI hero) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        if (!Utils.discordCheck(auth.getAuthId())) {
            Utils.showDiscordDialog();
            ExtensionsAPI extensionsAPI = api.getAPI(ExtensionsAPI.class);
            extensionsAPI.getFeatureInfo(this.getClass())
                    .addFailure("To use this option you need to be on my discord", "Log in to my discord and reload");
        }

        this.main = main;
        this.api = api;
        this.bot = bot;
        this.hero = hero;

        ConfigAPI configApi = api.getAPI(ConfigAPI.class);
        this.boxInfos = configApi.requireConfig("collect.box_infos");

        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        this.boxes = entities.getBoxes();

        this.resourceListUpdated = false;
        this.nextCheck = 0;
    }

    @Override
    public void setConfig(ConfigSetting<ProfileChangerConfig> arg0) {
        this.config = arg0.getValue();
    }

    @Override
    public void onTickBehavior() {
        updateResourceList();
        if (config.active) {
            checkNPC();
            checkResource();
            checkMap();

            if (nextCheck < System.currentTimeMillis()) {
                nextCheck = System.currentTimeMillis() + (config.timeToCheck * 1000);
                if ((config.condition == null || config.condition.get(api).allows())
                        && isReadyNpcCondition() && isReadyResourceCondition() && isReadyMapCondition()) {
                    resetCounters();
                    main.setConfig(config.BOT_PROFILE);
                }
            }
        }
    }

    @Override
    public void onStoppedBehavior() {
        updateResourceList();
    }

    private boolean isReadyNpcCondition() {
        return (!config.npcExtraCondition.active ||
                config.npcExtraCondition.npcCounter >= config.npcExtraCondition.npcsToKill)
                && (!config.npcExtraCondition2.active ||
                        config.npcExtraCondition2.npcCounter >= config.npcExtraCondition2.npcsToKill);
    }

    private void checkNPC() {
        Lockable target = hero.getLocalTarget();
        if (target != null && target.isValid() && target.isOwned()) {
            String name = target.getEntityInfo().getUsername();
            if (name != null && name.toLowerCase().contains(config.npcExtraCondition.npcName.toLowerCase())
                    && target.getId() != config.npcExtraCondition.lastNPCId) {
                config.npcExtraCondition.lastNPCId = target.getId();
                config.npcExtraCondition.npcCounter++;
            }
            if (name != null && name.toLowerCase().contains(config.npcExtraCondition2.npcName.toLowerCase())
                    && target.getId() != config.npcExtraCondition2.lastNPCId) {
                config.npcExtraCondition2.lastNPCId = target.getId();
                config.npcExtraCondition2.npcCounter++;
            }
        }
    }

    private boolean isReadyResourceCondition() {
        return !config.resourceCounterCondition.active
                || config.resourceCounterCondition.resourcesFarmed >= config.resourceCounterCondition.resourcesToFarm;
    }

    private void checkResource() {
        if (config.resourceCounterCondition.active
                && (hero.hasEffect(EntityEffect.BOOTY_COLLECTING) || hero.hasEffect(EntityEffect.BOX_COLLECTING))) {
            Box box = boxes.stream().min(Comparator.<Box>comparingDouble(b -> b.distanceTo(hero))).orElse(null);
            if (box != null && box.distanceTo(hero) < 100
                    && config.resourceCounterCondition.lastResourceId != box.getId()
                    && config.resourceCounterCondition.lastResourcePosition != box.getX()) {
                config.resourceCounterCondition.lastResourcePosition = box.getX();
                config.resourceCounterCondition.lastResourceId = box.getId();
                if (box.getTypeName().equals(config.resourceCounterCondition.resourceName)) {
                    config.resourceCounterCondition.resourcesFarmed++;
                }
            }
        }
    }

    private boolean isReadyMapCondition() {
        return !config.mapTimerCondition.active || (config.mapTimerCondition.mapTimeStart != 0
                && (config.mapTimerCondition.mapTimeStart + (config.mapTimerCondition.timeInMap * 60000)) <= System
                        .currentTimeMillis());
    }

    private void checkMap() {
        if (config.mapTimerCondition.active) {
            if (hero.getMap() != null && hero.getMap().getId() == config.mapTimerCondition.map) {
                if (config.mapTimerCondition.mapTimeStart <= 0) {
                    config.mapTimerCondition.mapTimeStart = System.currentTimeMillis();
                }
            } else {
                config.mapTimerCondition.mapTimeStart = 0;
            }
        }
    }

    private void resetCounters() {
        config.npcExtraCondition.npcCounter = 0;
        config.npcExtraCondition2.npcCounter = 0;
        config.resourceCounterCondition.resourcesFarmed = 0;
        config.mapTimerCondition.mapTimeStart = 0;
    }

    private void updateResourceList() {
        if (!resourceListUpdated) {
            Map<String, BoxInfo> allBoxes = boxInfos.getValue();

            ArrayList<String> boxes = new ArrayList<String>(allBoxes.keySet());

            ResourceSupplier.updateBoxes(boxes);
            resourceListUpdated = true;
        }
    }
}
