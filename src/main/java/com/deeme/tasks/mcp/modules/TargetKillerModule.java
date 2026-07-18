package com.deeme.tasks.mcp.modules;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.NpcFlag;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Pet;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.game.other.Locatable;
import eu.darkbot.api.game.other.Location;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.AttackAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.shared.modules.TemporalModule;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Temporal module that hunts a single entity (NPC, player or pet) selected by
 * id
 * via MCP. Reuses the same attack + circling movement logic as SentinelModule
 * and AstralGate: {@link AttackAPI} for lock/attack, radius-based circling for
 * evasion. Returns control to the previous module once the target dies, the
 * timeout elapses, or the target stays out of sight too long.
 *
 * <p>
 * Installed with {@code bot.setModule(new TargetKillerModule(...))}; the
 * previous non-temporal module is captured automatically by
 * {@link TemporalModule#install} and restored by {@link #goBack()}.
 * </p>
 */
public class TargetKillerModule extends TemporalModule {

    private static final double SPEED_MULTIPLIER = 0.625;
    private static final double ANGLE_ADJUSTMENT = 0.3;
    private static final double DISTANCE_INCREMENT = 2.0;
    private static final double MAX_DISTANCE_LIMIT = 10000;
    private static final double DEFAULT_RADIUS = 550.0;
    private static final long MISSING_GRACE_MS = 15_000L;

    private final HeroAPI hero;
    private final AttackAPI attack;
    private final EntitiesAPI entities;
    private final MovementAPI movement;
    private final ConfigSetting<Integer> maxCircleIterations;

    private final int targetId;
    private final long deadlineMs;
    private final Random rnd = new Random();

    private volatile boolean done;
    private boolean backwards;

    private Lockable target;
    private long lastSeenMs;

    public TargetKillerModule(PluginAPI api, BotAPI bot, int targetId, long timeoutSeconds) {
        super(bot);
        this.hero = api.requireAPI(HeroAPI.class);
        this.attack = api.requireAPI(AttackAPI.class);
        this.entities = api.requireAPI(EntitiesAPI.class);
        this.movement = api.requireAPI(MovementAPI.class);
        ConfigAPI configApi = api.requireAPI(ConfigAPI.class);
        this.maxCircleIterations = configApi.requireConfig("loot.max_circle_iterations");
        this.targetId = targetId;
        this.deadlineMs = timeoutSeconds > 0L
                ? System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds)
                : 0L;
    }

    @Override
    public String getStatus() {
        return done ? "MCP Hunter | returning" : "MCP Hunter | target " + targetId;
    }

    @Override
    public String getStoppedStatus() {
        return getStatus();
    }

    @Override
    public void onTickModule() {
        if (done) {
            return;
        }
        long now = System.currentTimeMillis();
        if (deadlineMs > 0L && now > deadlineMs) {
            finish();
            return;
        }

        Lockable current = resolveTarget();
        if (current == null) {
            if (target != null) {
                movement.moveTo(target.getLocationInfo());
            }
            if (lastSeenMs != 0L && now - lastSeenMs > MISSING_GRACE_MS) {
                finish();
            }
            return;
        }

        lastSeenMs = now;
        hero.setLocalTarget(current);
        attack.setTarget(current);
        attack.tryLockAndAttack();
        circleTarget();
    }

    private void circleTarget() {
        if (!attack.hasTarget()) {
            return;
        }
        Lockable tgt = attack.getTarget();
        if (tgt == null) {
            return;
        }

        Location direction = movement.getDestination();
        Location targetLoc = tgt.getLocationInfo().destinationInTime(400);

        double distance = hero.distanceTo(tgt);
        double angle = targetLoc.angleTo(hero);
        double radius = getRadius(tgt);
        double speed = tgt instanceof Movable ? ((Movable) tgt).getSpeed() : 0;
        boolean noCircle = attack.hasExtraFlag(NpcFlag.NO_CIRCLE);

        double angleDiff;
        if (noCircle) {
            double dist = targetLoc.distanceTo(direction);
            double minRad = Math.max(0, Math.min(radius - 200, radius * 0.5));
            if (dist <= radius && dist >= minRad) {
                return;
            }
            distance = minRad + Math.random() * (radius - minRad - 10);
            angleDiff = (Math.random() * 0.1) - 0.05;
        } else {
            double maxRadFix = radius / 2;
            double radiusFix = (int) Math.max(Math.min(radius - distance, maxRadFix), -maxRadFix);
            distance = (radius += radiusFix);
            angleDiff = Math.max((hero.getSpeed() * SPEED_MULTIPLIER)
                    + (Math.max(200, speed) * SPEED_MULTIPLIER)
                    - hero.distanceTo(Location.of(targetLoc, angle, radius)), 0) / radius;
        }
        direction = getBestDir(targetLoc, angle, angleDiff, distance);

        while (!movement.canMove(direction) && distance < MAX_DISTANCE_LIMIT) {
            direction.toAngle(targetLoc, angle += backwards ? -ANGLE_ADJUSTMENT : ANGLE_ADJUSTMENT,
                    distance += DISTANCE_INCREMENT);
        }
        if (distance >= MAX_DISTANCE_LIMIT) {
            direction.toAngle(targetLoc, angle, 500);
        }

        if (movement.canMove(direction)) {
            movement.moveTo(direction);
        }
    }

    private Location getBestDir(Locatable targetLoc, double angle, double angleDiff, double distance) {
        int maxIterations = maxCircleIterations.getValue();
        int iteration = 1;
        double forwardScore = 0;
        double backScore = 0;
        do {
            forwardScore += score(Locatable.of(targetLoc, angle + (angleDiff * iteration), distance));
            backScore += score(Locatable.of(targetLoc, angle - (angleDiff * iteration), distance));
            if (forwardScore < 0 != backScore < 0 || Math.abs(forwardScore - backScore) > 300) {
                break;
            }
        } while (iteration++ < maxIterations);

        if (iteration <= maxIterations) {
            backwards = backScore > forwardScore;
        }
        return Location.of(targetLoc, angle + angleDiff * (backwards ? -1 : 1), distance);
    }

    private double score(Locatable loc) {
        double blocked = movement.canMove(loc) ? 0 : -1000;
        double nearbyNpcs = 0;
        for (Npc n : entities.getNpcs()) {
            if (attack.getTarget() != n) {
                double nRadius = n.getInfo().getRadius();
                nearbyNpcs += Math.max(0, nRadius - n.distanceTo(loc));
            }
        }
        return blocked - nearbyNpcs;
    }

    private double getRadius(Lockable target) {
        if (!(target instanceof Npc)) {
            return DEFAULT_RADIUS + rnd.nextInt(60);
        }
        return attack.modifyRadius(((Npc) target).getInfo().getRadius());
    }

    private Lockable resolveTarget() {
        if (target != null && target.isValid()) {
            return target;
        }
        Lockable fresh = findTarget();
        if (fresh != null) {
            target = fresh;
            return fresh;
        }
        return null;
    }

    public void stop() {
        finish();
    }

    private void finish() {
        if (done) {
            return;
        }
        done = true;
        attack.setTarget(null);
        target = null;
        goBack();
    }

    private Lockable findTarget() {
        for (Npc n : entities.getNpcs()) {
            if (n.getId() == targetId) {
                return n;
            }
        }
        for (Player p : entities.getPlayers()) {
            if (p.getId() == targetId) {
                return p;
            }
        }
        for (Pet p : entities.getPets()) {
            if (p.getId() == targetId) {
                return p;
            }
        }
        return null;
    }
}
