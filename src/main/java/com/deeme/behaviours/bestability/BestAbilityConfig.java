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

    @Option("best_ability.min_health_health")
    @Percentage
    public double minHealthToUseHealth = 0.5;

    @Option("best_ability.min_health_damage")
    @Number(max = 1000000000, step = 1000)
    public int minHealthToUseDamage = 100000;

    @Option("best_ability.supported_abilities")
    @Dropdown(multi = true)
    public transient Set<SupportedAbilities> supportedAbilities = EnumSet.allOf(SupportedAbilities.class);
}