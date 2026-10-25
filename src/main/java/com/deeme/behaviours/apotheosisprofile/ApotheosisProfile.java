package com.deeme.behaviours.apotheosisprofile;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.modules.apotheosis.ApotheosisProfileBehaviour;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;

@Feature(name = "Kristallon Apotheosis Profile [PLUS]", description = "Changes to one profile while a Kristallon Apotheosis round is open and to another one when it ends")
public class ApotheosisProfile extends ApotheosisProfileBehaviour {

    public ApotheosisProfile(PluginAPI api) throws SecurityException {
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
