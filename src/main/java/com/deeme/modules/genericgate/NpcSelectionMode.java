package com.deeme.modules.genericgate;

import eu.darkbot.api.config.annotations.Configuration;

@Configuration("generic_gate.npc_selection_mode.list")
public enum NpcSelectionMode {
    DEFAULT,
    ALWAYS_CLOSEST,
    NPC_PRIORITY
}
