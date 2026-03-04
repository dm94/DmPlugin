package com.deeme.modules.sentinel;

import eu.darkbot.api.PluginAPI;

/**
 * Helper/hook class that sentinel module authors can instantiate and call from
 * their lifecycle. This class keeps a reference to the service and exposes a
 * small lifecycle-friendly API: start/stop (no automatic threads spawned) and
 * onTick/handleLeaderState which should be called periodically by the module.
 *
 * The idea is to avoid hard dependencies on specific DarkBot managers inside
 * the service while still providing an easy integration point for Sentinel.
 */
public class SentinelGroupLeaderHook {
    private final GroupLeaderMapTrackerService service;
    private final PluginAPI api; // kept for potential future use/extension

    public SentinelGroupLeaderHook(PluginAPI api, GroupLeaderMapTracker config) {
        this.api = api;
        this.service = new GroupLeaderMapTrackerService(config);
    }

    /**
     * Called by the host module on each periodic update (e.g. tick).
     * The host is responsible for obtaining LeaderInfo and MovementController
     * and passing them here.
     */
    public void onTick(GroupLeaderMapTrackerService.LeaderInfo leaderInfo,
                       GroupLeaderMapTrackerService.MovementController controller) {
        service.onLeaderState(leaderInfo, controller);
    }

    // start/stop are left as no-ops to avoid lifecycle assumptions; keep them for
    // clarity and future extension where a module wants to manage the hook state.
    public void start() {}
    public void stop() {}
}
