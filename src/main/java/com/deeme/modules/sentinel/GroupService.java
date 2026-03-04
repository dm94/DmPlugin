package com.deeme.modules.sentinel;

/**
 * Abstraction to obtain group-related runtime information. Implement this using the real
 * bot/game APIs to provide leader map id and current group health.
 */
public interface GroupService {
    /**
     * Returns the map id where the group leader currently is, or null if unknown.
     */
    Integer getLeaderMapId();

    /**
     * Returns the group's overall health as a value between 0.0 and 1.0.
     */
    double getGroupHealthPercent();
}
