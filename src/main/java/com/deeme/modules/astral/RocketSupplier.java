package com.deeme.modules.astral;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.game.items.Item;
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

    List<String> damageOrder = Arrays.asList(Rocket.AGT_500.getId(), Rocket.SP_100X.getId(), Rocket.PLT_3030.getId(),
            Rocket.PLT_2021.getId(), Rocket.PLT_2026.getId(), Rocket.R_310.getId());

    public RocketSupplier(HeroAPI heroapi, HeroItemsAPI items) {
        this.heroapi = heroapi;
        this.items = items;
    }

    public SelectableItem get() {
        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            if (shouldFocusSpeed(target)) {
                if (isAvailable(Rocket.R_IC3)) {
                    return Rocket.R_IC3;
                }
                if (isAvailable(Rocket.DCR_250)) {
                    return Rocket.DCR_250;
                }
            }

            if (shouldUsePLD() && isAvailable(Rocket.PLD_8)) {
                return Rocket.PLD_8;
            }

            try {
                return items.getItems(ItemCategory.ROCKETS).stream()
                        .filter(Item::isUsable)
                        .min(Comparator.comparing(i -> damageOrder.indexOf(i.getId()))).orElse(null);
            } catch (Exception e) {
                return null;
            }

        }
        return null;
    }

    public SelectableItem getReverse() {
        try {
            return items.getItems(ItemCategory.ROCKETS).stream()
                    .filter(Item::isUsable)
                    .max(Comparator.comparing(i -> damageOrder.indexOf(i.getId()))).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean shouldFocusSpeed(Lockable target) {
        double distance = heroapi.getLocationInfo().getCurrent().distanceTo(target.getLocationInfo());
        double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;

        return distance <= 600 || speed > heroapi.getSpeed();
    }

    private boolean shouldUsePLD() {
        return heroapi.getHealth().shieldPercent() < 0.8;
    }

    private boolean isAvailable(Rocket rocket) {
        return items.getItem(rocket, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY)
                .isPresent();
    }
}