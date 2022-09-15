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
    private boolean useRsb, useRcb, useSab, rsbActive = false;

    private Sab sab;
    private long usedRsb = 0;

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
            boolean isReady = items.getItem(Laser.RSB_75, ItemFlag.USABLE, ItemFlag.READY).isPresent();

            Character key = items.getKeyBind(Laser.RSB_75);
            if (key != null && isReady && usedRsb < System.currentTimeMillis() - 1000) {
                return isReady && usedRsb < System.currentTimeMillis() - 500;
            }
        }
        return false;
    }

    private boolean shouldRcb() {
        if (this.rsbActive) {
            boolean isReady = items.getItem(Laser.RCB_140, ItemFlag.USABLE, ItemFlag.READY).isPresent();

            Character key = items.getKeyBind(Laser.RCB_140);
            if (key != null && isReady && usedRsb < System.currentTimeMillis() - 1000) {
                return isReady && usedRsb < System.currentTimeMillis() - 500;
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