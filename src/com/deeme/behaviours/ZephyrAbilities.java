package com.deeme.behaviours;

import com.deeme.types.config.ExtraKeyConditions;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.core.itf.Behaviour;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.extensions.features.Feature;

@Feature(name = "Zephyr Abilities", description = "Use the abilities of the Zephyr")
public class ZephyrAbilities implements Behaviour, Configurable<ZephyrAbilities.ZephyrConfig> {

    private ZephyrConfig zephyrConfig;
    private Main main;
    private int lastNpcID = 0;

    @Override
    public void install(Main m) {
        this.main = m;
    }

    @Override
    public void setConfig(ZephyrConfig c) {
        this.zephyrConfig = c;
    }

    @Override
    public void tick() {
        if (main.hero.target != null && main.hero.target.id != this.lastNpcID){
            lastNpcID = main.hero.target.id;
            zephyrConfig.momentum.lastUse++;
            zephyrConfig.tripleBarrage.lastUse = 0;
        }
    }

    public static class ZephyrConfig {
        public @Option(value = "Momentum", description = "Momentum ability")
        ExtraKeyConditions momentum = new ExtraKeyConditions();

        public @Option(value = "TripleBarrage", description = "Triple Barrage ability")
        ExtraKeyConditions tripleBarrage = new ExtraKeyConditions();
    }
}
