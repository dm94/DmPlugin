package com.deeme.types.config;

import com.deeme.types.gui.SelectableItemSupplier;

import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.types.Condition;

public class ExtraKeyConditionsSelectable {

    @Option(value = "general.enabled")
    public boolean enable = false;

    @Option(value = "general.name")
    public String name = "";

    @Option(value = "general.condition")
    public Condition condition;

    @Option(value = "general.item")
    @Dropdown(options = SelectableItemSupplier.class)
    public String item = "";
}
