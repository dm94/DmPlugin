package com.deeme.types.config;

import com.deeme.types.gui.SelectableItemSupplier;
import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.config.types.Options;
import com.github.manolo8.darkbot.gui.tree.components.JListField;

import eu.darkbot.api.config.types.Condition;

public class ExtraKeyConditionsSelectable {

    @Option("Enable")
    public boolean enable = false;

    @Option("Name (Optional)")
    public String name = "";

    @Option("Condition")
    public Condition CONDITION;

    @Option("Item to be used")
    @Editor(JListField.class)
    @Options(SelectableItemSupplier.class)
    public String item = "";
}
