package com.deeme.behaviours.ai;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.modules.ai.npc.AiBrainBehaviour;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;

@Feature(name = "AI Brain [PLUS]", description = "Keeps the NPC decision up to date and applies it to the bot configuration (shadow mode by default)")
public class AiBrain extends AiBrainBehaviour {

    public AiBrain(PluginAPI api) throws SecurityException {
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
