package com.deeme.shared.movement;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;

@Configuration("movement_config")
public class MovementConfig {
    @Option("movement_config.default")
    @Dropdown
    public MovementModeEnum defaultMovementMode = MovementModeEnum.VS;

    @Option("movement_config.condition")
    public MovementWithConditions movementCondtion1 = new MovementWithConditions();

    @Option("movement_config.condition")
    public MovementWithConditions movementCondtion2 = new MovementWithConditions();

    @Option("movement_config.condition")
    public MovementWithConditions movementCondtion3 = new MovementWithConditions();

    @Option("movement_config.condition")
    public MovementWithConditions movementCondtion4 = new MovementWithConditions();

    @Option("movement_config.condition")
    public MovementWithConditions movementCondtion5 = new MovementWithConditions();
}
