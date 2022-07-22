package com.deeme.types.config;

import com.github.manolo8.darkbot.config.PlayerTag;
import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.config.types.Tag;
import com.github.manolo8.darkbot.config.types.TagDefault;

public class SentinelConfig {
    @Option(value = "Sentinel ID", description = "Main priority. Can be empty, will use tag or group leader")
    @Num(max = 2000000000, step = 1)
    public int MASTER_ID = 0;

    @Option(value = "Sentinel Tag", description = "Medium priority. He'll follow every ship with that tag")
    @Tag(TagDefault.ALL)
    public PlayerTag SENTINEL_TAG = null;

    @Option(value = "Follow group leader", description = "Last priority. Follow the leader of the group")
    public boolean followGroupLeader = true;

    @Option(value = "Ignore security", description = "Ignore the config, when enabled, the ship will follow you to death.")
    public boolean ignoreSecurity = false;

    @Option(value = "Range towards the leader", description = "Distance it will be from the leader")
    @Num(min = 10, max = 1000, step = 100)
    public int rangeToLider = 300;

    @Option(value = "Enable collector", description = "It will use the collector module when it is not doing anything.")
    public boolean collectorActive = false;

    @Option(value = "Copy master formation", description = "It will try to use the master's formation")
    public boolean copyMasterFormation = false;

    @Option(value = "Auto choose the best formation", description = "It only works if you have disabled the copy master formation option.")
    public boolean useBestFormation = true;

    @Option(value = "Auto use the best ability", description = "Beta - Can use almost all abilities")
    public boolean useAbility = false;

    public @Option(value = "Auto Attack", description = "Will attack even when the master is not attacking") AutoAttack autoAttack = new AutoAttack();

    public @Option(value = "Auto Cloak", description = "It will automatically camouflage") AutoCloak autoCloak = new AutoCloak();
}