package com.deeme.behaviours;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.behaviours.urgentdetector.UrgentDetector;
import com.github.manolo8.darkbot.utils.AuthAPI;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.managers.ExtensionsAPI;

@Feature(name = "Urgent Detector [PLUS]", description = "Detects urgent quests")
public class UrgentDetectorDummy extends UrgentDetector {
    public UrgentDetectorDummy(PluginAPI api) throws SecurityException {
        super(api);

        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        AuthAPI auth = api.requireAPI(AuthAPI.class);
        VerifierChecker.requireAuthenticity(auth);
        ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
        Utils.discordDonorCheck(extensionsAPI.getFeatureInfo(this.getClass()), auth.getAuthId());
    }
}
