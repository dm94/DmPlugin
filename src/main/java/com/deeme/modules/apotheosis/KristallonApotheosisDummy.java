package com.deeme.modules.apotheosis;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.modules.apotheosis.KristallonApotheosisModule;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;

@Feature(name = "Kristallon Apotheosis [PLUS]", description = "Automates the weekly Kristallon event: gathers points on X-2 to X-8 and joins the Emperor Kristallon arena")
public class KristallonApotheosisDummy extends KristallonApotheosisModule {

    public KristallonApotheosisDummy(PluginAPI api) throws SecurityException {
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
