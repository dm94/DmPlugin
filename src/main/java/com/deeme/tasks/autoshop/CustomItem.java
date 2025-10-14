package com.deeme.tasks.autoshop;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

@Configuration("buy_item_conditions.custom_item")
public class CustomItem implements ShopItem {
    @Option("buy_item_conditions.custom_item.name")
    public String name = "";

    @Option("buy_item_conditions.custom_item.category")
    public String category = "";

    @Option("buy_item_conditions.custom_item.category.credits_price")
    @Number(min = 1, step = 1)
    public int creditsPrice = 0;

    @Option("buy_item_conditions.custom_item.category.uri_price")
    @Number(min = 1, step = 1)
    public int uriPrice = 0;

    @Override
    public String getItemId() {
        return name;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public double getCreditsPrice() {
        return creditsPrice;
    }

    @Override
    public double getUridiumPrice() {
        return uriPrice;
    }

    @Override
    public String getDisplayName() {
        return name;
    }
}
