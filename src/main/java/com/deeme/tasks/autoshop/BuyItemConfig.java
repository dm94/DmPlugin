package com.deeme.tasks.autoshop;

import eu.darkbot.api.config.types.Condition;

/**
 * Common interface for buy item configurations to eliminate code duplication
 * between BuyItem and CustomBuyItem handling
 */
public interface BuyItemConfig {
    
    /**
     * Gets the next check time in milliseconds
     * @return next check time
     */
    long getNextCheck();
    
    /**
     * Sets the next check time in milliseconds
     * @param nextCheck the next check time
     */
    void setNextCheck(long nextCheck);
    
    /**
     * Checks if this item configuration is enabled
     * @return true if enabled
     */
    boolean isEnabled();
    
    /**
     * Gets the time to check in minutes
     * @return time to check in minutes
     */
    int getTimeToCheck();
    
    /**
     * Gets the quantity to purchase
     * @return quantity to purchase
     */
    int getQuantity();
    
    /**
     * Gets the shop item to purchase
     * @return the shop item
     */
    ShopItem getShopItem();
    
    /**
     * Gets the condition for purchasing
     * @return the condition, may be null
     */
    Condition getCondition();
    
    /**
     * Gets the quantity condition
     * @return the quantity condition
     */
    QuantityCondition getQuantityCondition();
}