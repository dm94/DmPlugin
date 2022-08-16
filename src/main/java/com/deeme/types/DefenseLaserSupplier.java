package com.deeme.types;

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
    private boolean useRsb, useRcb, useSab, rsbActive;

    private Sab sab;

    public DefenseLaserSupplier(PluginAPI api, HeroAPI heroapi, HeroItemsAPI items, Sab sab, boolean rsbActive) {
        this.api = api;
        this.heroapi = heroapi;
        this.items = items;
        this.sab = sab;
        this.rsbActive = rsbActive;
    }

    public Laser get() {
        return useRcb ? Laser.RCB_140
                : useRsb ? Laser.RSB_75
                        : useSab ? Laser.SAB_50
                                : Laser.UCB_100;
    }

    private boolean shouldSab() {
        return sab.ENABLED && heroapi.getHealth().shieldPercent() <= sab.PERCENT
                && heroapi.getLocalTarget().getHealth().getShield() > sab.NPC_AMOUNT
                && (sab.CONDITION == null || sab.CONDITION.get(api).toBoolean());
    }

    private boolean shouldRsb() {
        if (rsbActive) {
            return items.getItem(Laser.RSB_75, ItemFlag.USABLE, ItemFlag.READY)
                    .isPresent();
        }
        return false;
    }

    private boolean shouldRcb() {
        if (rsbActive) {
            return items.getItem(Laser.RCB_140, ItemFlag.USABLE, ItemFlag.READY)
                    .isPresent();
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