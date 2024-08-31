package com.deeme.behaviours.urgentdetector;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.modules.temporal.UrgentQuestModule;
import com.github.manolo8.darkbot.core.objects.facades.DiminishQuestMediator;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.shared.modules.TemporalModule;

@Feature(name = "Urgent Detector", description = "Changes profile when it detects an urgent quest")
public class UrgentDetector implements Behavior, Configurable<UrgentDetectorConfig> {
    private final PluginAPI api;
    private final ConfigAPI configAPI;
    private final BotAPI botApi;
    private final DiminishQuestMediator diminishQuestMediator;

    private UrgentDetectorConfig config;

    private long nextCheck = 0;

    public UrgentDetector(PluginAPI api) throws SecurityException {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        AuthAPI auth = api.requireAPI(AuthAPI.class);

        VerifierChecker.requireAuthenticity(auth);

        ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
        FeatureInfo<?> feature = extensionsAPI.getFeatureInfo(this.getClass());
        Utils.discordCheck(feature, auth.getAuthId());
        Utils.showDonateDialog(feature, auth.getAuthId());

        this.api = api;
        this.configAPI = api.requireAPI(ConfigAPI.class);
        this.botApi = api.requireAPI(BotAPI.class);
        this.diminishQuestMediator = api.requireInstance(DiminishQuestMediator.class);
    }

    @Override
    public void setConfig(ConfigSetting<UrgentDetectorConfig> arg0) {
        this.config = arg0.getValue();
    }

    @Override
    public void onTickBehavior() {
        if (this.nextCheck < System.currentTimeMillis()) {
            this.nextCheck = System.currentTimeMillis() + (this.config.timeToCheck * 1000);

            if (this.diminishQuestMediator.isWaitAccepting() || this.diminishQuestMediator.isAccepted()) {
                if (this.config.takeQuest && this.diminishQuestMediator.isWaitAccepting()) {
                    enableTakeUrgentQuestModule();
                } else {
                    changeProfile();
                }
            }
        }
    }

    private void enableTakeUrgentQuestModule() {
        if (!(this.botApi.getModule() instanceof TemporalModule)
                && this.botApi.getModule().getClass() != UrgentQuestModule.class) {
            this.botApi.setModule(
                    new UrgentQuestModule(this.api, this.botApi, this.diminishQuestMediator));
        }
    }

    private void changeProfile() {
        this.configAPI.setConfigProfile(config.botProfile);
    }
}
