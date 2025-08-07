package com.deeme.behaviours.bootykeymanager;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;

import java.util.HashSet;
import java.util.Set;
import com.deeme.types.suppliers.BootyKeySupplier;

@Configuration("booty_key_collector_manager")
public class BootyKeyCollectorConfig {

    @Option("booty_key_collector_manager.booty_keys_to_monitor")
    @Dropdown(multi = true, options = BootyKeySupplier.class)
    public Set<String> bootyKeysToMonitor = new HashSet<>();

    @Option("booty_key_collector_manager.check_interval")
    @Number(min = 1, max = 2000, step = 1)
    public int checkInterval = 30;
}
