package com.deeme.behaviours.bestammo;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.behaviours.bestammo.AutoBestAmmo;
import com.deemeplus.behaviours.bestammo.BestAmmoConfig;
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

@Feature(name = "Auto Best Ammo [PLUS]", description = "Auto use the best ammo")
public class AutoBestAmmoDummy implements Behavior, Configurable<BestAmmoConfig>, NpcExtraProvider {

    private AutoBestAmmo privateBehaviour;

    public AutoBestAmmoDummy(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class));
    }

    @Inject
    public AutoBestAmmoDummy(PluginAPI api, AuthAPI auth) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        VerifierChecker.requireAuthenticity(auth);
        ExtensionsAPI extensionsAPI = api.getAPI(ExtensionsAPI.class);
        Utils.discordDonorCheck(extensionsAPI.getFeatureInfo(this.getClass()), auth.getAuthId());

        try {
            this.privateBehaviour = new AutoBestAmmo(api);
        } catch (Exception e) {
            extensionsAPI.getFeatureInfo(this.getClass()).addFailure("Error", e.getMessage());
        }
    }

    @Override
    public NpcExtraFlag[] values() {
        return privateBehaviour.values();
    }

    @Override
    public void setConfig(ConfigSetting<BestAmmoConfig> arg0) {
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
