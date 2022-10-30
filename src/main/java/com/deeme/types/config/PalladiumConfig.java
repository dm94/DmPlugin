package com.deeme.types.config;

import com.deeme.types.gui.ShipSupplier;
import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;

import eu.darkbot.api.config.annotations.Dropdown;

public class PalladiumConfig {
    @Option(value = "Travel to portal before switch", description = "Go to the portal to change the hangar")
    public boolean goPortalChange = true;

    @Option(value = "Sell when it is on map 5-2", description = "If your collection ship passes 5-2 and has palladium it will go to sell and then go to 5-3")
    public boolean sellOnDie = true;

    @Option(value = "Collecting Hangar (5-3)", description = "Ship 5-3 Hangar. Must be in favourites")
    @Dropdown(options = ShipSupplier.class)
    public Integer collectHangar = -1;

    @Option(value = "Selling Hangar (5-2)", description = "Ship 5-2 Hangar. Must be in favourites")
    @Dropdown(options = ShipSupplier.class)
    public Integer sellHangar = -1;

    @Option(value = "Additional time between action changes", description = "In seconds, ideal to avoid some errors")
    @Num(min = 0, max = 300, step = 1)
    public int aditionalWaitingTime = 5;
}
