package com.deeme.types;

import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem.Formation;
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;

public class FormationSupplier implements PrioritizedSupplier<Formation> {
    protected final HeroItemsAPI items;
    protected final HeroAPI heroapi;

    private boolean useDiamond, useCrab, focusDamage, focusSpeed, focusPenetration = false;

    public FormationSupplier(HeroAPI heroapi, HeroItemsAPI items) {
        this.heroapi = heroapi;
        this.items = items;
    }

    public Formation get() {
        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            if (shoulFocusSpeed(target)) {
                if (items.getItem(Formation.WHEEL, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                    return Formation.WHEEL;
                }
            }
            if (shoulFocusPenetration(target)) {
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

            if (shoulFocusDamage(target)) {
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
        }

        return null;
    }

    private boolean shoulFocusDamage(Lockable target) {
        return target.getHealth().shieldPercent() < 0.3;
    }

    private boolean shoulFocusPenetration(Lockable target) {
        return target.getHealth().getShield() > 10000 && target.getHealth().shieldPercent() > 0.5
                && heroapi.getHealth().shieldPercent() < 0.2;
    }

    private boolean shoulFocusSpeed(Lockable target) {
        double distance = heroapi.getLocationInfo().getCurrent().distanceTo(target.getLocationInfo());
        double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
        return distance > 800 && speed >= heroapi.getSpeed();
    }

    private boolean shoulUseDiamond() {
        return heroapi.getHealth().hpPercent() < 0.7 && heroapi.getHealth().shieldPercent() < 0.1
                && heroapi.getHealth().getMaxShield() > 50000;
    }

    private boolean shoulUseCrab() {
        return heroapi.getLaser() == Laser.SAB_50
                || (heroapi.getHealth().hpPercent() < 0.2 && heroapi.getHealth().getShield() > 30000);
    }

    @Override
    public Priority getPriority() {
        Lockable target = heroapi.getLocalTarget();
        if (target != null) {
            focusDamage = shoulFocusDamage(target);
            focusPenetration = shoulFocusPenetration(target);
            focusSpeed = shoulFocusSpeed(target);
            useDiamond = shoulUseDiamond();
            useCrab = shoulUseCrab();
        }
        return focusSpeed ? Priority.HIGHEST
                : focusPenetration ? Priority.HIGH
                        : useCrab ? Priority.MODERATE
                                : useDiamond ? Priority.MODERATE : focusPenetration ? Priority.LOW : Priority.LOWEST;
    }
}