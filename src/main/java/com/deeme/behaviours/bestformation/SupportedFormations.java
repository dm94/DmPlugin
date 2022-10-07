package com.deeme.behaviours.bestformation;

import eu.darkbot.api.config.annotations.Configuration;

@Configuration("supported_formations")
public enum SupportedFormations {
    STAR("f-04-st"),
    PINCER("f-05-pi"),
    DOUBLE_ARROW("f-06-da"),
    DIAMOND("f-07-di"),
    CHEVRON("f-08-ch"),
    MOTH("f-09-mo"),
    CRAB("f-10-cr"),
    BARRAGE("f-12-ba"),
    BAT("f-13-bt"),
    DRILL("f-3d-dr"),
    VETERAN("f-3d-vt"),
    WHEEL("f-3d-wl");

    private final String id;

    SupportedFormations(String id) {
        this.id = "drone_formation_" + id;
    }

    public String getId() {
        return id;
    }
}