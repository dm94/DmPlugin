package com.deeme.tasks.autoshop;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;

@Configuration("auto_shop")
public class Config {
    public @Option("buy_item_conditions") BuyItem item1 = new BuyItem();
    public @Option("buy_item_conditions") BuyItem item2 = new BuyItem();
    public @Option("buy_item_conditions") BuyItem item3 = new BuyItem();
    public @Option("buy_item_conditions") BuyItem item4 = new BuyItem();
    public @Option("buy_item_conditions") BuyItem item5 = new BuyItem();
    public @Option("buy_item_conditions") CustomBuyItem item6 = new CustomBuyItem();
    public @Option("buy_item_conditions") CustomBuyItem item7 = new CustomBuyItem();
}
