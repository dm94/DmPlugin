package com.deeme.modules.pvp;

import java.util.ArrayList;
import java.util.Collections;

import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.StatsAPI;

public class AntiPushLogic {
    private AntiPush config;
    private final StatsAPI stats;
    private final HeroAPI heroapi;

    public AntiPushLogic(HeroAPI heroapi, StatsAPI stats, AntiPush config) {
        this.heroapi = heroapi;
        this.stats = stats;
        this.config = config;
    }

    public void registerTarget(Ship target) {
        if (!config.enable) {
            return;
        }

        if (target != null && target.isValid() && heroapi.isAttacking()) {
            if (target.getId() != config.lastPlayerId) {
                if (config.lastPlayerId != 0
                        && config.lastExperiencieCheck < stats.getTotalExperience()) {
                    config.playersKilled.add(config.lastPlayerId);
                    config.lastPlayerId = 0;
                } else {
                    config.lastPlayerId = target.getId();
                    config.lastExperiencieCheck = stats.getTotalExperience();
                }
            }
        } else if (config.lastPlayerId != 0
                && config.lastExperiencieCheck < stats.getTotalExperience()) {
            config.playersKilled.add(config.lastPlayerId);
            config.lastPlayerId = 0;
        }
    }

    public ArrayList<Integer> getIgnoredPlayers() {
        ArrayList<Integer> playersToIgnore = new ArrayList<>();

        if (config.enable) {
            config.playersKilled.forEach(id -> {
                if (!playersToIgnore.contains(id)
                        && Collections.frequency(config.playersKilled,
                                id) >= config.maxKills) {
                    playersToIgnore.add(id);
                }
            });
        }

        return playersToIgnore;
    }
}
