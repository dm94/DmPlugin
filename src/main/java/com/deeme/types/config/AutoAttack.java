package com.deeme.types.config;

import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Configuration;

@Configuration("sentinel.auto_attack")
public class AutoAttack {
    @Option("auto_attack.help_attack_npcs")
    public boolean helpAttackNPCs = true;

    @Option("auto_attack.help_attack_enemies")
    public boolean helpAttackEnemyPlayers = true;

    @Option("auto_attack.help_attack_players")
    public boolean helpAttackPlayers = true;

    @Option("auto_attack.auto_attack_enemies")
    public boolean autoAttackEnemies = false;

    @Option("auto_attack.max_range")
    @Number(min = 0, max = 1000, step = 100)
    public int rangeForEnemies = 100;

    @Option("auto_attack.defend_from_npcs")
    public boolean defendFromNPCs = false;
}