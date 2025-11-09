package com.deeme.modules.quest;

import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;

import java.util.Arrays;
import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.JLabel;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.modules.quest.QuestModule;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.ExtraMenus;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.InstructionProvider;

@Feature(name = "Quest Module [PLUS]", description = "For do quests")
public class QuestModuleDummy extends QuestModule implements InstructionProvider, ExtraMenus {
    private JLabel label = new JLabel("");

    public QuestModuleDummy(PluginAPI api) throws SecurityException {
        super(api);

        AuthAPI auth = api.requireAPI(AuthAPI.class);
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        VerifierChecker.requireAuthenticity(auth);

        ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
        Utils.discordDonorCheck(extensionsAPI.getFeatureInfo(this.getClass()), auth.getAuthId());

        label.setText(
                "The first time, delete the NPC list and send the bot to the map where you want the quest to be done");
    }

    @Override
    public JComponent beforeConfig() {
        return this.label;
    }

    @Override
    public Collection<JComponent> getExtraMenuItems(PluginAPI api) {
        return Arrays.asList(
                createSeparator("Quest Module - Debug"),
                create("Clear NPC List", e -> super.clearNpcList()));
    }
}
