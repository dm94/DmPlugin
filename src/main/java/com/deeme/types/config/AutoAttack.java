package com.deeme.types.config;

import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;

@Option(value = "Auto Attack", description = "Will attack even when the master is not attacking")
public class AutoAttack {
    @Option(value = "Help to attack NPCs", description = "Will help to attack NPCs")
    public boolean helpAttackNPCs = true;

    @Option(value = "Help to attack Enemy Players", description = "Will help to attack Enemy Players")
    public boolean helpAttackEnemyPlayers = true;

    @Option(value = "Help attack all players", description = "Will help to attack Players. Ignore whether he is an enemy or not")
    public boolean helpAttackPlayers = true;

    @Option(value = "Auto Attack Enemies", description = "Attack enemies in range")
    public boolean autoAttackEnemies = false;

    @Option(value = "Maximum range for enemies", description = "Enemies above this range will not be attacked")
    @Num(min = 0, max = 1000, step = 100)
    public int rangeForEnemies = 100;

    @Option(value = "Defend against NPCs", description = "It will only attack NPCs if it is not attacking and is being attacked")
    public boolean defendFromNPCs = false;
}