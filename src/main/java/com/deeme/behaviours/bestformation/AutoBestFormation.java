package com.deeme.behaviours.bestformation;

import java.util.Arrays;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem.Formation;
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.utils.SafetyFinder;
import eu.darkbot.shared.utils.SafetyFinder.Escaping;

@Feature(name = "Auto Best Formation", description = "Automatically switches formations")
public class AutoBestFormation implements Behavior, Configurable<BestFormationConfig> {

    protected final PluginAPI api;
    protected final HeroAPI heroapi;
    protected final HeroItemsAPI items;
    protected final SafetyFinder safety;
    private BestFormationConfig config;

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

    }

    @Override
    public void setConfig(ConfigSetting<BestFormationConfig> arg0) {
        this.config = arg0.getValue();
    }

    @Override
    public void onTickBehavior() {
        Entity target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            if (config.npcEnabled || !(target instanceof Npc)) {
                useSelectableReadyWhenReady(getBestFormation());
            }
        } else if (safety.state() == Escaping.ENEMY) {
            useSelectableReadyWhenReady(getBestFormation());
        }
    }

    private Formation getBestFormation() {
        if (config.useVeteran && shoulUseVeteran()
                && items.getItem(Formation.VETERAN, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
            return Formation.VETERAN;
        }
        if (shoulFocusSpeed() && items.getItem(Formation.WHEEL, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
            return Formation.WHEEL;
        }
        if (shoulFocusPenetration()) {
            if (items.getItem(Formation.MOTH, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Formation.MOTH;
            }
            if (items.getItem(Formation.DOUBLE_ARROW, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Formation.DOUBLE_ARROW;
            }
        }

        if (shoulUseCrab() && items.getItem(Formation.CRAB, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
            return Formation.CRAB;
        }

        if (shoulUseDiamond() && items.getItem(Formation.DIAMOND, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
            return Formation.DIAMOND;
        }

        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            if (target instanceof Npc) {
                if (items.getItem(Formation.BAT, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                    return Formation.BAT;
                }
                if (items.getItem(Formation.BARRAGE, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                    return Formation.BARRAGE;
                }
            }

            if (items.getItem(Formation.PINCER, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Formation.PINCER;
            }
            if (items.getItem(Formation.STAR, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Formation.STAR;
            }
            if (items.getItem(Formation.DRILL, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Formation.DRILL;
            }
            if (items.getItem(Formation.DOUBLE_ARROW, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Formation.DOUBLE_ARROW;
            }
        }

        return null;
    }

    private boolean shoulFocusPenetration() {
        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            return target.getHealth() != null && target.getHealth().getShield() > 10000
                    && target.getHealth().shieldPercent() > 0.5
                    && heroapi.getHealth().shieldPercent() < 0.2;
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
            return distance > 800 && speed >= heroapi.getSpeed();
        }

        return false;
    }

    private boolean shoulUseDiamond() {
        return heroapi.getHealth().hpPercent() < 0.7 && heroapi.getHealth().shieldPercent() < 0.1
                && heroapi.getHealth().getMaxShield() > 50000;
    }

    private boolean shoulUseCrab() {
        return (heroapi.getLaser() != null && heroapi.getLaser() == Laser.SAB_50)
                || (heroapi.getHealth().hpPercent() < 0.2 && heroapi.getHealth().getShield() > 30000);
    }

    private boolean shoulUseVeteran() {
        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid() && target instanceof Npc) {
            return target.getHealth() != null && target.getHealth().hpPercent() <= 0.15;
        }
        return false;
    }

    private boolean useSelectableReadyWhenReady(Formation formation) {
        if (formation == null) {
            return false;
        }

        if (!heroapi.isInFormation(formation)
                && items.useItem(formation, 500, ItemFlag.USABLE, ItemFlag.READY).isSuccessful()) {
            return true;
        }

        return false;
    }
}
