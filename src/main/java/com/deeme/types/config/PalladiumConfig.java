package com.deeme.types.config;

import com.deeme.types.gui.ShipSupplier;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;

@Configuration("palladium_hangar")
public class PalladiumConfig {
    @Option("palladium_hangar.go_portal_change")
    public boolean goPortalChange = true;

    @Option("palladium_hangar.sell_on_die")
    public boolean sellOnDie = true;

    @Option("palladium_hangar.sid_ko")
    public boolean ignoreSID = false;

    @Option("palladium_hangar.collect_hangar")
    @Dropdown(options = ShipSupplier.class)
    public Integer collectHangar = -1;

    @Option("palladium_hangar.sell_hangar")
    @Dropdown(options = ShipSupplier.class)
    public Integer sellHangar = -1;

    @Option("palladium_hangar.aditional_waiting_time")
    @Number(min = 0, max = 300, step = 1)
    public int aditionalWaitingTime = 5;
}
