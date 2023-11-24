package com.deeme.behaviours.bestrocketlauncher;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.behaviours.bestrocketlauncher.AutoBestRocketLauncher;
import com.deemeplus.behaviours.bestrocketlauncher.BestRocketLauncherConfig;
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

        VerifierChecker.requireAuthenticity(auth);
        ExtensionsAPI extensionsAPI = api.getAPI(ExtensionsAPI.class);
        Utils.discordDonorCheck(extensionsAPI.getFeatureInfo(this.getClass()), auth.getAuthId());

        try {
            this.privateBehaviour = new AutoBestRocketLauncher(api);
        } catch (Exception e) {
            extensionsAPI.getFeatureInfo(this.getClass()).addFailure("Error", e.getMessage());
        }
    }

    @Override
    public NpcExtraFlag[] values() {
        if (this.privateBehaviour == null) {
            return new NpcExtraFlag[0];
        }

        return privateBehaviour.values();
    }

    @Override
    public void setConfig(ConfigSetting<BestRocketLauncherConfig> arg0) {
        if (this.privateBehaviour == null) {
            return;
        }

        privateBehaviour.setConfig(arg0);
    }

    @Override
    public void onTickBehavior() {
        if (this.privateBehaviour == null) {
            return;
        }

        privateBehaviour.onTickBehavior();
    }

    @Override
    public void onStoppedBehavior() {
        if (this.privateBehaviour == null) {
            return;
        }

        privateBehaviour.onStoppedBehavior();
    }

}
