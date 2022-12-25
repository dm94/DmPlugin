package com.deeme.behaviours.profilechanger;

import com.deeme.types.gui.ConfigSupplier;
import com.github.manolo8.darkbot.config.ConfigManager;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.types.Condition;

@Configuration("profile_changer")
public class ProfileChangerConfig {
    @Option("general.enabled")
    public boolean active = false;

    @Option("general.next_check_time")
    @Number(max = 300, step = 1, min = 5)
    public int timeToCheck = 60;

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

    @Option("profile_changer.map_timmer_condition")
    public MapTimmerCondition mapTimerCondition = new MapTimmerCondition();

    @Option("profile_changer.time_condition")
    public TimeCondition timeCondition = new TimeCondition();

    @Option("profile_changer.deaths_condition")
    public DeathsCondition deathsCondition = new DeathsCondition();
}