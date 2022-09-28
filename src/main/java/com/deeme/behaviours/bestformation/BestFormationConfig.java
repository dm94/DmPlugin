package com.deeme.behaviours.bestformation;

import com.github.manolo8.darkbot.config.types.Option;

public class BestFormationConfig {
    @Option(value = "Enable for NPCs")
    public boolean npcEnabled = true;

    @Option(value = "Use veteran (F-16-VT)", description = "It will change to this formation when NPCs have less than 15% health.")
    public boolean useVeteran = true;
}