package com.deeme.behaviours.antitrain;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.behaviours.antitrain.AntiTrain;
import com.deemeplus.behaviours.antitrain.AntiTrainConfig;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "Anti Train [PLUS]", description = "Some options for combating trains")
public class AntiTrainDummy implements Behavior, Configurable<AntiTrainConfig> {
    private AntiTrain privateBehaviour;

    public AntiTrainDummy(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class));
    }

    @Inject
    public AntiTrainDummy(PluginAPI api, AuthAPI auth) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        VerifierChecker.requireAuthenticity(auth);
        ExtensionsAPI extensionsAPI = api.getAPI(ExtensionsAPI.class);
        Utils.discordDonorCheck(extensionsAPI.getFeatureInfo(this.getClass()), auth.getAuthId());

        try {
            this.privateBehaviour = new AntiTrain(api);
        } catch (Exception e) {
            extensionsAPI.getFeatureInfo(this.getClass()).addFailure("Error", e.getMessage());
        }
    }

    @Override
    public void setConfig(ConfigSetting<AntiTrainConfig> arg0) {
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

}
