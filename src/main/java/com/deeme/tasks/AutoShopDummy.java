package com.deeme.tasks;

import com.deemetool.tasks.autoshop.Config;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemetool.tasks.autoshop.AutoShop;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "AutoShop [PLUS]", description = "Auto Buy Items")
public class AutoShopDummy implements Task, Configurable<Config> {
    private AutoShop privateTask;

    public AutoShopDummy(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class));
    }

    @Inject
    public AutoShopDummy(PluginAPI api, AuthAPI auth) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }
        VerifierChecker.checkAuthenticity(auth);
        Utils.discordDonorCheck(api.getAPI(ExtensionsAPI.class).getFeatureInfo(this.getClass()), auth.getAuthId());

        this.privateTask = new AutoShop(api);
    }

    @Override
    public void setConfig(ConfigSetting<Config> arg0) {
        this.privateTask.setConfig(arg0);
    }

    @Override
    public void onTickTask() {
        privateTask.onTickTask();
    }
}
