package com.deeme.behaviours.bestrocket;

import com.deeme.types.gui.RocketSupplier;
import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.config.types.Options;
import com.github.manolo8.darkbot.gui.tree.components.JListField;

import eu.darkbot.api.config.annotations.Option;

public class BestRocketConfig {
    @Option(value = "best_rocket.npc_rocket")
    @Editor(JListField.class)
    @Options(RocketSupplier.class)
    public String npcRocket = "";
}