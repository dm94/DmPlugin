package com.deeme.modules.genericgate;

import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.modules.genericgate.GenericGate;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Feature;

@Feature(name = "Generic Gate [PLUS]", description = "For any map, event")
public class GenericGateDummy extends GenericGate {
    public GenericGateDummy(PluginAPI api) {
        super(api);

        AuthAPI auth = api.requireAPI(AuthAPI.class);
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }
        VerifierChecker.requireAuthenticity(auth);
        ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
        Utils.discordDonorCheck(extensionsAPI.getFeatureInfo(this.getClass()), auth.getAuthId());
    }
}
