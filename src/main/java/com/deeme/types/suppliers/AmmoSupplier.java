package com.deeme.types.suppliers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.game.items.Item;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.managers.HeroItemsAPI;

public class AmmoSupplier implements PrioritizedSupplier<SelectableItem> {
    private final HeroItemsAPI items;
    private static final int MIN_AMMO = 200;

    List<Laser> damageOrder = Arrays.asList(Laser.RCB_140, Laser.RSB_75, Laser.UCB_100, Laser.A_BL, Laser.EMAA_20,
            Laser.VB_142,
            Laser.MCB_50, Laser.MCB_25, Laser.LCB_10);

    public AmmoSupplier(HeroItemsAPI items) {
        this.items = items;
    }

    public SelectableItem get() {
        return damageOrder.stream().filter(this::ableToUse).findFirst().orElse(null);
    }

    public SelectableItem getReverse() {
        return damageOrder.stream().filter(this::ableToUse).max(Comparator.comparing(i -> damageOrder.indexOf(i)))
                .orElse(null);
    }

    private boolean ableToUse(SelectableItem laser) {
        Optional<Item> item = items.getItem(laser, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY);
        return item.isPresent() && item.get().getQuantity() >= MIN_AMMO;
    }

}