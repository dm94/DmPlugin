package com.deeme.types.suppliers;

import com.github.manolo8.darkbot.config.Config.Loot.Sab;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.selectors.LaserSelector;
import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.game.items.Item;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;

public class DefenseLaserSupplier implements LaserSelector, PrioritizedSupplier<Laser> {
    private final PluginAPI api;
    private final HeroAPI heroapi;
    private final HeroItemsAPI items;
    private boolean rsbActive = false;

    private Sab sab;

    public DefenseLaserSupplier(PluginAPI api, HeroAPI heroapi, HeroItemsAPI items, Sab sab, boolean rsbActive) {
        this.api = api;
        this.heroapi = heroapi;
        this.items = items;
        this.sab = sab;
        this.rsbActive = rsbActive;
    }

    public Laser get() {
        if (this.rsbActive) {
            if (ammoAvailable(Laser.RCB_140)) {
                return Laser.RCB_140;
            } else if (ammoAvailable(Laser.RSB_75)) {
                return Laser.RSB_75;
            }
        }

        if (shouldSab()) {
            return Laser.SAB_50;
        }
        return Laser.UCB_100;
    }

    private boolean shouldSab() {
        return this.sab.ENABLED && heroapi.getHealth().shieldPercent() <= sab.PERCENT
                && heroapi.getLocalTarget().getHealth().getShield() > sab.NPC_AMOUNT
                && (sab.CONDITION == null || sab.CONDITION.get(api).allows())
                && ammoAvailable(Laser.SAB_50);
    }

    private boolean ammoAvailable(SelectableItem laser) {
        Item item = items
                .getItem(laser, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY, ItemFlag.AVAILABLE).orElse(null);

        if (item == null) {
            return false;
        }

        return item.getTimer() == null || item.getTimer().getAvailableIn() <= 0;
    }

    @Override
    public PrioritizedSupplier<Laser> getLaserSupplier() {
        return this;
    }
}