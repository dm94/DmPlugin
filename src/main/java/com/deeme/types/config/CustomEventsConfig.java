package com.deeme.types.config;

import com.github.manolo8.darkbot.config.types.Option;

public class CustomEventsConfig {
    @Option("Tick with bot stopped")
    public boolean tickStopped = false;

    public @Option("Event 1") ExtraKeyConditionsKey otherKey = new ExtraKeyConditionsKey();
    public @Option("Event 2") ExtraKeyConditionsKey otherKey2 = new ExtraKeyConditionsKey();
    public @Option("Event 3") ExtraKeyConditionsKey otherKey3 = new ExtraKeyConditionsKey();
    public @Option("Event 4") ExtraKeyConditionsKey otherKey4 = new ExtraKeyConditionsKey();
    public @Option("Event 5") ExtraKeyConditionsKey otherKey5 = new ExtraKeyConditionsKey();
    public @Option("Event 6") ExtraKeyConditionsSelectable selectable1 = new ExtraKeyConditionsSelectable();
    public @Option("Event 7") ExtraKeyConditionsSelectable selectable2 = new ExtraKeyConditionsSelectable();
    public @Option("Event 8") ExtraKeyConditionsSelectable selectable3 = new ExtraKeyConditionsSelectable();
    public @Option("Event 9") ExtraKeyConditionsSelectable selectable4 = new ExtraKeyConditionsSelectable();
    public @Option("Event 10") ExtraKeyConditionsSelectable selectable5 = new ExtraKeyConditionsSelectable();
    public @Option("Event 11") ExtraKeyConditionsSelectable selectable6 = new ExtraKeyConditionsSelectable();
    public @Option("Event 12") ExtraKeyConditionsSelectable selectable7 = new ExtraKeyConditionsSelectable();
    public @Option("Event 13") ExtraKeyConditionsSelectable selectable8 = new ExtraKeyConditionsSelectable();
    public @Option("Event 14") ExtraKeyConditionsSelectable selectable9 = new ExtraKeyConditionsSelectable();
    public @Option("Event 15") ExtraKeyConditionsSelectable selectable10 = new ExtraKeyConditionsSelectable();
}
