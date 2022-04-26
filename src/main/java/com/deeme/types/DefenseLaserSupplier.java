package com.deeme.types;

import com.deeme.types.config.Defense;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.Config.Loot.Sab;

import org.jetbrains.annotations.NotNull;

import eu.darkbot.api.extensions.selectors.LaserSelector;
import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.managers.HeroItemsAPI;

public class DefenseLaserSupplier implements LaserSelector, PrioritizedSupplier<Laser> {
    private Main main;
    private final HeroItemsAPI items;
    private long lastRsbUse = 0;
    private boolean useRsb, useSab, rsbActive;

    private Sab sab;

    public DefenseLaserSupplier(Main main, HeroItemsAPI items, Defense defense) {
        this.main = main;
        this.items = items;
        this.sab = defense.SAB;
        this.rsbActive = defense.useRSB;
    }

    public DefenseLaserSupplier(Main main, HeroItemsAPI items, Sab sab, boolean rsbActive) {
        this.main = main;
        this.items = items;
        this.sab = sab;
        this.rsbActive = rsbActive;
    }

    public Laser get() {
        return useRsb ? Laser.RSB_75
        : useSab ? Laser.SAB_50
        : Laser.UCB_100;
    }

    private boolean shouldSab() {
        return sab.ENABLED && this.main.hero.getHealth().shieldPercent() <= sab.PERCENT
        && this.main.hero.getLocalTarget().getHealth().getShield() > sab.NPC_AMOUNT
        && (sab.CONDITION == null || sab.CONDITION.get(main.pluginAPI).toBoolean());
    }

    private boolean shouldRsb() {
        if (rsbActive) {
            boolean isReady = items.getItem(Laser.RSB_75, ItemFlag.USABLE, ItemFlag.READY).isPresent();

            if (isReady && lastRsbUse < System.currentTimeMillis() - 1000) lastRsbUse = System.currentTimeMillis();
            return isReady && lastRsbUse > System.currentTimeMillis() - 500;
        }
        return false;
    }
    
    public Priority getPriority() {
        useRsb = shouldRsb();
        useSab = shouldSab();
        return useRsb ? Priority.MODERATE : useSab ? Priority.LOW : Priority.LOWEST;
    }

    @Override
    public @NotNull PrioritizedSupplier<Laser> getLaserSupplier() {
        return this;
    }
}