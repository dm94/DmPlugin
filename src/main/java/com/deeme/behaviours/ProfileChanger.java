package com.deeme.behaviours;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import com.deeme.behaviours.profilechanger.NormalCondition;
import com.deeme.behaviours.profilechanger.NpcCounterCondition;
import com.deeme.behaviours.profilechanger.ProfileChangerConfig;
import com.deeme.behaviours.profilechanger.ResourceSupplier;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.modules.DisconnectModule;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.BoxInfo;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Box;
import eu.darkbot.api.game.enums.EntityEffect;
import eu.darkbot.api.game.other.Gui;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.GameScreenAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.RepairAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "ProfileChanger", description = "Change the profile to another one when completing a task")
public class ProfileChanger implements Behavior, Configurable<ProfileChangerConfig> {

    protected final PluginAPI api;
    protected final BotAPI bot;
    protected final HeroAPI hero;
    protected final RepairAPI repair;
    protected final StatsAPI stats;

    private ProfileChangerConfig config;
    private Main main;

    private long nextCheck = 0;

    private boolean resourceListUpdated = false;
    private final ConfigSetting<Map<String, BoxInfo>> boxInfos;
    private final Gui lostConnectionGUI;
    protected Collection<? extends Box> boxes;

    public ProfileChanger(Main main, PluginAPI api) {
        this(main, api, api.requireAPI(AuthAPI.class),
                api.requireAPI(BotAPI.class),
                api.requireAPI(HeroAPI.class),
                api.requireAPI(StatsAPI.class));
    }

    @Inject
    public ProfileChanger(Main main, PluginAPI api, AuthAPI auth, BotAPI bot, HeroAPI hero, StatsAPI stats) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog();

        this.main = main;
        this.api = api;
        this.bot = bot;
        this.hero = hero;
        this.stats = stats;
        this.repair = api.getAPI(RepairAPI.class);

        ConfigAPI configApi = api.getAPI(ConfigAPI.class);
        this.boxInfos = configApi.requireConfig("collect.box_infos");

        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        this.boxes = entities.getBoxes();

        GameScreenAPI gameScreenAPI = api.getAPI(GameScreenAPI.class);
        lostConnectionGUI = gameScreenAPI.getGui("lost_connection");

        this.resourceListUpdated = false;
        this.nextCheck = 0;

        resetCounters();
    }

    @Override
    public void setConfig(ConfigSetting<ProfileChangerConfig> arg0) {
        this.config = arg0.getValue();
    }

    @Override
    public void onTickBehavior() {
        updateResourceList();
        if (config.active) {
            checkNPC(config.npcExtraCondition);
            checkNPC(config.npcExtraCondition2);
            checkResource();
            checkMap();

            if (nextCheck < System.currentTimeMillis()) {
                nextCheck = System.currentTimeMillis() + (config.timeToCheck * 1000);

                if (config.orConditional) {
                    orConditional();
                } else {
                    andConditional();
                }
            }
        }
    }

    @Override
    public void onStoppedBehavior() {
        updateResourceList();
        config.mapTimerCondition.mapTimeStart = 0;
        if (config.closeBot) {
            stopCheck();
        }
    }

    private void changeAction() {
        if (config.closeBot) {
            if (!hero.getMap().isGG() && bot.getModule().canRefresh()) {
                resetCounters();
                if (!isDisconnect() && !(bot.getModule() instanceof DisconnectModule)) {
                    bot.setModule(new DisconnectModule(null, "Profile Changer"));
                }
            }
        } else {
            resetCounters();
            if (config.reloadBot) {
                Main.API.handleRefresh();
            }
            main.setConfig(config.BOT_PROFILE);
        }
    }

    private void stopCheck() {
        if (isDisconnect()) {
            System.exit(0);
        }
    }

    private boolean isDisconnect() {
        return lostConnectionGUI != null && lostConnectionGUI.isVisible();
    }

    private void orConditional() {
        if (isReadyNormalCondtion(config.normalCondition)
                || isReadyNormalCondtion(config.normalCondition2)
                || isReadyNpcCondition(config.npcExtraCondition)
                || isReadyNpcCondition(config.npcExtraCondition2)
                || isReadyResourceCondition() || isReadyMapCondition()
                || isReadyTimeCondition()
                || isReadyDeathCondition()) {
            changeAction();
        }
    }

    private void andConditional() {
        if (isReadyNormalCondtion(config.normalCondition)
                && isReadyNormalCondtion(config.normalCondition2)
                && isReadyNpcCondition(config.npcExtraCondition)
                && isReadyNpcCondition(config.npcExtraCondition2)
                && isReadyResourceCondition() && isReadyMapCondition()
                && isReadyTimeCondition()
                && isReadyDeathCondition()) {
            changeAction();
        }
    }

    private boolean isReadyNormalCondtion(NormalCondition condition) {
        return condition == null || !condition.active ||
                condition.condition == null || condition.condition.get(api).allows();
    }

    private boolean isReadyNpcCondition(NpcCounterCondition npcCondition) {
        return !npcCondition.active ||
                npcCondition.npcCounter >= npcCondition.npcsToKill;
    }

    private void checkNPC(NpcCounterCondition npcCondition) {
        if (npcCondition.active) {
            Lockable target = hero.getLocalTarget();
            if (target != null && target.isValid() && target.isOwned()) {
                String name = target.getEntityInfo().getUsername();
                if (target.getId() != npcCondition.lastNPCId && npcCondition.isAttacked) {
                    npcCondition.isAttacked = false;
                    if (npcCondition.lastExperiencieCheck < stats.getTotalExperience()) {
                        npcCondition.npcCounter++;
                    }
                }
                if (name != null && name.toLowerCase().contains(npcCondition.npcName.toLowerCase())
                        && target.getId() != npcCondition.lastNPCId) {
                    npcCondition.lastNPCId = target.getId();
                    npcCondition.lastExperiencieCheck = stats.getTotalExperience();
                    npcCondition.isAttacked = true;
                }
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
        return !config.mapTimerCondition.active || (config.mapTimerCondition.mapTimeStart > 0
                && (config.mapTimerCondition.mapTimeStart + (config.mapTimerCondition.timeInMap * 60000)) <= System
                        .currentTimeMillis());
    }

    private boolean isReadyTimeCondition() {
        if (!config.timeCondition.active) {
            return true;
        }

        LocalDateTime da = LocalDateTime.now();

        return da.getHour() > config.timeCondition.hour
                || (config.timeCondition.hour == da.getHour() && da.getMinute() >= config.timeCondition.minute);
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

    private boolean isReadyDeathCondition() {
        return !config.deathsCondition.active
                || config.deathsCondition.maxDeaths >= repair.getDeathAmount();
    }

    private void resetCounters() {
        if (this.main == null || this.config == null) {
            return;
        }
        if (this.config.deathsCondition.active) {
            this.main.repairManager.resetDeaths();
        }

        config.npcExtraCondition.npcCounter = 0;
        config.npcExtraCondition2.npcCounter = 0;
        config.resourceCounterCondition.resourcesFarmed = 0;
        config.mapTimerCondition.mapTimeStart = 0;
    }

    private void updateResourceList() {
        if (!resourceListUpdated) {
            Map<String, BoxInfo> allBoxes = boxInfos.getValue();
            ArrayList<String> arrayBoxes = new ArrayList<>(allBoxes.keySet());
            ResourceSupplier.updateBoxes(arrayBoxes);
            resourceListUpdated = true;
        }
    }
}
