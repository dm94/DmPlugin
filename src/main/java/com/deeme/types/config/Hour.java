package com.deeme.types.config;

import com.github.manolo8.darkbot.config.types.Option;

@Option(value = "Hours", description = "Hours/day")
public class Hour {
    String defaultValue = "P1";

    @Option(value = "Mon", description = "Monday")
    public String mon = defaultValue;

    @Option(value = "Tue", description = "Tuesday")
    public String tue = defaultValue;

    @Option(value = "Wed", description = "Wednesday")
    public String wed = defaultValue;

    @Option(value = "Thu", description = "Thursday")
    public String thu = defaultValue;

    @Option(value = "Fri", description = "Friday")
    public String fri = defaultValue;

    @Option(value = "Sat", description = "Saturday")
    public String sat = defaultValue;

    @Option(value = "Sun", description = "Sunday")
    public String sun = defaultValue;

    @Override
    public String toString() {
        return "";
    }

}
