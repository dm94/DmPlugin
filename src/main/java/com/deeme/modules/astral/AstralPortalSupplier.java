package com.deeme.modules.astral;

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

    private boolean focusDamage = false;
    private boolean focusAmmo = false;
    private boolean focusGenerators = false;
    private boolean focusModules = false;

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
        Portal target = null;
        if (heroapi.getHealth().hpPercent() < 0.9) {
            target = portals.stream().filter(portal -> portal.getTypeId() == 99).findFirst().orElse(null);
        }
        if (needFocusAmmo() && target == null) {
            this.focusAmmo = true;
            target = portals.stream().filter(portal -> portal.getTypeId() == 91).findFirst().orElse(null);
        }
        if (astralShip != null) {
            if (astralShip.getMaxModules() > astralShip.getModules() && target == null) {
                this.focusModules = true;
                target = portals.stream().filter(portal -> portal.getTypeId() == 89).findFirst().orElse(null);
            }
            if (astralShip.getMaxGenerators() > astralShip.getGenerators() && target == null) {
                this.focusGenerators = true;
                target = portals.stream().filter(portal -> portal.getTypeId() == 95).findFirst().orElse(null);
            }
            if (astralShip.getMaxWeapons() > astralShip.getWeapons() && target == null) {
                this.focusDamage = true;
                target = portals.stream().filter(portal -> portal.getTypeId() == 87).findFirst().orElse(null);
            }
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

    private boolean needFocusAmmo() {
        return items.getItems(ItemCategory.LASERS).stream().mapToDouble(ammo -> ammo.getQuantity()).sum() <= 50000;
    }
}