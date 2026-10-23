package com.deeme.tasks.autoshop;

/**
 * Unified interface for shop items, either an item picked from the shop
 * listing or a custom item configured by the user
 */
public interface ShopItem {
    
    /**
     * Gets the item identifier used for purchase
     * @return the item ID or name
     */
    String getItemId();
    
    /**
     * Gets the category for the shop purchase
     * @return the category string
     */
    String getCategory();
    
    /**
     * Gets the credits price for a single unit
     * @return credits price
     */
    double getCreditsPrice();
    
    /**
     * Gets the uridium price for a single unit
     * @return uridium price
     */
    double getUridiumPrice();
    
    /**
     * Gets the display name for UI purposes
     * @return display name
     */
    String getDisplayName();
}