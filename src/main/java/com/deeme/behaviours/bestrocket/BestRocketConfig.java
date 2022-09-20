package com.deeme.behaviours.bestrocket;

import com.deeme.types.gui.RocketSupplier;
import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.config.types.Options;
import com.github.manolo8.darkbot.gui.tree.components.JListField;

public class BestRocketConfig {
    @Option("Rocket for NPCs")
    @Editor(JListField.class)
    @Options(RocketSupplier.class)
    public String npcRocket = "";
}