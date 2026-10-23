package com.deeme.tasks.autoshop;

import java.util.Locale;
import java.util.Objects;

/**
 * An item as the shop sells it. When the user picks an item in the dropdown it
 * is stored in the configuration, so the task keeps working with the last known
 * data even if the shop page can not be read.
 */
public class ShopItemData implements ShopItem {
    public static final String CREDITS = "credits";
    public static final String URIDIUM = "uridium";

    public String itemId = "";
    public String name = "";
    public String category = "";
    public ShopPage page;
    public double price;
    public String currency = "";
    public int maxAmount;
    public int dailyLimit = -1;

    public ShopItemData() {
    }

    /**
     * Builds the shop data of an item that doesn't come from the shop listing
     * (a custom item), using the values configured by the user.
     */
    public ShopItemData(ShopItem source) {
        this.itemId = source.getItemId() == null ? "" : source.getItemId();
        this.name = source.getDisplayName() == null ? this.itemId : source.getDisplayName();
        this.category = source.getCategory() == null ? "" : source.getCategory();
        this.page = ShopPage.forCategory(this.category);

        if (source.getCreditsPrice() > 0) {
            this.price = source.getCreditsPrice();
            this.currency = CREDITS;
        } else if (source.getUridiumPrice() > 0) {
            this.price = source.getUridiumPrice();
            this.currency = URIDIUM;
        }
    }

    public ShopItemData(ShopItemData source) {
        copyFrom(source);
    }

    public void copyFrom(ShopItemData source) {
        this.itemId = source.itemId;
        this.name = source.name;
        this.category = source.category;
        this.page = source.page;
        this.price = source.price;
        this.currency = source.currency;
        this.maxAmount = source.maxAmount;
        this.dailyLimit = source.dailyLimit;
    }

    public boolean isEmpty() {
        return this.itemId == null || this.itemId.isEmpty();
    }

    @Override
    public String getItemId() {
        return itemId;
    }

    @Override
    public String getCategory() {
        return category == null ? "" : category;
    }

    @Override
    public double getCreditsPrice() {
        return CREDITS.equals(currency) ? price : 0;
    }

    @Override
    public double getUridiumPrice() {
        return URIDIUM.equals(currency) ? price : 0;
    }

    @Override
    public String getDisplayName() {
        return name == null || name.isEmpty() ? itemId : name;
    }

    /**
     * The shop uses "real"/"virtual" to refer to uridium/credits.
     */
    public static String normalizeCurrency(String currency) {
        if (currency == null) {
            return "";
        }
        if ("real".equalsIgnoreCase(currency)) {
            return URIDIUM;
        }
        if ("virtual".equalsIgnoreCase(currency)) {
            return CREDITS;
        }
        return currency.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ShopItemData)) {
            return false;
        }
        return Objects.equals(itemId, ((ShopItemData) other).itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(itemId);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
