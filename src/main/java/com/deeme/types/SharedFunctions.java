package com.deeme.types;

import com.github.manolo8.darkbot.Main;

import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.HeroAPI;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SharedFunctions {

    public static Ship getAttacker(Ship assaulted, Main main) {
        HeroAPI hero = main.pluginAPI.getAPI(HeroAPI.class);
        EntitiesAPI entities = main.pluginAPI.getAPI(EntitiesAPI.class);
        return getAttacker(assaulted, entities.getShips(), hero);
    }

    public static Ship getAttacker(Ship assaulted, Collection<? extends Ship> allShips, HeroAPI hero) {
        List<eu.darkbot.api.game.entities.Ship> ships = allShips.stream()
                .filter(s -> s.getEntityInfo().isEnemy())
                .filter(s -> s.isAttacking(assaulted))
                .filter(s -> !isPet(s.getEntityInfo().getUsername()))
                .sorted(Comparator.comparingDouble(s -> s.getLocationInfo().distanceTo(hero)))
                .collect(Collectors.toList());

        if (ships.isEmpty()) return null;

        Ship ship = (Ship) ships.get(0);

        return ship;
    }

    public static boolean hasAttacker(Ship assaulted, Main main) {
        Ship ship = getAttacker(assaulted, main);
        return ship != null;
    }

    public static boolean isPet(String name) {
        return name.matches(".*?(\\s)(\\[(\\d+)\\])");
    }

}
