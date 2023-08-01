package com.deeme.behaviours.bestrocketlauncher;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemetool.behaviours.bestrocketlauncher.AutoBestRocketLauncher;
import com.deemetool.behaviours.bestrocketlauncher.BestRocketLauncherConfig;
import com.github.manolo8.darkbot.config.NpcExtraFlag;
import com.github.manolo8.darkbot.core.itf.NpcExtraProvider;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "Auto Best Rocket Launcher [PLUS]", description = "Auto use the best Rocket Launcher")
public class AutoBestRocketLauncherDummy implements Behavior, Configurable<BestRocketLauncherConfig>, NpcExtraProvider {

    private AutoBestRocketLauncher privateBehaviour;

    public AutoBestRocketLauncherDummy(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class));
    }

    @Inject
    public AutoBestRocketLauncherDummy(PluginAPI api, AuthAPI auth) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }
        VerifierChecker.checkAuthenticity(auth);
        Utils.discordDonorCheck(api.getAPI(ExtensionsAPI.class).getFeatureInfo(this.getClass()), auth.getAuthId());

        this.privateBehaviour = new AutoBestRocketLauncher(api);
    }

    @Override
    public NpcExtraFlag[] values() {
        return privateBehaviour.values();
    }

    @Override
    public void setConfig(ConfigSetting<BestRocketLauncherConfig> arg0) {
        privateBehaviour.setConfig(arg0);
    }

    @Override
    public void onTickBehavior() {
        privateBehaviour.onTickBehavior();
    }

    @Override
    public void onStoppedBehavior() {
        privateBehaviour.onStoppedBehavior();
    }

}
