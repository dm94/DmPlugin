package com.deeme.types.config;

import com.deeme.types.gui.SelectableItemSupplier;

import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.types.Condition;

public class ExtraKeyConditionsSelectable {

    @Option("general.enabled")
    public boolean enable = false;

    @Option("general.name")
    public String name = "";

    @Option("general.condition")
    public Condition condition;

    @Option("general.item")
    @Dropdown(options = SelectableItemSupplier.class)
    public String item = "";
}
