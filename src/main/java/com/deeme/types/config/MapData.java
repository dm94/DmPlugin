package com.deeme.types.config;

import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;

@Option("Map")
public class MapData {

    @Option(value = "Time", description = "In minutes")
    @Num(max = 600, step = 5)
    public int time = 0;

    @Option(value = "Deaths", description = "If exceed them, change of map")
    @Num(max = 999, step = 1)
    public int deaths = 0;

    @Override
    public String toString() {
        return (time != 0 ? "T: " + time + " | " : "") + (deaths != 0 ? "D: " + deaths : "");
    }
}