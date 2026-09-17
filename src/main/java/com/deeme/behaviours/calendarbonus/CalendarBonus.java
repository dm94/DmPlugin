package com.deeme.behaviours.calendarbonus;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.modules.monthlydeluxe.CalendarBonusBehaviour;
import com.deemeplus.modules.monthlydeluxe.CalendarBonusConfig;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;

@Feature(name = "Calendar Bonus", description = "Claims the daily bonus calendar reward when it is available")
public class CalendarBonus extends CalendarBonusBehaviour {

    public CalendarBonus(PluginAPI api) throws SecurityException {
        super(api);

        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        AuthAPI auth = api.requireAPI(AuthAPI.class);

        VerifierChecker.requireAuthenticity(auth);

        ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
        FeatureInfo<?> feature = extensionsAPI.getFeatureInfo(this.getClass());
        Utils.discordCheck(feature, auth.getAuthId());
        Utils.showDonateDialog(feature, auth.getAuthId());
    }

    @Override
    public void setConfig(ConfigSetting<CalendarBonusConfig> config) {
        super.setConfig(config);
    }
}
