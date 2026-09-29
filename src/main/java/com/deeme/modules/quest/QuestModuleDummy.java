package com.deeme.modules.quest;

import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JLabel;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.modules.quest.QuestModule;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Draw;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.api.extensions.InstructionProvider;
import eu.darkbot.api.extensions.PluginInfo;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.I18nAPI;

@Draw(value = Draw.Stage.OVERLAY)
@Feature(name = "Quest Module [PLUS]", description = "For do quests")
public class QuestModuleDummy extends QuestModule implements InstructionProvider {
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

        I18nAPI i18n = api.requireAPI(I18nAPI.class);
        FeatureInfo<?> featureInfo = extensionsAPI.getFeatureInfo(this.getClass());
        PluginInfo pluginInfo = featureInfo != null ? featureInfo.getPluginInfo() : null;
        label.setText(i18n.getOrDefault(pluginInfo, "quest_module.first_time_info",
                "The first time, delete the NPC list and send the bot to the map where you want the quest to be done"));
    }

    @Override
    public JComponent beforeConfig() {
        return this.label;
    }
}
