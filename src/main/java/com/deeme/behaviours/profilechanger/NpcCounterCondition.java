
package com.deeme.behaviours.profilechanger;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;

@Configuration("profile_changer.npc_counter_condition")
public class NpcCounterCondition {

    public transient int lastNPCId = 0;
    public transient double lastExperiencieCheck = 0;
    public transient boolean isAttacked = false;

    @Option("general.enabled")
    public boolean active = false;

    @Option("profile_changer.npc_counter_condition.npc_name")
    public String npcName = "";

    @Option("profile_changer.npc_counter_condition.npc_to_kill")
    @Number(min = 0, max = 100000, step = 1)
    public int npcsToKill = 1;

    @Option("profile_changer.npc_counter_condition.npcs_killed")
    @Number(min = 0, max = 1000000, step = 1)
    public int npcCounter = 0;
}
