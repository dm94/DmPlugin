package com.deeme.behaviours.bestrocket;

import java.util.EnumSet;
import java.util.Set;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.game.items.SelectableItem.Rocket;

@Configuration("best_rocket")
public class BestRocketConfig {
    @Option("best_rocket.npc_rocket")
    @Dropdown(options = RocketSupplier.class)
    public String npcRocket = "";

    @Option("best_rocket.rockets_to_use_npcs")
    @Dropdown(multi = true)
    public Set<Rocket> rocketsToUseNPCs = EnumSet.noneOf(Rocket.class);

    @Option("best_rocket.rockets_to_use_players")
    @Dropdown(multi = true)
    public Set<Rocket> rocketsToUsePlayers = EnumSet.allOf(Rocket.class);

    @Option("general.tick_stopped")
    public boolean tickStopped = false;
}