package com.deeme.types.config;

import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.config.types.Options;
import com.github.manolo8.darkbot.gui.tree.components.JListField;
import com.github.manolo8.darkbot.gui.tree.components.JPercentField;

public class AmbulanceConfig {
    @Option(value = "Enable")
    public boolean enable = false;

    @Option(value = "Type of ship", description = "Also works with the plus versions")
    @Editor(JListField.class)
    @Options(AvailableShips.class)
    public int shipType = 0;

    @Option(value = "Time to check, seconds", description = "Min time per check")
    @Num(min = 1, max = 300, step = 1)
    public int timeToCheck = 20;

    @Option(value = "Max health", description = "It is Only activated if health lower than this")
    @Editor(JPercentField.class)
    public double healthToRepair = 0.5;

    @Option(value = "Repair Shield")
    public boolean repairShield = true;
}
