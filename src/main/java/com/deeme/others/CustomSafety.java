package com.deeme.others;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.legacy.Config;
import eu.darkbot.api.config.types.PlayerInfo;
import eu.darkbot.api.config.types.PlayerTag;
import eu.darkbot.api.config.types.SafetyInfo;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.other.Location;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.utils.PortalJumper;
import eu.darkbot.util.TimeUtils;
import eu.darkbot.util.Timer;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.github.manolo8.darkbot.config.SafetyInfo.JumpMode;

/* 
 * Custom Implementation based in:
 * https://github.com/darkbot-reloaded/DarkBotAPI/blob/master/shared/src/main/java/eu/darkbot/shared/utils/SafetyFinder.java
*/

public class CustomSafety {
    private final HeroAPI hero;
    private final MovementAPI movement;

    private final ConfigAPI config;
    private final Config legacyConfig;
    private final ConfigSetting<PlayerTag> enemiesTag;
    private final ConfigSetting<Integer> rememberEnemySeconds;

    private final Collection<? extends Ship> ships;

    private final PortalJumper jumper;
    private final Timer lastMoveTimer = Timer.getRandom(3 * TimeUtils.SECOND, TimeUtils.SECOND);

    public CustomSafety(PluginAPI api) {
        this(api.requireAPI(HeroAPI.class), api.requireAPI(MovementAPI.class), api.requireAPI(ConfigAPI.class),
                api.requireAPI(EntitiesAPI.class), new PortalJumper(api));
    }

    @Inject
    public CustomSafety(HeroAPI hero,
            MovementAPI movement,
            ConfigAPI config,
            EntitiesAPI entities,
            PortalJumper portalJumper) {
        this.hero = hero;
        this.movement = movement;

        this.config = config;
        this.legacyConfig = config.getLegacy();
        this.enemiesTag = config.requireConfig("general.running.enemies_tag");
        this.rememberEnemySeconds = config.requireConfig("general.running.remember_enemies_for");

        this.ships = entities.getShips();

        this.jumper = portalJumper;
    }

    public void escapeTick() {
        escape();
    }

    private void escape() {
        SafetyInfo safety = getSafety();
        if (safety == null) {
            return;
        }

        if (hero.getLocationInfo().distanceTo(safety) > safety.getRadius()) {
            moveToSafety(safety);
        } else if (hero.getHealth().hpDecreasedIn(200)) {
            if (safety.getType() != SafetyInfo.Type.PORTAL) {
                return;
            }
            SafetyInfo.JumpMode jm = safety.getJumpMode();
            if (jm != null && jm.ordinal() > JumpMode.FLEEING.ordinal()) {
                safety.getEntity()
                        .ifPresent(e -> jumper.travelAndJump((Portal) e));
            }
        }
    }

    private SafetyInfo getSafety() {
        List<SafetyInfo> safeties = config.getLegacy()
                .getSafeties(hero.getMap())
                .stream()
                .filter(s -> s.getEntity().map(Entity::isValid).orElse(false))
                .peek(s -> s.setDistance(Math.max(0, movement.getDistanceBetween(hero, s) - s.getRadius())))
                .sorted(Comparator.comparingDouble(SafetyInfo::getDistance))
                .collect(Collectors.toList());
        if (safeties.isEmpty()) {
            return null;
        }

        List<Ship> enemies = ships.stream().filter(this::runFrom).collect(Collectors.toList());

        return safeties.stream()
                .filter(s -> s.getDistance() < enemies.stream()
                        .mapToDouble(enemy -> movement.getDistanceBetween(enemy, s))
                        .min().orElse(Double.POSITIVE_INFINITY))
                .findFirst()
                .orElse(safeties.get(0));
    }

    private void moveToSafety(SafetyInfo safety) {
        if (safety == null || !safety.getEntity().map(Entity::isValid).orElse(false)
                || (movement.getDestination().distanceTo(safety) < safety.getRadius() && lastMoveTimer.isActive()))
            return;

        hero.setRunMode();

        double angle = safety.angleTo(hero) + Math.random() * 0.2 - 0.1;
        movement.moveTo(Location.of(safety, angle, -safety.getRadius() * (0.3 + (0.60 * Math.random())))); // 30%-90%
                                                                                                           // radius
        lastMoveTimer.activate();
    }

    private boolean runFrom(Ship ship) {
        if (enemiesTag.getValue() != null) {
            PlayerInfo playerInfo = legacyConfig.getPlayerInfos().get(ship.getId());
            if (playerInfo != null && enemiesTag.getValue().hasTag(playerInfo))
                return true;
        }

        return ship.getEntityInfo().isEnemy() && isAttackingOrBlacklisted(ship);
    }

    private boolean isAttackingOrBlacklisted(Ship ship) {
        if (ship.isAttacking(hero)) {
            ship.setBlacklisted(rememberEnemySeconds.getValue() * 1000L);
        }

        return ship.isBlacklisted();
    }

}
