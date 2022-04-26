package com.deeme.types.config;

import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;

@Option("Extra Conditions")
public class ExtraKeyConditions {

    public long lastUse = 0;

    @Option(value = "Enable")
    public boolean enable = false;

    public @Option(key = "config.loot.sab.key") Character Key;

    @Option(value = "Countdown", description ="Only to be used if necessary. In seconds")
    @Num(min = 1, max = 10000)
    public int countdown = 60;

    @Option(value = "Health", description ="If health between")
    public Config.PercentRange HEALTH_RANGE = new Config.PercentRange(0.5, 0.95);

    @Option(value = "Enemy Health", description ="If the enemy's health is between")
    public Config.PercentRange HEALTH_ENEMY_RANGE = new Config.PercentRange(0.2, 0.4);

    public ExtraKeyConditions() {
    }

    public ExtraKeyConditions(int newCountDown) {
        this.countdown = newCountDown;
    }
}
