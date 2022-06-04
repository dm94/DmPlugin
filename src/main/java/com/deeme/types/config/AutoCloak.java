package com.deeme.types.config;

import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;

@Option(value = "Auto Cloak", description = "It will automatically camouflage")
public class AutoCloak {
    @Option(value = "Auto Cloak")
    public boolean autoCloakShip = false;

    @Option(value = "Minimum time to cloak after attacking", description = "It will not be cloaked until those seconds have elapsed.")
    @Num(min = 0, max = 1000, step = 1)
    public int secondsOfWaiting = 10;
}