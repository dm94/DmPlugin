package com.deeme.behaviours.profilechanger;

import com.deeme.types.gui.ConfigSupplier;
import com.github.manolo8.darkbot.config.ConfigManager;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.types.Condition;

@Configuration("profile_changer")
public class ProfileChangerConfig {
    @Option("general.enabled")
    public boolean active = false;

    @Option("general.bot_profile")
    @Dropdown(options = ConfigSupplier.class)
    public String BOT_PROFILE = ConfigManager.DEFAULT;

    @Option("general.condition")
    public Condition condition;

    @Option("profile_changer.npc_counter_condition")
    public NpcCounterCondition npcExtraCondition = new NpcCounterCondition();

    @Option("profile_changer.npc_counter_condition")
    public NpcCounterCondition npcExtraCondition2 = new NpcCounterCondition();

    @Option("profile_changer.resource_counter_condition")
    public ResourceCounterCondition resourceCounterCondition = new ResourceCounterCondition();
}