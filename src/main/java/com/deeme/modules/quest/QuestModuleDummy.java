package com.deeme.modules.quest;

import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.utils.Inject;

import java.util.Arrays;
import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.JLabel;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.modules.quest.config.Config;
import com.deemeplus.modules.quest.QuestModule;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.ExtraMenus;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.InstructionProvider;

@Feature(name = "Quest Module [PLUS]", description = "For do quests")
public class QuestModuleDummy implements Module, Behavior, Configurable<Config>, InstructionProvider, ExtraMenus {
    private QuestModule privateModule;
    private JLabel label = new JLabel("");

    public QuestModuleDummy(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class));
    }

    @Inject
    public QuestModuleDummy(PluginAPI api, AuthAPI auth) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        VerifierChecker.requireAuthenticity(auth);
        ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
        Utils.discordDonorCheck(extensionsAPI.getFeatureInfo(this.getClass()), auth.getAuthId());

        try {
            this.privateModule = new QuestModule(api);
        } catch (Exception e) {
            extensionsAPI.getFeatureInfo(this.getClass()).addFailure("Error", e.getMessage());
        }

        label.setText(
                "The first time, delete the NPC list and send the bot to the map where you want the quest to be done");
    }

    @Override
    public void onTickModule() {
        if (this.privateModule == null) {
            return;
        }

        this.privateModule.tick();
    }

    @Override
    public String getStatus() {
        if (this.privateModule == null) {
            return "Loading";
        }

        return this.privateModule.getStatus();
    }

    @Override
    public String getStoppedStatus() {
        return getStatus();
    }

    @Override
    public JComponent beforeConfig() {
        return this.label;
    }

    @Override
    public boolean canRefresh() {
        if (this.privateModule == null) {
            return false;
        }

        return this.privateModule.canRefresh();
    }

    @Override
    public void setConfig(ConfigSetting<Config> config) {
        if (this.privateModule == null) {
            return;
        }

        this.privateModule.setConfig(config);
    }

    @Override
    public void onTickBehavior() {
        if (this.privateModule == null) {
            return;
        }

        this.privateModule.onTickBehavior();
    }

    @Override
    public Collection<JComponent> getExtraMenuItems(PluginAPI api) {
        return Arrays.asList(
                createSeparator("Quest Module - Debug"),
                create("Clear NPC List", e -> {
                    if (this.privateModule == null) {
                        return;
                    }

                    this.privateModule.clearNpcList();
                }));
    }
}
