package com.deeme.types.config;

import com.deeme.types.gui.SelectableItemSupplier;
import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.config.types.Options;
import com.github.manolo8.darkbot.gui.tree.components.JListField;

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
    @Editor(JListField.class)
    @Options(SelectableItemSupplier.class)
    public String item = "";
}
