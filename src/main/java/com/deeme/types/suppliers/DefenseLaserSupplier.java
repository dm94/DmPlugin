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
    private boolean useRsb, useRcb, useSab, rsbActive = false;

    private Sab sab;

    public DefenseLaserSupplier(PluginAPI api, HeroAPI heroapi, HeroItemsAPI items, Sab sab, boolean rsbActive) {
        this.api = api;
        this.heroapi = heroapi;
        this.items = items;
        this.sab = sab;
        this.rsbActive = rsbActive;
    }

    public Laser get() {
        return shouldRcb() ? Laser.RCB_140
                : shouldRsb() ? Laser.RSB_75
                        : shouldSab() ? Laser.SAB_50
                                : Laser.UCB_100;
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
    public Priority getPriority() {
        useRcb = shouldRcb();
        useRsb = shouldRsb();
        useSab = shouldSab();
        return useRcb ? Priority.HIGH : useRsb ? Priority.MODERATE : useSab ? Priority.LOW : Priority.LOWEST;
    }

    @Override
    public PrioritizedSupplier<Laser> getLaserSupplier() {
        return this;
    }
}