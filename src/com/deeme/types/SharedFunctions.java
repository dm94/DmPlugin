package com.deeme.types;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.entities.Ship;
import com.github.manolo8.darkbot.core.manager.HeroManager;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SharedFunctions {

    public static Ship getAttacker(Ship assaulted, Main main, HeroManager hero, Ship target) {
        if (target != null && !target.removed &&
                target.playerInfo.isEnemy()) {
            return target;
        }

        List<Ship> ships = main.mapManager.entities.ships.stream()
                .filter(s -> s.playerInfo.isEnemy())
                .filter(s -> s.isAttacking(assaulted))
                .filter(s -> !isPet(s.playerInfo.username))
                .sorted(Comparator.comparingDouble(s -> s.locationInfo.distance(hero)))
                .collect(Collectors.toList());

        if (ships.isEmpty()) return null;

        return ships.get(0);
    }

    private static boolean isPet(String name) {
        return name.matches(".*?(\\s)(\\[(\\d+)\\])");
    }

}
