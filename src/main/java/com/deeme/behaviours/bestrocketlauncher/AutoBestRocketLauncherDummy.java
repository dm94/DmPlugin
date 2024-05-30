package com.deeme.behaviours.bestrocketlauncher;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.behaviours.bestrocketlauncher.AutoBestRocketLauncher;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "Auto Best Rocket Launcher [PLUS]", description = "Auto use the best Rocket Launcher")
public class AutoBestRocketLauncherDummy extends AutoBestRocketLauncher {
    public AutoBestRocketLauncherDummy(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class));
    }

    @Inject
    public AutoBestRocketLauncherDummy(PluginAPI api, AuthAPI auth) {
        super(api);

        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        VerifierChecker.requireAuthenticity(auth);
        ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
        Utils.discordDonorCheck(extensionsAPI.getFeatureInfo(this.getClass()), auth.getAuthId());
    }
}
