package com.deeme.behaviours.bestability;

import java.util.EnumSet;
import java.util.Set;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Percentage;

@Configuration("best_ability")
public class BestAbilityConfig {
    @Option("general.npc_enabled")
    public boolean npcEnabled = true;

    @Option("general.next_check_time")
    @Number(max = 300, step = 1)
    public int timeToCheck = 20;

    @Option("best_ability.min_health_health")
    @Percentage
    public double minHealthToUseHealth = 0.5;

    @Option("best_ability.min_health_damage")
    @Number(max = 1000000000, step = 1000)
    public int minHealthToUseDamage = 100000;

    @Option("best_ability.supported_abilities")
    @Dropdown(multi = true)
    public Set<SupportedAbilities> supportedAbilities = EnumSet.allOf(SupportedAbilities.class);

    @Option("best_ability.abilities_to_use_everytime")
    @Dropdown(multi = true)
    public Set<SupportedAbilities> abilitiesToUseEverytime = EnumSet.noneOf(SupportedAbilities.class);

    @Option("general.tick_stopped")
    public boolean tickStopped = false;
}