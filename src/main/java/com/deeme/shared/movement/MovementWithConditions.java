package com.deeme.shared.movement;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.types.Condition;

@Configuration("movement_conditions")
public class MovementWithConditions {
    @Option("defense.movement_mode")
    @Dropdown
    public MovementModeEnum movementMode = MovementModeEnum.VSSAFETY;

    @Option("general.condition")
    public Condition condition;
}
