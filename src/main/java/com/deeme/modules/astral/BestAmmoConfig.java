package com.deeme.modules.astral;

import eu.darkbot.api.config.annotations.Configuration;

@Configuration("astral.best_ammo.list")
public enum BestAmmoConfig {
    ALWAYS, SPECIAL_LOGIC, ONLY_MARKED;
}