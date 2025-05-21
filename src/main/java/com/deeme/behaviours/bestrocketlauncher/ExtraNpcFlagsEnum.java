package com.deeme.behaviours.bestrocketlauncher;

import com.github.manolo8.darkbot.config.NpcExtraFlag;

public enum ExtraNpcFlagsEnum implements NpcExtraFlag {
    BEST_AMMO("ABRL", "Auto Best Rocket Launcher", "Auto choose the best rocket launcher");

    private final String shortName;
    private final String name;
    private final String description;

    ExtraNpcFlagsEnum(String shortName, String name, String description) {
        this.shortName = shortName;
        this.name = name;
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getShortName() {
        return shortName;
    }
}
