package com.deeme.behaviours.bestformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.github.manolo8.darkbot.config.NpcExtraFlag;
import com.github.manolo8.darkbot.core.itf.NpcExtraProvider;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem.Formation;
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.AuthAPI;
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
    public void onTickBehavior() {
        if (nextCheck < System.currentTimeMillis()) {
            nextCheck = System.currentTimeMillis() + (config.timeToCheck * 1000);
            if (isAttacking() || safety.state() == Escaping.ENEMY) {
                useSelectableReadyWhenReady(getBestFormation());
            }
        }
    }

    private Formation getBestFormation() {
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

        Entity target = heroapi.getLocalTarget();
        if (target instanceof Npc) {
            if (shoulUseBat()) {
                return Formation.BAT;
            } else if (hasFormation(Formation.BARRAGE)) {
                return Formation.BARRAGE;
            }
        } else if (hasFormation(Formation.PINCER)) {
            return Formation.PINCER;
        }

        if (hasFormation(Formation.STAR)) {
            return Formation.STAR;
        } else if (hasFormation(Formation.DRILL) && !shoulFocusSpeed()
                && !isFaster()) {
            return Formation.DRILL;
        } else if (hasFormation(Formation.DOUBLE_ARROW)) {
            return Formation.DOUBLE_ARROW;
        } else if (hasFormation(Formation.CHEVRON)
                && (heroapi.getHealth().hpPercent() < 0.8 || heroapi.isInFormation(Formation.CHEVRON))) {
            return Formation.CHEVRON;
        }

        return null;
    }

    private boolean shoulFocusPenetration() {
        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            return target.getHealth() != null && target.getHealth().getShield() > 100000
                    && target.getHealth().shieldPercent() > 0.5
                    && (target.getHealth().hpPercent() < 0.2 || heroapi.getHealth().getMaxShield() < 1000);
        }
        return false;
    }

    private boolean shoulFocusSpeed() {
        if (safety.state() == Escaping.ENEMY) {
            return true;
        }

        Entity target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            double distance = heroapi.getLocationInfo().getCurrent().distanceTo(target.getLocationInfo());
            double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
            return distance > 500 && (speed >= heroapi.getSpeed() || heroapi.isInFormation(Formation.WHEEL));
        }

        return false;
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
            System.err.println(e.getMessage());
        }
        return false;
    }

    private boolean shoulUseVeteran() {
        if (hasFormation(Formation.VETERAN)) {
            if (config.useVeteran || hasTag(ExtraNpcFlags.USE_VETERAN)) {
                Lockable target = heroapi.getLocalTarget();
                if (target != null && target.isValid() && target instanceof Npc) {
                    return target.getHealth() != null
                            && (target.getHealth().hpPercent() <= 0.15 && target.getHealth().getHp() < 200000);
                }
            }
            return heroapi.getMap() != null && heroapi.getMap().isGG() && allNpcs.isEmpty() && allPortals.isEmpty();
        }
        return false;
    }

    private boolean shoulUseBat() {
        return hasFormation(Formation.BAT)
                && !isFaster();
    }

    private boolean useSelectableReadyWhenReady(Formation formation) {
        return (formation != null && !heroapi.isInFormation(formation)
                && items.useItem(formation, 1000, ItemFlag.USABLE, ItemFlag.READY).isSuccessful());
    }

    private boolean hasTag(Enum<?> tag) {
        Lockable target = heroapi.getLocalTarget();
        return (target != null && target.isValid() && target instanceof Npc
                && ((Npc) target).getInfo().hasExtraFlag(tag));
    }

    private boolean hasFormation(Formation formation) {
        if (config.formationsToUse.stream().anyMatch(s -> s.name().equals(formation.name()))) {
            if (availableFormations.contains(formation)) {
                return true;
            } else if (items.getItem(formation, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.AVAILABLE).isPresent()) {
                availableFormations.add(formation);
                return true;
            }
        }

        return false;
    }

    private boolean isAttacking() {
        Entity target = heroapi.getLocalTarget();
        return target != null && target.isValid() && heroapi.isAttacking() && target.distanceTo(heroapi) < 1000
                && !(heroapi.getEffects() != null
                        && heroapi.getEffects().toString().contains("76"));
    }

    private boolean isFaster() {
        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
            return speed > heroapi.getSpeed();
        }
        return false;
    }
}
