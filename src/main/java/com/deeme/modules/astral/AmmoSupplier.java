package com.deeme.modules.astral;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.game.items.ItemCategory;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.managers.HeroItemsAPI;

public class AmmoSupplier implements PrioritizedSupplier<SelectableItem> {
    private final HeroItemsAPI items;

    List<String> damageOrder = Arrays.asList(Laser.UCB_100.getId(),
            Laser.MCB_50.getId(),
            Laser.MCB_25.getId(),
            Laser.LCB_10.getId());

    public AmmoSupplier(HeroItemsAPI items) {
        this.items = items;
    }

    public SelectableItem get() {
        try {
            return items.getItems(ItemCategory.LASERS).stream()
                    .filter(item -> item.isUsable() && item.isAvailable() && item.getQuantity() > 100)
                    .sorted(Comparator.comparing(i -> damageOrder.indexOf(i.getId()))).findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Priority getPriority() {
        return Priority.MODERATE;
    }
}