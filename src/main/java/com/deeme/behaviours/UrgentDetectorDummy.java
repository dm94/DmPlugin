package com.deeme.behaviours;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Feature;

@Feature(name = "Urgent Detector [OLD]", description = "Use the other Urgent Detector")
public class UrgentDetectorDummy {
    public UrgentDetectorDummy(PluginAPI api) throws SecurityException {
        throw new UnsupportedOperationException("Use the other Urgent Detector, this one will be deleted");
    }
}
