package com.deeme.shared.movement;

import eu.darkbot.api.config.annotations.Configuration;

@Configuration("defense.movement_mode.list")
public enum MovementModeEnum {
    VS, VSSAFETY, RANDOM, GROUPVSSAFETY, GROUPVS;
}
