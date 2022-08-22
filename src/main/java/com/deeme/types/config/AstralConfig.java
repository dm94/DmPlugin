package com.deeme.types.config;

import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;

@Option(value = "Astral config")
public class AstralConfig {
    @Option(value = "Minimum radius for npcs")
    @Num(min = 500, max = 2000, step = 100)
    public int radioMin = 560;

    @Option(value = "Default Ammo")
    public Character ammoKey;

    @Option(value = "Auto choose the best ammo", description = "Remember to add the ignore boxes tag to the npc for this to work")
    public boolean useBestAmmo = true;

    @Option(value = "Auto choose the best ammo Always", description = "If you enable it, it will always use the best ammo")
    public boolean useBestAmmoAlways = false;

    @Option(value = "Auto choose the portal", description = "It will choose the map that he thinks is the most appropriate.")
    public boolean autoChoosePortal = false;

    @Option(value = "Auto choose the item (TEST)", description = "It will choose the items randomly. DON´T USE FOR NOW")
    public boolean autoChooseItem = false;
}