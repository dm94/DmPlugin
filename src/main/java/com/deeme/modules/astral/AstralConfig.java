package com.deeme.modules.astral;

import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;

@Option(value = "Astral config")
public class AstralConfig {
    @Option(value = "Minimum radius for npcs")
    @Num(min = 500, max = 2000, step = 10)
    public int radioMin = 560;

    @Option(value = "Default Ammo")
    public Character ammoKey;

    @Option(value = "Always attack the nearest NPC", description = "It may work better than the other logic")
    public boolean alwaysTheClosestNPC = false;

    @Option(value = "Auto choose the best ammo", description = "Will always use the best ammo. Disabled will only be used when ticked in the NPC list.")
    public boolean useBestAmmo = false;

    @Option(value = "Auto choose the portal (TEST)", description = "It will choose the map that he thinks is the most appropriate.")
    public boolean autoChoosePortal = false;

    @Option(value = "Auto choose the item (TEST)", description = "It will choose the items randomly. DON´T USE FOR NOW")
    public boolean autoChooseItem = false;
}