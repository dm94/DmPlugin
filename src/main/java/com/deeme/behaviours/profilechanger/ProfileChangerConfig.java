package com.deeme.behaviours.profilechanger;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.shared.config.ProfileNames;
import eu.darkbot.api.config.annotations.Number;

@Configuration("profile_changer")
public class ProfileChangerConfig {
    @Option("general.enabled")
    public boolean active = false;

    @Option("general.next_check_time")
    @Number(max = 300, step = 1, min = 1)
    public int timeToCheck = 60;

    @Option("general.bot_profile")
    @Dropdown(options = ProfileNames.class)
    public String BOT_PROFILE = "";

    @Option("profile_changer.close_bot")
    public boolean closeBot = false;

    @Option("profile_changer.reload_bot")
    public boolean reloadBot = false;

    @Option("profile_changer.only_one_condition")
    public boolean orConditional = false;

    @Option("general.tick_stopped")
    public boolean tickStopped = false;

    @Option("general.condition")
    public NormalCondition normalCondition = new NormalCondition();

    @Option("general.condition")
    public NormalCondition normalCondition2 = new NormalCondition();

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