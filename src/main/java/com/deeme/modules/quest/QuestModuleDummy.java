package com.deeme.modules.quest;

import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.utils.Inject;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.modules.quest.QuestModule;
import com.github.manolo8.darkbot.Main;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Feature;

@Feature(name = "Quest Module [PLUS]", description = "For do quests")
public class QuestModuleDummy implements Module {
    private QuestModule privateModule;

    public QuestModuleDummy(Main main, PluginAPI api) {
        this(main, api, api.requireAPI(AuthAPI.class));
    }

    @Inject
    public QuestModuleDummy(Main main, PluginAPI api, AuthAPI auth) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        VerifierChecker.requireAuthenticity(auth);
        ExtensionsAPI extensionsAPI = api.getAPI(ExtensionsAPI.class);
        Utils.discordDonorCheck(extensionsAPI.getFeatureInfo(this.getClass()), auth.getAuthId());

        try {
            this.privateModule = new QuestModule(main, api);
        } catch (Exception e) {
            extensionsAPI.getFeatureInfo(this.getClass()).addFailure("Error", e.getMessage());
        }
    }

    @Override
    public void onTickModule() {
        this.privateModule.tick();
    }

    @Override
    public String getStatus() {
        return this.privateModule.getStatus();
    }

    @Override
    public String getStoppedStatus() {
        return this.privateModule.getStatus();
    }

    @Override
    public boolean canRefresh() {
        return this.privateModule.canRefresh();
    }
}