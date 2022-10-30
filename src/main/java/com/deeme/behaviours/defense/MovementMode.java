package com.deeme.behaviours.defense;

import eu.darkbot.api.config.annotations.Configuration;

@Configuration("defense.movement_mode.list")
public enum MovementMode {
    VS, VSSAFETY, RANDOM, GROUPVSSAFETY, GROUPVS;
}
