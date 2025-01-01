package com.deeme.modules.genericgate;

import com.github.manolo8.darkbot.config.NpcExtraFlag;

public enum ExtraNpcFlagsEnum implements NpcExtraFlag {
    MOVE_AWAY("AWAY", "Move Away", "Generic Gate - Will try to dodge this NPC");

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