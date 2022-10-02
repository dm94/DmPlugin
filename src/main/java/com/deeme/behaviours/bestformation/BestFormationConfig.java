package com.deeme.behaviours.bestformation;

import com.github.manolo8.darkbot.config.types.Option;

public class BestFormationConfig {
    @Option(value = "Enable for NPCs")
    public boolean npcEnabled = true;

    @Option(value = "Always use veteran (F-16-VT)", description = "Will always use the veteran formation. Disabled will only be used when ticked in the NPC list.")
    public boolean useVeteran = true;
}