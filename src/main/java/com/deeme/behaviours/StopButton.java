package com.deeme.behaviours;

import java.util.Arrays;
import java.util.Collection;

import javax.swing.JComponent;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.github.manolo8.darkbot.modules.DisconnectModule;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.ExtraMenus;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.other.Gui;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.GameScreenAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "StopButton", description = "Add a button to stop the bot completely")
public class StopButton implements Behavior, ExtraMenus {

    protected final PluginAPI api;
    protected final BotAPI bot;
    protected final HeroAPI heroapi;
    protected final MovementAPI movement;
    protected Collection<? extends Portal> portals;
    private final Gui lostConnectionGUI;

    private boolean stopBot = false;
    private boolean closeBot = false;

    public StopButton(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class),
                api.requireAPI(BotAPI.class));
    }

    @Inject
    public StopButton(PluginAPI api, AuthAPI auth, BotAPI bot) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog(auth.getAuthId());

        this.api = api;
        this.bot = bot;
        this.heroapi = api.getAPI(HeroAPI.class);
        this.movement = api.getAPI(MovementAPI.class);
        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        this.portals = entities.getPortals();

        GameScreenAPI gameScreenAPI = api.getAPI(GameScreenAPI.class);
        lostConnectionGUI = gameScreenAPI.getGui("lost_connection");
    }

    @Override
    public void onTickBehavior() {
        if (!stopBot) {
            return;
        }

        if (!heroapi.getMap().isGG() && bot.getModule().canRefresh()) {
            if (!isDisconnect() && !(bot.getModule() instanceof DisconnectModule)) {
                bot.setModule(new DisconnectModule(null, "Stop Button"));
            }
        } else if (heroapi.getMap() != null && heroapi.getMap().isGG() && !portals.isEmpty()) {
            goOutFromGG();
        }

        stopCheck();
    }

    @Override
    public void onStoppedBehavior() {
        stopCheck();
    }

    @Override
    public Collection<JComponent> getExtraMenuItems(PluginAPI pluginAPI) {
        return Arrays.asList(
                createSeparator("StopButton"),
                create("Stop Bot", e -> {
                    stopBot = true;
                }), create("Stop Bot + Close", e -> {
                    stopBot = true;
                    closeBot = true;
                }));
    }

    private void goOutFromGG() {
        Portal p = portals.stream().filter(m -> m.getTargetMap().isPresent() && !m.getTargetMap().get().isGG())
                .findFirst().orElse(null);
        if (p == null || p.isJumping()) {
            return;
        }

        if (heroapi.distanceTo(p) < 200) {
            movement.jumpPortal(p);
        } else {
            movement.moveTo(p);
        }
    }

    private void stopCheck() {
        if (isDisconnect()) {
            if (closeBot) {
                System.exit(0);
            }
            stopBot = false;
        }
    }

    private boolean isDisconnect() {
        return lostConnectionGUI != null && lostConnectionGUI.isVisible();
    }

}
