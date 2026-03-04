package com.deeme.modules.sentinel;

import java.util.Objects;

/**
 * Core service implementing the Group Leader Map Tracker logic.
 *
 * This class contains decision logic and exposes a small integration contract
 * (MovementController and LeaderInfo) so existing Sentinel lifecycle code can
 * wire real DarkBot managers into it and call onTick/update periodically.
 *
 * Usage (example):
 *  - Instantiate with GroupLeaderMapTracker config.
 *  - On each sentinel periodic update, call onLeaderState(leaderInfo, controller).
 *
 * The service will:
 *  - If enabled and followLeaderMap is true: move to the leader's map when it changes.
 *  - If enabled and useGroupHealth is true: when group health drops below threshold, press
 *    health button or use ability according to config.
 */
public class GroupLeaderMapTrackerService {
    private final GroupLeaderMapTracker config;

    // last known leader map id to avoid repeating commands
    private Integer lastKnownLeaderMap = null;

    public GroupLeaderMapTrackerService(GroupLeaderMapTracker config) {
        this.config = Objects.requireNonNull(config);
    }

    /**
     * Called by the sentinel module (or wrapper) every tick/update with current leader info
     * and a MovementController implementation.
     *
     * @param leaderInfo object with leader data (may be null if no leader present)
     * @param controller movement/interaction adapter provided by the host module
     */
    public void onLeaderState(LeaderInfo leaderInfo, MovementController controller) {
        if (!config.enabled || controller == null) return;

        // Handle leader map following
        if (config.followLeaderMap && leaderInfo != null && leaderInfo.hasMap()) {
            int leaderMap = leaderInfo.getMapId();
            int currentMap = controller.getCurrentMapId();

            // only issue move command when leader map changes and it's different from current map
            if (lastKnownLeaderMap == null || lastKnownLeaderMap != leaderMap) {
                lastKnownLeaderMap = leaderMap;
                if (currentMap != leaderMap) {
                    controller.moveToMap(leaderMap, config.priorityAreas);
                }
            }
        }

        // Handle group health reaction
        if (config.useGroupHealth && leaderInfo != null && leaderInfo.hasGroupHealth()) {
            double groupHealth = leaderInfo.getGroupHealthPercent();
            if (groupHealth < config.groupHealthThresholdPercent) {
                // choose action: press health button or use ability
                if (config.pressHealthButton) {
                    controller.pressHealthButton();
                } else if (config.abilityKey != null && !config.abilityKey.isEmpty()) {
                    controller.useAbility(config.abilityKey);
                }
            }
        }
    }

    /**
     * A small abstraction representing the information we need from a leader.
     * The sentinel module or caller should provide a concrete implementation
     * that reads from DarkBot's group/party/leader structures.
     */
    public interface LeaderInfo {
        /**
         * Return true if we have a leader and map information is available
         */
        boolean hasMap();

        /**
         * Leader's current map id (as represented by the Star/Map id used by the bot)
         */
        int getMapId();

        /**
         * Whether group health info is present
         */
        boolean hasGroupHealth();

        /**
         * Group health (0.0 - 1.0)
         */
        double getGroupHealthPercent();
    }

    /**
     * Movement/interaction contract used by the service so it doesn't depend on
     * specific DarkBot API types. Host modules (Sentinel) should implement this
     * adapter using the bot's actual movement/GUI APIs.
     */
    public interface MovementController {
        /**
         * Current map id of the bot
         */
        int getCurrentMapId();

        /**
         * Command the bot to travel to the provided map id. priorityAreas is a
         * comma-separated string (from config) that the implementation may use
         * to prioritise travel (optional - may be ignored by simple implementations).
         */
        void moveToMap(int mapId, String priorityAreas);

        /**
         * Press the 'health' button in UI (if available) to attempt heal.
         */
        void pressHealthButton();

        /**
         * Use an ability identified by key (as provided by config).
         */
        void useAbility(String abilityKey);
    }
}
