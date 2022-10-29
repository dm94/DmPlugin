package com.deeme.behaviours.bestrocket;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;

@Configuration("best_rocket")
public class BestRocketConfig {
    @Option("best_rocket.npc_rocket")
    @Dropdown(options = RocketSupplier.class)
    public String npcRocket = "";

    @Option("best_rocket.useicr")
    public boolean useICRorDCR = true;

    @Option("best_rocket.usepld")
    public boolean usePLD = true;
}