package com.deeme.tasks.autoshop;

import java.util.Locale;

/**
 * Tabs of the in game shop, each one exposes a different itemData list.
 */
public enum ShopPage {
    AMMO("Ammo"),
    SPECIALS("Specials"),
    BOOSTER("Booster");

    private final String name;

    ShopPage(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getTpl() {
        return "internalDock" + name;
    }

    /**
     * Returns the shop page that sells items of the given category, or null when
     * the category is unknown and every page has to be checked.
     */
    public static ShopPage forCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return null;
        }

        switch (category.trim().toLowerCase(Locale.ROOT)) {
            case "booster":
            case "boosters":
                return BOOSTER;
            case "special":
            case "specials":
            case "petgear":
                return SPECIALS;
            default:
                return AMMO;
        }
    }
}
