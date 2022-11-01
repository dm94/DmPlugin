package com.deeme.behaviours.profilechanger;

import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Number;

@Configuration("profile_changer.resource_counter_condition")
public class ResourceCounterCondition {
    public transient int lastResourceId = 0;
    public transient double lastResourcePosition = 0;

    @Option("general.enabled")
    public boolean active = false;

    @Option("profile_changer.resource_counter_condition.resource_name")
    @Dropdown(options = ResourceSupplier.class)
    public String resourceName = "";

    @Option("profile_changer.resource_counter_condition.resource_to_farm")
    @Number(min = 0, max = 100000, step = 1)
    public int resourcesToFarm = 1;

    @Option("profile_changer.resource_counter_condition.resources_farmerd")
    @Number(min = 0, max = 1000000, step = 1)
    public int resourcesFarmed = 0;
}
