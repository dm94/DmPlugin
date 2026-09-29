package com.deeme.shared;

import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.managers.HeroAPI;

/**
 * Keeps track of whether the current target is still engaged in combat.
 *
 * The time out counter only grows while the target's health is frozen:
 * damage landing (HP down) or the enemy repairing (HP up) both mean the
 * fight is still alive, so the target must not be dropped.
 */
public class TargetEngagementTracker {

    private static final long CHECK_INTERVAL_MS = 1000;
    private static final int HP_DECREASE_WINDOW_MS = 1000;

    private final HeroAPI hero;

    private long nextCheck = 0;
    private int seconds = 0;

    private int lastTargetId = 0;
    private double lastTargetHp = -1;

    public TargetEngagementTracker(HeroAPI hero) {
        this.hero = hero;
    }

    /**
     * Checks the engagement at most once per second.
     *
     * @return true when the target must be dropped: maxSeconds elapsed with
     *         no damage dealt and no HP movement at all. Internal counters
     *         are reset automatically when it expires.
     */
    public boolean isTimeOut(Ship target, int maxSeconds, int maxRange) {
        if (nextCheck < System.currentTimeMillis()) {
            nextCheck = System.currentTimeMillis() + CHECK_INTERVAL_MS;
            if (isTargetEngaged(target, maxRange)) {
                seconds = 0;
            } else {
                seconds++;
            }
        }

        if (seconds < maxSeconds) {
            return false;
        }

        reset();
        return true;
    }

    private boolean isTargetEngaged(Ship target, int maxRange) {
        if (target == null) {
            return false;
        }

        if (target.getId() != lastTargetId) {
            lastTargetId = target.getId();
            lastTargetHp = target.getHealth().getHp();
            seconds = 0;
            return false;
        }

        if (target.getHealth() == null) {
            return hero.isAttacking(target);
        }

        if (hero.isAttacking(target) && target.getHealth().hpDecreasedIn(HP_DECREASE_WINDOW_MS)
                && target.getLocationInfo().distanceTo(hero) < maxRange) {
            lastTargetHp = target.getHealth().getHp();
            return true;
        }

        double currentHp = target.getHealth().getHp();
        boolean hpLowest = lastTargetHp >= 0 && currentHp < lastTargetHp;
        lastTargetHp = currentHp;

        return hpLowest;
    }

    public void reset() {
        nextCheck = 0;
        seconds = 0;
        lastTargetId = 0;
        lastTargetHp = -1;
    }

    public String getStatus(int maxSeconds) {
        return seconds + "/" + maxSeconds;
    }
}
