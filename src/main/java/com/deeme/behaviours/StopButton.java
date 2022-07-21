package com.deeme.behaviours;

import java.util.Arrays;
import java.util.Collection;

import javax.swing.JComponent;

import com.deeme.types.VerifierChecker;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.itf.ExtraMenuProvider;
import com.github.manolo8.darkbot.modules.DisconnectModule;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.other.Gui;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.GameScreenAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "StopButton", description = "Add a button to stop the bot completely")
public class StopButton implements Behavior, ExtraMenuProvider {

    protected final PluginAPI api;
    protected final BotAPI bot;

    private final Gui lostConnectionGUI;

    public boolean stopBot = false;

    public StopButton(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class),
                api.requireAPI(BotAPI.class));
    }

    @Inject
    public StopButton(PluginAPI api, AuthAPI auth, BotAPI bot) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        this.api = api;
        this.bot = bot;

        GameScreenAPI gameScreenAPI = api.getAPI(GameScreenAPI.class);
        lostConnectionGUI = gameScreenAPI.getGui("lost_connection");
    }

    @Override
    public void onTickBehavior() {
        if (stopBot && bot.getModule().canRefresh()) {
            if (!isDisconnect() && !(bot.getModule() instanceof DisconnectModule)) {
                bot.setModule(new DisconnectModule(null, "Stop Button"));
            }
        }

    }

    @Override
    public Collection<JComponent> getExtraMenuItems(Main arg0) {
        return Arrays.asList(
                createSeparator("DmPlugin"),
                create("Stop Bot", e -> {
                    stopBot = true;
                }));
    }

    private boolean isDisconnect() {
        return lostConnectionGUI != null && lostConnectionGUI.isVisible();
    }

}
