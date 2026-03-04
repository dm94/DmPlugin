package com.deeme.modules.sentinel;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Percentage;
import eu.darkbot.api.config.annotations.Number;

/**
 * Configuration for the Group Leader Map Tracker feature.
 * This controls whether the bot follows the group leader's current map and
 * whether it reacts to low group health by pressing a heal button or using
 * an ability.
 */
@Configuration("sentinel.group_leader_map_tracker")
public class GroupLeaderMapTracker {
    @Option("group_leader_map_tracker.enabled")
    public boolean enabled = false;

    @Option("group_leader_map_tracker.followLeaderMap")
    public boolean followLeaderMap = true;

    @Option("group_leader_map_tracker.priorityAreas")
    public String priorityAreas = ""; // comma separated area IDs/names (optional)

    @Option("group_leader_map_tracker.useGroupHealth")
    public boolean useGroupHealth = false;

    @Option("group_leader_map_tracker.groupHealthThresholdPercent")
    @Percentage
    public double groupHealthThresholdPercent = 0.5; // 50%

    @Option("group_leader_map_tracker.pressHealthButton")
    public boolean pressHealthButton = true;

    @Option("group_leader_map_tracker.abilityKey")
    public String abilityKey = ""; // ability key to use when health is low (if not pressing the health button)

    @Option("group_leader_map_tracker.followDelayMs")
    @Number(min = 0, max = 60_000, step = 100)
    public int followDelayMs = 2000; // delay between follow attempts in ms
}
