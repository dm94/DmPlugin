package com.deeme.tasks.autoshop;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.types.Condition;

@Configuration("buy_item_conditions")
public class CustomBuyItem implements BuyItemConfig {
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
    public CustomItem customItem = new CustomItem();

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
        return customItem;
    }

    @Override
    public Condition getCondition() {
        return condition;
    }

    @Override
    public QuantityCondition getQuantityCondition() {
        return quantityCondition;
    }
}
