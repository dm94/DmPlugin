package com.deeme.types.config;

import com.github.manolo8.darkbot.config.PlayerTag;
import com.github.manolo8.darkbot.config.types.Tag;
import com.github.manolo8.darkbot.config.types.TagDefault;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;

@Configuration("sentinel")
public class SentinelConfig {
    @Option(value = "sentinel.master_id")
    @Number(max = 2000000000, step = 1)
    public int MASTER_ID = 0;

    @Option(value = "sentinel.tag")
    @Tag(TagDefault.ALL)
    public PlayerTag SENTINEL_TAG = null;

    @Option(value = "sentinel.follow_group")
    public boolean followGroupLeader = true;

    @Option(value = "sentinel.ignore_safety")
    public boolean ignoreSecurity = false;

    @Option(value = "sentinel.range_to_leader")
    @Number(min = 10, step = 100, max = 100000)
    public int rangeToLider = 300;

    @Option(value = "sentinel.collector")
    public boolean collectorActive = false;

    @Option(value = "sentinel.copy_master_formation")
    public boolean copyMasterFormation = false;

    @Option(value = "sentinel.jumping_portals")
    public boolean followByPortals = false;

    @Option(value = "sentinel.use_master_destination")
    public boolean goToMasterDestination = true;

    @Option(value = "sentinel.aggressive_follow")
    public boolean aggressiveFollow = false;

    @Option(value = "sentinel.auto_attack")
    public AutoAttack autoAttack = new AutoAttack();

    @Option(value = "general.auto_cloak")
    public AutoCloak autoCloak = new AutoCloak();
}