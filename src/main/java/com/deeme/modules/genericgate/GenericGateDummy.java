package com.deeme.modules.genericgate;

import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.utils.Inject;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.modules.genericgate.Config;
import com.deemeplus.modules.genericgate.GenericGate;
import com.github.manolo8.darkbot.config.NpcExtraFlag;
import com.github.manolo8.darkbot.core.itf.NpcExtraProvider;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;

@Feature(name = "Generic Gate [PLUS]", description = "For any map, event")
public class GenericGateDummy implements Module, Configurable<Config>, NpcExtraProvider {

    private GenericGate privateModule;

    public GenericGateDummy(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class));
    }

    @Inject
    public GenericGateDummy(PluginAPI api, AuthAPI auth) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        VerifierChecker.requireAuthenticity(auth);
        ExtensionsAPI extensionsAPI = api.getAPI(ExtensionsAPI.class);
        Utils.discordDonorCheck(extensionsAPI.getFeatureInfo(this.getClass()), auth.getAuthId());

        try {
            this.privateModule = new GenericGate(api);
        } catch (Exception e) {
            extensionsAPI.getFeatureInfo(this.getClass()).addFailure("Error", e.getMessage());
        }
    }

    @Override
    public NpcExtraFlag[] values() {
        return this.privateModule.values();
    }

    @Override
    public void setConfig(ConfigSetting<Config> arg0) {
        this.privateModule.setConfig(arg0);
    }

    @Override
    public void onTickModule() {
        this.privateModule.onTickModule();
    }

    @Override
    public String getStatus() {
        return this.privateModule.getStatus();
    }

    @Override
    public boolean canRefresh() {
        return this.privateModule.canRefresh();
    }
}
