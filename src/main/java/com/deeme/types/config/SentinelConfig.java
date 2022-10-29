package com.deeme.types.config;

import com.github.manolo8.darkbot.config.PlayerTag;
import com.github.manolo8.darkbot.config.types.Tag;
import com.github.manolo8.darkbot.config.types.TagDefault;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;

@Configuration("sentinel")
public class SentinelConfig {
    @Option("sentinel.master_id")
    @Number(max = 2000000000, step = 1)
    public int MASTER_ID = 0;

    @Option("sentinel.tag")
    @Tag(TagDefault.ALL)
    public PlayerTag SENTINEL_TAG = null;

    @Option("sentinel.follow_group")
    public boolean followGroupLeader = true;

    @Option("sentinel.ignore_safety")
    public boolean ignoreSecurity = false;

    @Option("sentinel.range_to_leader")
    @Number(min = 10, step = 100, max = 100000)
    public int rangeToLider = 300;

    @Option("sentinel.collector")
    public boolean collectorActive = false;

    @Option("sentinel.copy_master_formation")
    public boolean copyMasterFormation = false;

    @Option("sentinel.jumping_portals")
    public boolean followByPortals = false;

    @Option("sentinel.use_master_destination")
    public boolean goToMasterDestination = true;

    @Option("sentinel.aggressive_follow")
    public boolean aggressiveFollow = false;

    @Option("sentinel.auto_attack")
    public AutoAttack autoAttack = new AutoAttack();

    @Option("general.auto_cloak")
    public AutoCloak autoCloak = new AutoCloak();
}