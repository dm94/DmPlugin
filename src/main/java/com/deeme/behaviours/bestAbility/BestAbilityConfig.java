package com.deeme.behaviours.bestability;

import java.util.EnumSet;
import java.util.Set;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;

@Configuration("best_ability")
public class BestAbilityConfig {
    @Option(value = "general.npc_enabled")
    public boolean npcEnabled = true;

    @Option(value = "best_ability.supported_abilities")
    @Dropdown(multi = true)
    public transient Set<SupportedAbilities> supportedAbilities = EnumSet.allOf(SupportedAbilities.class);
}