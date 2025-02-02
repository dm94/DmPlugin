package com.deeme.tasks.autoshop;

import com.deeme.types.gui.SelectableItemSupplier;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

@Configuration("quantity_condition")
public class QuantityCondition {
    @Option("general.enabled")
    public boolean active = false;

    @Option("quantity_condition.item_to_control")
    @Dropdown(options = SelectableItemSupplier.class)
    public String item = "";

    @Option("quantity_condition.min_quantity")
    @Number(min = 0, step = 1, max = 1000000)
    public int quantity = 0;
}
