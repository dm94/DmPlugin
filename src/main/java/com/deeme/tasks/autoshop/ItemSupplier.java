package com.deeme.tasks.autoshop;

import java.util.List;
import java.util.Locale;

import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.managers.BackpageAPI;

/**
 * Fills the item dropdown with the items currently sold in the shop, so the
 * categories, prices and limits are the ones the game is using right now.
 */
public class ItemSupplier implements Dropdown.Options<ShopItemData> {
    private final BackpageAPI backpage;

    public ItemSupplier(BackpageAPI backpage) {
        this.backpage = backpage;
    }

    @Override
    public List<ShopItemData> options() {
        return ShopCatalog.items(this.backpage);
    }

    @Override
    public String getText(ShopItemData item) {
        if (item == null || item.isEmpty()) {
            return "None";
        }

        String category = item.getCategory();
        if (category.isEmpty()) {
            return item.getDisplayName();
        }
        return category.toUpperCase(Locale.ROOT) + " || " + item.getDisplayName();
    }
}
