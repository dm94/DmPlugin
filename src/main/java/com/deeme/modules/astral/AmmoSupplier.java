package com.deeme.modules.astral;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.game.items.Item;
import eu.darkbot.api.game.items.ItemCategory;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.managers.HeroItemsAPI;

public class AmmoSupplier implements PrioritizedSupplier<SelectableItem> {
    private final HeroItemsAPI items;

    List<String> damageOrder = Arrays.asList(Laser.RCB_140.getId(), Laser.RSB_75.getId(), Laser.UCB_100.getId(),
            Laser.MCB_50.getId(), Laser.MCB_25.getId(), Laser.LCB_10.getId());

    public AmmoSupplier(HeroItemsAPI items) {
        this.items = items;
    }

    public SelectableItem get() {
        return getAmmoAvailable().stream().min(Comparator.comparing(i -> damageOrder.indexOf(i.getId())))
                .orElse(null);
    }

    public SelectableItem getReverse() {
        return getAmmoAvailable().stream().max(Comparator.comparing(i -> damageOrder.indexOf(i.getId())))
                .orElse(null);
    }

    private List<? extends Item> getAmmoAvailable() {
        try {
            return items.getItems(ItemCategory.LASERS).stream()
                    .filter(item -> item.isUsable() && item.getQuantity() > 100).collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}