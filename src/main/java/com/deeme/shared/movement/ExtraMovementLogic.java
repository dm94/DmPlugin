package com.deeme.shared.movement;

import java.util.Comparator;
import java.util.Optional;
import java.util.Random;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.types.Condition;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.group.GroupMember;
import eu.darkbot.api.game.other.Location;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.GroupAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.utils.SafetyFinder;

public class ExtraMovementLogic {
    private final PluginAPI api;
    private final HeroAPI hero;
    private final MovementAPI movement;
    private final SafetyFinder safetyFinder;
    private final GroupAPI group;

    private static final int DISTANCE_TO_USE_VS_MODE = 1500;

    private MovementConfig config;
    private Random rnd;

    public ExtraMovementLogic(PluginAPI api, MovementConfig config) {
        this(api, api.requireAPI(HeroAPI.class), api.requireAPI(MovementAPI.class), config);
    }

    @Inject
    public ExtraMovementLogic(PluginAPI api, HeroAPI hero, MovementAPI movement, MovementConfig config) {
        this.api = api;
        this.hero = hero;
        this.movement = movement;
        this.group = api.getAPI(GroupAPI.class);
        this.safetyFinder = api.requireInstance(SafetyFinder.class);
        this.config = config;
        this.rnd = new Random();
    }

    public void tick() {
        move();
    }

    private void move() {
        MovementModeEnum movementSelected = getMovementMode();

        switch (movementSelected) {
            case VS:
                vsMove();
                break;
            case RANDOM:
                handleRandom();
                break;
            case GROUPVSSAFETY:
                handleGroupVSSafety();
                break;
            case GROUPVS:
                handleGroupVS();
                break;
            case VSSAFETY:
            default:
                handleVSSafety();
                break;
        }
    }

    private void handleVSSafety() {
        if (!safetyFinder.tick()) {
            vsMove();
        }
    }

    private void handleRandom() {
        if (!movement.isMoving() || movement.isOutOfMap()) {
            movement.moveRandom();
        }
    }

    private void handleGroupVSSafety() {
        if (safetyFinder.tick()) {
            handleGroupMovement();
        }
    }

    private void handleGroupVS() {
        handleGroupMovement();
    }

    private void handleGroupMovement() {
        GroupMember groupMember = getClosestMember();
        if (groupMember != null) {
            if (groupMember.getLocation().distanceTo(hero) < DISTANCE_TO_USE_VS_MODE) {
                vsMove();
            } else {
                movement.moveTo(groupMember.getLocation());
            }
        } else {
            vsMove();
        }
    }

    private MovementModeEnum getMovementMode() {
        if (checkCondition(config.movementCondtion1.condition)) {
            return config.movementCondtion1.movementMode;
        }
        if (checkCondition(config.movementCondtion2.condition)) {
            return config.movementCondtion2.movementMode;
        }
        if (checkCondition(config.movementCondtion3.condition)) {
            return config.movementCondtion3.movementMode;
        }
        if (checkCondition(config.movementCondtion4.condition)) {
            return config.movementCondtion4.movementMode;
        }
        if (checkCondition(config.movementCondtion5.condition)) {
            return config.movementCondtion5.movementMode;
        }

        return config.defaultMovementMode;
    }

    private boolean checkCondition(Condition condition) {
        if (condition == null) {
            return false;
        }

        return condition.get(api).allows();
    }

    private Optional<Location> getDestination() {
        Ship target = hero.getTargetAs(Ship.class);
        if (target != null && target.isValid()) {
            return Optional.of(target.getDestination().orElse(target.getLocationInfo().destinationInTime(500)));
        } else {
            Entity other = hero.getTarget();
            if (other != null && other.isValid()) {
                return Optional.of(other.getLocationInfo());
            }
        }

        Lockable t = hero.getLocalTarget();
        if (t != null && t.isValid()) {
            return Optional.of(t.getLocationInfo());
        }

        return Optional.empty();
    }

    private void vsMove() {
        Location targetLoc = getDestination().orElse(null);

        if (targetLoc == null) {
            return;
        }

        double distance = hero.getLocationInfo().distanceTo(targetLoc);
        if (distance > 600) {
            if (movement.canMove(targetLoc)) {
                movement.moveTo(targetLoc);
            }
        } else if (!movement.isMoving()) {
            movement.moveTo(Location.of(targetLoc, rnd.nextInt(360), distance));
        }
    }

    private GroupMember getClosestMember() {
        if (group.hasGroup()) {
            return group.getMembers().stream()
                    .filter(member -> !member.isDead() && member.getMapId() == hero.getMap().getId())
                    .min(Comparator.<GroupMember>comparingDouble(m -> m.getLocation().distanceTo(hero)))
                    .orElse(null);
        }
        return null;
    }
}
