package com.deeme.types;

import java.util.Collection;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.items.ItemCategory;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;

public class AstralPortalSupplier implements PrioritizedSupplier<Portal> {
    protected HeroAPI heroapi;
    protected HeroItemsAPI items;
    protected Collection<? extends Portal> portals;
    protected AstralShip astralShip;

    private boolean focusHealth, focusDamage, focusAmmo, focusGenerators, focusModules = false;

    /*
     * 1 - Base
     * 87 - Weapon
     * 88 - Weapon (Hard)
     * 89 - Generator
     * 90 - Generator (Hard)
     * 91 - Ammo
     * 92 - Ammo (Hard)
     * 95 - Module
     * 96 - Module (Hard)
     * 99 - PV
     */

    public AstralPortalSupplier(PluginAPI api, AstralShip astralShip) {
        this.heroapi = api.getAPI(HeroAPI.class);
        this.items = api.getAPI(HeroItemsAPI.class);
        this.astralShip = astralShip;

        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        this.portals = entities.getPortals();
    }

    public void setAstralShip(AstralShip astralShip) {
        this.astralShip = astralShip;
    }

    public Portal get() {
        getPriority();
        Portal target = null;
        if (focusHealth) {
            target = portals.stream().filter(portal -> portal.getTypeId() == 99).findFirst().orElse(null);
        }
        if (focusAmmo && target == null) {
            target = portals.stream().filter(portal -> portal.getTypeId() == 91).findFirst().orElse(null);
        }
        if (focusModules && target == null) {
            target = portals.stream().filter(portal -> portal.getTypeId() == 89).findFirst().orElse(null);
        }
        if (focusGenerators && target == null) {
            target = portals.stream().filter(portal -> portal.getTypeId() == 95).findFirst().orElse(null);
        }
        if (focusDamage && target == null) {
            target = portals.stream().filter(portal -> portal.getTypeId() == 87).findFirst().orElse(null);
        }

        if (target == null) {
            target = getHardPortals();
        }

        return target;
    }

    public Portal getHardPortals() {
        Portal target = null;

        if (focusAmmo) {
            target = portals.stream().filter(portal -> portal.getTypeId() == 92).findFirst().orElse(null);
        }
        if (focusModules && target == null) {
            target = portals.stream().filter(portal -> portal.getTypeId() == 96).findFirst().orElse(null);
        }
        if (focusGenerators && target == null) {
            target = portals.stream().filter(portal -> portal.getTypeId() == 90).findFirst().orElse(null);
        }
        if (focusDamage && target == null) {
            target = portals.stream().filter(portal -> portal.getTypeId() == 88).findFirst().orElse(null);
        }

        return target;
    }

    @Override
    public Priority getPriority() {
        this.focusHealth = heroapi.getHealth().hpPercent() < 0.9;
        this.focusAmmo = needFocusAmmo();
        if (astralShip != null) {
            this.focusModules = astralShip.getMaxModules() > astralShip.getModules();
            this.focusDamage = astralShip.getMaxWeapons() > astralShip.getWeapons();
            this.focusGenerators = astralShip.getMaxGenerators() > astralShip.getGenerators();
        }

        return focusHealth ? Priority.HIGHEST
                : focusAmmo ? Priority.HIGH
                        : focusModules ? Priority.MODERATE : focusDamage ? Priority.LOW : Priority.LOWEST;
    }

    private boolean needFocusAmmo() {
        return items.getItems(ItemCategory.LASERS).stream().mapToDouble(ammo -> ammo.getQuantity()).sum() <= 50000;
    }
}