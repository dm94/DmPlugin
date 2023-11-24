package com.deeme.behaviours.aitrainer;

import com.deemeplus.behaviours.aitrainer.AiTrainer;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "AI Trainer", description = "Send information about the missiles and lasers used for AI training purposes")
public class AiTrainerDummy implements Behavior, Task {

    private AiTrainer privateBehavior;

    @Inject
    public AiTrainerDummy(PluginAPI api, HeroAPI hero, StatsAPI stats, HeroItemsAPI items, AuthAPI auth) {
        this.privateBehavior = new AiTrainer(api, hero, stats, items, auth);
    }

    @Override
    public void onTickTask() {
        if (this.privateBehavior == null) {
            return;
        }

        this.privateBehavior.onTickTask();
    }

    @Override
    public void onTickBehavior() {
        if (this.privateBehavior == null) {
            return;
        }

        this.privateBehavior.onTickBehavior();
    }

}
