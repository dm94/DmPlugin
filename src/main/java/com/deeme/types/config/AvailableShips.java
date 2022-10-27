package com.deeme.types.config;

import eu.darkbot.api.config.annotations.Configuration;

@Configuration("ambulance.ship_type.list")
public enum AvailableShips {
    AEGIS, HAMMERCLAW, SOLACE;
}