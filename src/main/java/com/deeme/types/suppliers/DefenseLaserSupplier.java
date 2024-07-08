package com.deeme.types.suppliers;

import com.deeme.behaviours.defense.AmmoConfig;
import com.deeme.types.SharedFunctions;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.game.items.Item;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;

public class DefenseLaserSupplier {
    private final PluginAPI api;
    private final HeroAPI heroapi;
    private final HeroItemsAPI items;
    private AmmoConfig ammoConfig;

    public DefenseLaserSupplier(PluginAPI api, HeroAPI heroapi, HeroItemsAPI items, AmmoConfig ammoConfig) {
        this.api = api;
        this.heroapi = heroapi;
        this.items = items;
        this.ammoConfig = ammoConfig;
    }

    public SelectableItem get() {
        if (this.ammoConfig.useRSB) {
            if (ammoAvailable(Laser.RCB_140)) {
                return Laser.RCB_140;
            } else if (ammoAvailable(Laser.RSB_75)) {
                return Laser.RSB_75;
            }
        }

        if (shouldSab()) {
            return Laser.SAB_50;
        }

        return SharedFunctions.getItemById(this.ammoConfig.defaultLaser);
    }

    private boolean shouldSab() {
        return this.ammoConfig.sab.ENABLED && heroapi.getHealth().shieldPercent() <= this.ammoConfig.sab.PERCENT
                && heroapi.getLocalTarget().getHealth().getShield() > this.ammoConfig.sab.NPC_AMOUNT
                && (this.ammoConfig.sab.CONDITION == null || this.ammoConfig.sab.CONDITION.get(api).allows())
                && ammoAvailable(Laser.SAB_50);
    }

    private boolean ammoAvailable(SelectableItem laser) {
        Item item = items
                .getItem(laser, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY, ItemFlag.AVAILABLE).orElse(null);

        if (item == null) {
            return false;
        }

        return item.getQuantity() > 200 && (item.getTimer() == null || item.getTimer().getAvailableIn() <= 0);
    }
}