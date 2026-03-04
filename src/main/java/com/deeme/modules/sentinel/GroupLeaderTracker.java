package com.deeme.modules.sentinel;

import com.deeme.modules.genericgate.TravelMap;

/**
 * Tracker that, when enabled, will cause the bot to travel to the group leader's map and react
 * to group health changes (e.g. press heal). This class contains the minimal logic and
 * integration points; real integration requires implementing GroupService and TravelController
 * with the actual bot/game APIs and wiring this tracker into the Sentinel module tick loop.
 */
public class GroupLeaderTracker {
    private final TravelMap config;
    private final GroupService groupService;
    private final TravelController travelController;
    private final double healthThreshold; // 0.0 - 1.0

    public GroupLeaderTracker(TravelMap config,
                              GroupService groupService,
                              TravelController travelController,
                              double healthThreshold) {
        this.config = config;
        this.groupService = groupService;
        this.travelController = travelController;
        this.healthThreshold = healthThreshold;
    }

    /**
     * Perform a single tick: possibly travel to leader's map and react to low group health.
     * This should be called periodically by the Sentinel module main loop.
     */
    public void tick() {
        if (config == null || !config.active) return;

        // Follow leader map if enabled
        if (config.followLeader) {
            Integer leaderMap = groupService.getLeaderMapId();
            if (leaderMap != null && leaderMap != travelController.getCurrentMapId()) {
                travelController.travelToMap(leaderMap);
                // After traveling we skip health reaction this tick to avoid double-actions.
                return;
            }
        }

        // React to group health
        double groupHealth = groupService.getGroupHealthPercent();
        if (groupHealth < healthThreshold) {
            travelController.pressHeal();
        }
    }
}
