package com.deeme.behaviours.antitrain;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemetool.behaviours.antitrain.AntiTrain;
import com.deemetool.behaviours.antitrain.AntiTrainConfig;

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
        VerifierChecker.checkAuthenticity(auth);
        Utils.discordDonorCheck(api.getAPI(ExtensionsAPI.class).getFeatureInfo(this.getClass()), auth.getAuthId());

        this.privateBehaviour = new AntiTrain(api);
    }

    @Override
    public void setConfig(ConfigSetting<AntiTrainConfig> arg0) {
        privateBehaviour.setConfig(arg0);
    }

    @Override
    public void onTickBehavior() {
        privateBehaviour.onTickBehavior();
    }

}
