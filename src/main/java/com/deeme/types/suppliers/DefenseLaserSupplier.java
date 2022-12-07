package com.deeme.types.suppliers;

import com.github.manolo8.darkbot.config.Config.Loot.Sab;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.selectors.LaserSelector;
import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;

public class DefenseLaserSupplier implements LaserSelector, PrioritizedSupplier<Laser> {
    protected final PluginAPI api;
    protected final HeroAPI heroapi;
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
        if (shouldRcb()) {
            return Laser.RCB_140;
        } else if (shouldRsb()) {
            return Laser.RSB_75;
        } else if (shouldSab()) {
            return Laser.SAB_50;
        }
        return Laser.UCB_100;
    }

    private boolean shouldSab() {
        return this.sab.ENABLED && heroapi.getHealth().shieldPercent() <= sab.PERCENT
                && heroapi.getLocalTarget().getHealth().getShield() > sab.NPC_AMOUNT
                && (sab.CONDITION == null || sab.CONDITION.get(api).allows());
    }

    private boolean shouldRsb() {
        if (this.rsbActive) {
            Character key = items.getKeyBind(Laser.RSB_75);
            if (key != null) {
                return items.getItem(Laser.RSB_75, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY)
                        .isPresent();
            }
        }
        return false;
    }

    private boolean shouldRcb() {
        if (this.rsbActive) {
            Character key = items.getKeyBind(Laser.RCB_140);
            if (key != null) {
                return items.getItem(Laser.RCB_140, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY)
                        .isPresent();
            }
        }
        return false;
    }

    @Override
    public PrioritizedSupplier<Laser> getLaserSupplier() {
        return this;
    }
}