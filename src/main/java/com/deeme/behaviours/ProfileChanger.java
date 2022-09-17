package com.deeme.behaviours;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deeme.types.config.ProfileChanger.ProfileChangerConfig;
import com.github.manolo8.darkbot.Main;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "ProfileChanger", description = "Change the profile to another one when completing a task")
public class ProfileChanger implements Behavior, Configurable<ProfileChangerConfig> {

    protected final PluginAPI api;
    protected final BotAPI bot;
    protected final HeroAPI hero;
    private ProfileChangerConfig config;
    private Main main;

    public boolean stopBot = false;

    public ProfileChanger(Main main, PluginAPI api) {
        this(main, api, api.requireAPI(AuthAPI.class),
                api.requireAPI(BotAPI.class),
                api.requireAPI(HeroAPI.class));
    }

    @Inject
    public ProfileChanger(Main main, PluginAPI api, AuthAPI auth, BotAPI bot, HeroAPI hero) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        if (!Utils.discordCheck(auth.getAuthId())) {
            Utils.showDiscordDialog();
            ExtensionsAPI extensionsAPI = api.getAPI(ExtensionsAPI.class);
            extensionsAPI.getFeatureInfo(this.getClass())
                    .addFailure("To use this option you need to be on my discord", "Log in to my discord and reload");
        }

        this.main = main;
        this.api = api;
        this.bot = bot;
        this.hero = hero;
    }

    @Override
    public void setConfig(ConfigSetting<ProfileChangerConfig> arg0) {
        this.config = arg0.getValue();
    }

    @Override
    public void onTickBehavior() {
        checkNPC();
        if (config.condition == null || config.condition.get(api).allows()) {
            if ((!config.npcExtraCondition.active || (config.npcExtraCondition.active
                    && config.npcExtraCondition.npcCounter >= config.npcExtraCondition.npcsToKill)) &&
                    (!config.npcExtraCondition2.active || (config.npcExtraCondition2.active
                            && config.npcExtraCondition2.npcCounter >= config.npcExtraCondition2.npcsToKill))) {
                config.npcExtraCondition.npcCounter = 0;
                config.npcExtraCondition2.npcCounter = 0;
                main.setConfig(config.BOT_PROFILE);
            }
        }
    }

    private void checkNPC() {
        Lockable target = hero.getLocalTarget();
        if (target != null && target.isValid() && target.isOwned()) {
            String name = target.getEntityInfo().getUsername();
            if (name != null && name.toLowerCase().contains(config.npcExtraCondition.npcName.toLowerCase())
                    && target.getId() != config.npcExtraCondition.lastNPCId) {
                config.npcExtraCondition.lastNPCId = target.getId();
                config.npcExtraCondition.npcCounter++;
            }
            if (name != null && name.toLowerCase().contains(config.npcExtraCondition2.npcName.toLowerCase())
                    && target.getId() != config.npcExtraCondition2.lastNPCId) {
                config.npcExtraCondition2.lastNPCId = target.getId();
                config.npcExtraCondition2.npcCounter++;
            }
        }
    }
}
