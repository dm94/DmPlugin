package com.deeme.tasks.autoshop;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.types.Condition;

@Configuration("buy_item_conditions")
public class BuyItem implements BuyItemConfig {
    public transient long nextCheck = 0;

    @Option("general.enabled")
    public boolean enable = false;

    @Option("general.next_check_time_minutes")
    @Number(min = 1, max = 1440, step = 1)
    public int timeToCheck = 20;

    @Option("buy_item_conditions.quantity")
    @Number(min = 1, step = 1, max = 1000000)
    public int quantity = 1;

    @Option("buy_item_conditions.item")
    @Dropdown(options = ItemSupplier.class)
    public String itemToBuy = "";

    @Option("general.condition")
    public Condition condition;

    @Option("quantity_condition")
    public QuantityCondition quantityCondition = new QuantityCondition();

    @Override
    public long getNextCheck() {
        return nextCheck;
    }

    @Override
    public void setNextCheck(long nextCheck) {
        this.nextCheck = nextCheck;
    }

    @Override
    public boolean isEnabled() {
        return enable;
    }

    @Override
    public int getTimeToCheck() {
        return timeToCheck;
    }

    @Override
    public int getQuantity() {
        return quantity;
    }

    @Override
    public ShopItem getShopItem() {
        return getItemById(itemToBuy);
    }

    @Override
    public Condition getCondition() {
        return condition;
    }

    @Override
    public QuantityCondition getQuantityCondition() {
        return quantityCondition;
    }

    private ItemSupported getItemById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        
        for (ItemSupported item : ItemSupported.values()) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }
}
