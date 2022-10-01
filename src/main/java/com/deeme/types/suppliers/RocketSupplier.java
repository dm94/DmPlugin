package com.deeme.types.suppliers;

import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem.Rocket;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;

public class RocketSupplier implements PrioritizedSupplier<Rocket> {
    private final HeroAPI heroapi;
    private final HeroItemsAPI items;
    private final Double hpMin;

    private boolean stopEnemy = false;
    private boolean usePLD = false;

    public RocketSupplier(HeroAPI heroapi, HeroItemsAPI items, double hpMin) {
        this.heroapi = heroapi;
        this.items = items;
        this.hpMin = hpMin;
    }

    public Rocket get() {
        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            if (shoulFocusSpeed(target)) {
                if (items.getItem(Rocket.R_IC3, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY)
                        .isPresent()) {
                    return Rocket.R_IC3;
                }
                if (items.getItem(Rocket.DCR_250, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY)
                        .isPresent()) {
                    return Rocket.DCR_250;
                }
            }

            if (shoulUsePLD(target) && items
                    .getItem(Rocket.PLD_8, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY).isPresent()) {
                return Rocket.PLD_8;
            }
        }
        if (items.getItem(Rocket.PLT_3030, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY).isPresent()) {
            return Rocket.PLT_3030;
        } else if (items.getItem(Rocket.PLT_2021, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY)
                .isPresent()) {
            return Rocket.PLT_2021;
        } else if (items.getItem(Rocket.PLT_2026, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY)
                .isPresent()) {
            return Rocket.PLT_2026;
        } else if (items.getItem(Rocket.R_310, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY)
                .isPresent()) {
            return Rocket.R_310;
        }
        return null;
    }

    private boolean shoulFocusSpeed(Lockable target) {
        double distance = heroapi.getLocationInfo().getCurrent().distanceTo(target.getLocationInfo());
        double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;

        return (distance > 400 && speed > heroapi.getSpeed())
                || (distance < 600 && speed > heroapi.getSpeed() && heroapi.getHealth().hpPercent() < hpMin);
    }

    private boolean shoulUsePLD(Lockable target) {
        return target instanceof Movable && ((Movable) target).isAiming(heroapi)
                && heroapi.getHealth().hpPercent() < 0.5;
    }

    @Override
    public Priority getPriority() {
        Lockable target = heroapi.getLocalTarget();
        if (target != null) {
            stopEnemy = shoulFocusSpeed(target);
            usePLD = shoulUsePLD(target);
        }
        return stopEnemy ? Priority.MODERATE : usePLD ? Priority.LOW : Priority.LOWEST;
    }
}