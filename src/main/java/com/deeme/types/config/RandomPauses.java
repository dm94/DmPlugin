package com.deeme.types.config;

import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;


@Option("Random Pauses")
public class RandomPauses {

    @Option(value = "Min interval for each pause", description = "Minimum time that must pass for a pause in minutes")
    @Num(min = 20)
    public int minInterval = 60;

    @Option(value = "Max interval for each pause", description = "Maximum time that must pass for a pause in minutes")
    @Num(min = 20)
    public int maxInterval = 90;

    @Option(value = "Min Pause", description = "Minimum time for random pause in minutes")
    @Num(min = 5)
    public int minPause = 5;

    @Option(value = "Max Pause", description = "Maximum time for random pause in minutes")
    @Num(min = 10)
    public int maxPause = 15;
}
