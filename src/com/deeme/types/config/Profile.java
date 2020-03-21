package com.deeme.types.config;

import com.deeme.types.gui.ShipSupplier;
import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.config.types.Options;
import com.github.manolo8.darkbot.gui.tree.components.JListField;

@Option("Profile")
public class Profile {
    @Option(value = "Hangar ID", description = "Test. Hangar to use.")
    @Editor(JListField.class)
    @Options(ShipSupplier.class)
    public String hangar = "";

    @Option(value = "Map Timetable", description = "If you want to use the map timetable to change the map every x time or death")
    public boolean useMapTimetable = false;

    @Option(value = "Random pause", description = "If you want me to make random pauses")
    public boolean randomPause = false;

    public @Option(key = "config.general") Config.General GENERAL = new Config.General();

    public @Option(key = "config.pet") Config.PetSettings PET = new Config.PetSettings();

    public @Option(key = "config.loot.sab") Config.Loot.Sab SAB = new Config.Loot.Sab();

    public @Option(key = "config.group") Config.GroupSettings GROUP = new Config.GroupSettings();

    @Override
    public String toString() {
        return (useMapTimetable ? "MT" : GENERAL.WORKING_MAP) + GENERAL.CURRENT_MODULE;
    }
}