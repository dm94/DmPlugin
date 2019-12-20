package com.deeme.types.config;

import com.github.manolo8.darkbot.config.types.Option;

@Option(value = "Hours", description = "Hours/day")
public class Hour {

    @Option(value = "Mon", description = "Monday")
    public String mon = "P1";

    @Option(value = "Tue", description = "Tuesday")
    public String tue = "P1";

    @Option(value = "Wed", description = "Wednesday")
    public String wed = "P1";

    @Option(value = "Thu", description = "Thursday")
    public String thu = "P1";

    @Option(value = "Fri", description = "Friday")
    public String fri = "P1";

    @Option(value = "Sat", description = "Saturday")
    public String sat = "P1";

    @Option(value = "Sun", description = "Sunday")
    public String sun = "P1";

    @Override
    public String toString() {
        return "";
    }

}
