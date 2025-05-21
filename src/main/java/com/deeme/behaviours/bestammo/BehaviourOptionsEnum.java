package com.deeme.behaviours.bestammo;

import eu.darkbot.api.config.annotations.Configuration;

@Configuration("general.options")
public enum BehaviourOptionsEnum {
    VS_PLAYERS, TICK_STOPPED, ONLY_PROMETHEUS, ALWAYS_FOR_NPC, RESPECT_NPC_AMMO, RESPECT_RSB_TAG, REPLACE_AMMO_KEY,
    CHANGE_AMMO_DIRECTLY
}
