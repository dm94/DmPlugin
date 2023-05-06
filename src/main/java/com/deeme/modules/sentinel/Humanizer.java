package com.deeme.modules.sentinel;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

@Configuration("humanizer")
public class Humanizer {
    @Option("humanizer.random_time")
    public boolean addRandomTime = false;

    @Option("humanizer.max_random_time")
    @Number(min = 0, max = 1000, step = 1)
    public int maxRandomTime = 3;
}
