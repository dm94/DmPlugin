package com.deeme.types;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.game.items.ItemCategory;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Rocket;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;

public class RocketSupplier implements PrioritizedSupplier<SelectableItem> {
    private final HeroAPI heroapi;
    private final HeroItemsAPI items;
    private final Double hpMin;

    private boolean stopEnemy = false;
    private boolean usePLD = false;

    List<String> damageOrder = Arrays.asList(Rocket.PLT_3030.getId(), Rocket.PLT_2021.getId(), Rocket.PLT_2026.getId(), Rocket.R_310.getId());

    public RocketSupplier(HeroAPI heroapi, HeroItemsAPI items, double hpMin) {
        this.heroapi = heroapi;
        this.items = items;
        this.hpMin = hpMin;
    }

    public SelectableItem get() {
        boolean isAvailable = false;
        if (stopEnemy) {
            isAvailable = items.getItem(Rocket.R_IC3, ItemFlag.USABLE, ItemFlag.READY).isPresent();
            if (isAvailable) {
                return Rocket.R_IC3;
            } else {
                isAvailable = items.getItem(Rocket.DCR_250, ItemFlag.USABLE, ItemFlag.READY).isPresent();
                if (isAvailable) {
                    return Rocket.DCR_250;
                }
            }
        }

        if (usePLD) {
            isAvailable = items.getItem(Rocket.PLD_8, ItemFlag.USABLE, ItemFlag.READY).isPresent();
            if (isAvailable) {
                return Rocket.PLD_8;
            }
        }

        try {
            return items.getItems(ItemCategory.ROCKETS).stream()
            .filter(item -> item.isUsable() && item.isAvailable()).sorted(Comparator.comparing(i -> damageOrder.indexOf(i.getId()))).findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean shoulFocusSpeed(Lockable target) {
        double distance = heroapi.getLocationInfo().getCurrent().distanceTo(target.getLocationInfo());
        double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;

        return (distance > 400 && speed > heroapi.getSpeed()) || (distance < 600 && speed > heroapi.getSpeed() && heroapi.getHealth().hpPercent() < hpMin);
    }

    private boolean shoulUsePLD(Lockable target) {
        return target instanceof Movable ? ((Movable) target).isAiming(heroapi) : false;
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