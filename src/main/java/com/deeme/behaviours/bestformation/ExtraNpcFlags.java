package com.deeme.behaviours.bestformation;

import com.github.manolo8.darkbot.config.NpcExtraFlag;

public enum ExtraNpcFlags implements NpcExtraFlag {
    USE_VETERAN("FVT", "Use veteran formation", "Auto Best Formation - Will use veteran foramtion");

    private final String shortName;
    private final String name;
    private final String description;

    ExtraNpcFlags(String shortName, String name, String description) {
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