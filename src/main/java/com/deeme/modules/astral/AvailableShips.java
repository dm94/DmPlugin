package com.deeme.modules.astral;

import eu.darkbot.api.config.annotations.Configuration;

@Configuration("astral.ship_type.list")

public enum AvailableShips {
    ZEPHYR("zephyr"),
    DIMINISHER("diminisher"),
    SENTINEL("sentinel"),
    PUSAT("pusat");

    private final String id;

    AvailableShips(String id) {
        this.id = "ship_" + id.toLowerCase();
    }

    public String getId() {
        return id;
    }
}