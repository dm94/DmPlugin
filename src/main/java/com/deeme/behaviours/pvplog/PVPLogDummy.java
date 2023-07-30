package com.deeme.behaviours.pvplog;

import com.deemetool.behaviours.pvplog.PVPLog;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Task;

@Feature(name = "PVPLog", description = "Send a battle log for training purposes")
public class PVPLogDummy implements Behavior, Task {

    private PVPLog privateBehaviour;

    public PVPLogDummy(PluginAPI api) {
        this.privateBehaviour = new PVPLog(api);
    }

    @Override
    public void onTickTask() {
        this.privateBehaviour.onTickTask();
    }

    @Override
    public void onTickBehavior() {
        this.privateBehaviour.onTickBehavior();
    }

}
