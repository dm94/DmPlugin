package com.deeme.behaviours.bestformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.deeme.types.SharedFunctions;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.config.NpcExtraFlag;
import com.github.manolo8.darkbot.core.itf.NpcExtraProvider;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Formation;
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.utils.SafetyFinder;
import eu.darkbot.shared.utils.SafetyFinder.Escaping;

@Feature(name = "Auto Best Formation", description = "Automatically switches formations")
public class AutoBestFormation implements Behavior, Configurable<BestFormationConfig>, NpcExtraProvider {

    protected final PluginAPI api;
    protected final HeroAPI heroapi;
    protected final HeroItemsAPI items;
    protected final SafetyFinder safety;
    private BestFormationConfig config;
    private Collection<? extends Npc> allNpcs;
    private Collection<? extends Portal> allPortals;
    private long nextCheck = 0;

    protected final ConfigSetting<Config.ShipConfig> configOffensive;

    private ArrayList<Formation> availableFormations = new ArrayList<>();

    public AutoBestFormation(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class), api.requireAPI(HeroItemsAPI.class));
    }

    @Inject
    public AutoBestFormation(PluginAPI api, AuthAPI auth, HeroItemsAPI items) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog();

        this.api = api;
        this.items = items;
        this.heroapi = api.getAPI(HeroAPI.class);
        this.safety = api.requireInstance(SafetyFinder.class);
        this.availableFormations = new ArrayList<>();

        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        this.allNpcs = entities.getNpcs();
        this.allPortals = entities.getPortals();

        ConfigAPI configApi = api.getAPI(ConfigAPI.class);
        this.configOffensive = configApi.requireConfig("general.offensive");
    }

    @Override
    public NpcExtraFlag[] values() {
        return ExtraNpcFlags.values();
    }

    @Override
    public void setConfig(ConfigSetting<BestFormationConfig> arg0) {
        this.config = arg0.getValue();
    }

    @Override
    public void onStoppedBehavior() {
        if (hasOption(BehaviourOptions.TICK_STOPPED)) {
            onTickBehavior();
        }
    }

    @Override
    public void onTickBehavior() {
        if (nextCheck < System.currentTimeMillis()) {
            nextCheck = System.currentTimeMillis() + (config.timeToCheck * 1000);
            if (isAttacking() || safety.state() == Escaping.ENEMY) {
                useSelectableReadyWhenReady(getBestFormation());
            }
        }
    }

    private Formation getBestFormation() {
        if (hasOption(BehaviourOptions.RESPECT_NPC_FORMATION) && hasCustomFormation()) {
            return null;
        }

        Formation playerFormation = getPlayerTargetFormation();
        if (playerFormation != null) {
            return playerFormation;
        }

        if (shoulUseVeteran()) {
            return Formation.VETERAN;
        }
        if (hasFormation(Formation.WHEEL) && shoulFocusSpeed()) {
            return Formation.WHEEL;
        }
        if (shoulFocusPenetration()) {
            if (hasFormation(Formation.MOTH)) {
                return Formation.MOTH;
            } else if (hasFormation(Formation.DOUBLE_ARROW)) {
                return Formation.DOUBLE_ARROW;
            }
        }

        if (shoulUseCrab()) {
            return Formation.CRAB;
        } else if (shoulUseDiamond()) {
            return Formation.DIAMOND;
        }

        Formation damageFormation = getDamageFormation();
        if (damageFormation != null) {
            return damageFormation;
        }

        return getDefaultFormation();
    }

    private Formation getPlayerTargetFormation() {
        if (hasOption(BehaviourOptions.COPY_PLAYER_FORMATION)) {
            Player target = heroapi.getLocalTargetAs(Player.class);
            if (target != null && target.isValid()) {
                Formation targetFormation = target.getFormation();
                if (targetFormation != null && hasFormation(targetFormation)) {
                    return targetFormation;
                }
            }
        }

        return null;
    }

    private Formation getDamageFormation() {
        Entity target = heroapi.getLocalTarget();
        if (target instanceof Npc) {
            Npc npc = (Npc) target;
            if (npc.getHealth().getHp() > 30000 && shoulUseDrill()) {
                return Formation.DRILL;
            }
            if (shoulUseBat()) {
                return Formation.BAT;
            } else if (hasFormation(Formation.BARRAGE)) {
                return Formation.BARRAGE;
            }
        } else if (hasFormation(Formation.PINCER)) {
            return Formation.PINCER;
        }

        if (shoulUseDrill()) {
            return Formation.DRILL;
        } else if (hasFormation(Formation.STAR)) {
            return Formation.STAR;
        } else if (hasFormation(Formation.DOUBLE_ARROW)) {
            return Formation.DOUBLE_ARROW;
        } else if (hasFormation(Formation.CHEVRON)
                && (heroapi.getHealth().hpPercent() < 0.8 || heroapi.isInFormation(Formation.CHEVRON))) {
            return Formation.CHEVRON;
        }

        return null;
    }

    private Formation getDefaultFormation() {
        if (isAttacking()) {
            SelectableItem formation = SharedFunctions.getItemById(config.defaultFormation);
            return (Formation) formation;
        }

        return null;
    }

    private boolean shoulFocusPenetration() {
        Lockable target = heroapi.getLocalTarget();

        return target != null && target.isValid() && target.getHealth() != null
                && target.getHealth().getShield() > 100000
                && target.getHealth().shieldPercent() > 0.5
                && (target.getHealth().hpPercent() < 0.2 || heroapi.getHealth().getMaxShield() < 1000);
    }

    private boolean shoulFocusSpeed() {
        if (safety.state() == Escaping.ENEMY) {
            return true;
        }

        Entity target = heroapi.getLocalTarget();

        if (target == null || !target.isValid()) {
            return false;
        }

        double distance = heroapi.getLocationInfo().getCurrent().distanceTo(target.getLocationInfo());
        double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
        return distance > 500 && (speed >= heroapi.getSpeed() || heroapi.isInFormation(Formation.WHEEL));

    }

    private boolean shoulUseDrill() {
        return hasFormation(Formation.DRILL) && !shoulFocusSpeed() && !isInRange(500);
    }

    private boolean shoulUseDiamond() {
        return hasFormation(Formation.DIAMOND)
                && (heroapi.getHealth().hpPercent() < 0.7
                        || (heroapi.isInFormation(Formation.DIAMOND) && heroapi.getHealth().hpPercent() > 0.99))
                && heroapi.getHealth().shieldPercent() < 0.1
                && heroapi.getHealth().getMaxShield() > 50000;
    }

    private boolean shoulUseCrab() {
        if (!hasFormation(Formation.CRAB)
                || isFaster()) {
            return false;
        }

        try {
            if ((heroapi.getLaser() != null && heroapi.getLaser() == Laser.SAB_50)
                    || (heroapi.getHealth() != null && heroapi.getHealth().hpPercent() < 0.2
                            && heroapi.getHealth().getShield() > 300000)) {
                return true;
            }
        } catch (Exception e) {
            /* Nothing here */
        }
        return false;
    }

    private boolean shoulUseVeteran() {
        if (!hasFormation(Formation.VETERAN)) {
            return false;
        }
        if (hasTag(ExtraNpcFlags.USE_VETERAN) || hasOption(BehaviourOptions.USE_VETERAN)) {
            Lockable target = heroapi.getLocalTarget();
            if (target != null && target.isValid() && target instanceof Npc && target.getHealth() != null
                    && (target.getHealth().hpPercent() <= 0.15 && target.getHealth().getHp() < 200000)) {
                return true;
            }
        }
        return heroapi.getMap() != null && heroapi.getMap().isGG() && allNpcs.isEmpty()
                && allPortals.isEmpty();
    }

    private boolean shoulUseBat() {
        return hasFormation(Formation.BAT) && !isInRange(500);
    }

    private boolean useSelectableReadyWhenReady(Formation formation) {
        if (formation == null) {
            return false;
        }

        if (heroapi.isInFormation(formation)) {
            return false;
        }

        if (items.useItem(formation, 1000, ItemFlag.USABLE, ItemFlag.READY).isSuccessful()) {
            changeOffensiveConfig(formation);
            return true;
        }
        return false;
    }

    private void changeOffensiveConfig(Formation formation) {
        try {
            Character key = items.getKeyBind(formation);
            if (key == null || configOffensive.getValue().getLegacyFormation() == null
                    || configOffensive.getValue().getLegacyFormation().equals(key)) {
                configOffensive.setValue(new Config.ShipConfig(configOffensive.getValue().CONFIG, key));
            }
        } catch (Exception e) {
            /* Nothing here */
        }
    }

    private boolean hasTag(Enum<?> tag) {
        Lockable target = heroapi.getLocalTarget();
        return (target != null && target.isValid() && target instanceof Npc
                && ((Npc) target).getInfo().hasExtraFlag(tag));
    }

    private boolean hasCustomFormation() {
        Lockable target = heroapi.getLocalTarget();
        return (target != null && target.isValid() && target instanceof Npc
                && ((Npc) target).getInfo().getFormation().isPresent());
    }

    private boolean hasFormation(Formation formation) {
        if (config.formationsToUse == null) {
            return false;
        }

        if (config.formationsToUse.stream().anyMatch(s -> s.name() != null && s.name().equals(formation.name()))) {
            if (availableFormations.contains(formation)) {
                return true;
            } else if (items.getItem(formation, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.AVAILABLE).isPresent()) {
                availableFormations.add(formation);
                return true;
            }
        }

        return false;
    }

    private boolean hasOption(BehaviourOptions option) {
        return config.options.stream().anyMatch(s -> s.name() != null && s.name().equals(option.name()));
    }

    private boolean isAttacking() {
        Entity target = heroapi.getLocalTarget();

        if ((target instanceof Npc
                && (hasOption(BehaviourOptions.VS_NPC) || hasTag(ExtraNpcFlags.USE_AUTO_BEST_FORMATION)))
                || hasOption(BehaviourOptions.VS_PLAYERS)) {
            return target != null && target.isValid() && heroapi.isAttacking() && target.distanceTo(heroapi) < 1000
                    && !(heroapi.getEffects() != null
                            && heroapi.getEffects().toString().contains("76"));
        }

        return false;
    }

    private boolean isInRange(int range) {
        Lockable target = heroapi.getLocalTarget();
        return (target != null && target.isValid() && heroapi.distanceTo(target) < range);
    }

    private boolean isFaster() {
        Lockable target = heroapi.getLocalTarget();

        if (target == null || !target.isValid()) {
            return false;
        }

        double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
        return speed > heroapi.getSpeed();
    }
}
