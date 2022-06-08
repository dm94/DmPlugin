package com.deeme.types.backpage;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.backpage.HangarManager;
import com.github.manolo8.darkbot.backpage.hangar.Hangar;
import com.github.manolo8.darkbot.core.objects.LogoutGui;
import com.github.manolo8.darkbot.modules.DisconnectModule;
import com.github.manolo8.darkbot.utils.Time;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.game.other.Gui;
import eu.darkbot.api.managers.BackpageAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.GameScreenAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;

import java.util.Arrays;
import java.util.List;

import static com.github.manolo8.darkbot.Main.API;

public class HangarChanger {
    protected final PluginAPI api;
    protected final HeroAPI heroapi;
    protected final MovementAPI movement;
    protected final BotAPI bot;
    protected final BackpageAPI backpageAPI;

    public final Gui lostConnectionGUI;

    public Integer activeHangar = null;
    private final Main main;
    private final HangarManager hangarManager;
    public long disconectTime = 0;
    private long lastStopChangeHangar = 0;
    public final LogoutGui logout;
    private final List<GameMap> maps;

    public HangarChanger(Main m, GameMap... maps) {
        this.main = m;
        this.hangarManager = main.backpage.hangarManager;
        this.logout = main.guiManager.logout;
        this.maps = maps == null || maps.length == 0 ? null : Arrays.asList(maps);

        this.api = main.pluginAPI.getAPI(PluginAPI.class);
        this.heroapi = api.getAPI(HeroAPI.class);
        this.movement = api.getAPI(MovementAPI.class);
        this.bot = api.getAPI(BotAPI.class);
        this.backpageAPI = api.getAPI(BackpageAPI.class);

        GameScreenAPI gameScreenAPI = api.getAPI(GameScreenAPI.class);
        lostConnectionGUI = gameScreenAPI.getGui("lost_connection");
    }

    public void updateHangarActive() {
        try {
            hangarManager.updateHangarList();
            hangarManager.updateCurrentHangar();
            activeHangar = hangarManager.getHangarList().getData().getRet().getHangars().stream()
                    .filter(Hangar::isActive)
                    .map(Hangar::getHangarId)
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
            activeHangar = null;
        }
    }

    public void disconnect(boolean stop) {
        if (heroapi.getMap() != null && heroapi.getMap().getId() > 0
                && (maps == null || maps.contains(heroapi.getMap()))
                && !logout.isVisible() && !heroapi.isMoving()) {
            System.out.println("Disconnecting...");
            logout.show(true);
            if (stop) {
                bot.setRunning(false);
            }
            if (lostConnectionGUI != null && lostConnectionGUI.isVisible()) {
                disconectTime = System.currentTimeMillis();
            }
        }
    }

    public void reloadAfterDisconnect(boolean play) {
        System.out.println("Reload...");
        disconectTime = 0;
        API.handleRefresh();
        if (play) {
            bot.setRunning(true);
        }
        movement.stop(true);
    }

    public void disconnectChangeHangarAndReload(Integer hangar) {
        if (lastStopChangeHangar < System.currentTimeMillis() - 60000 && backpageAPI.isInstanceValid()) {
            if (disconectTime == 0 && !heroapi.isMoving()) {
                lastStopChangeHangar = System.currentTimeMillis();
                disconnect(true);
                Time.sleep(40000);
                changeHangar(hangar, false);
                Time.sleep(40000);
                reloadAfterDisconnect(true);
            }
        }
    }

    public boolean isDisconnect() {
        return lostConnectionGUI != null && lostConnectionGUI.isVisible();
    }

    public void setDisconnectModule(String reason) {
        if (!isDisconnect() && !heroapi.getMap().isGG() && !(main.module instanceof DisconnectModule)) {
            System.out.println("Set Disconnect Module: " + reason);
            bot.setModule(new DisconnectModule(null, reason));
        }
    }

    public boolean changeHangar(Integer hangar, boolean inBase) {
        if ((inBase || (lostConnectionGUI != null && lostConnectionGUI.isVisible())) && hangar != null) {
            System.out.println("Hangar change to: " + hangar);
            if (changeHangar(hangar)) {
                activeHangar = null;
                updateHangarActive();
                return true;
            }
        }
        return false;
    }

    private boolean changeHangar(Integer hangarId) {
        String token = "";
        try {
            token = main.backpage
                    .getReloadToken(backpageAPI.getConnection("indexInternal.es?action=internalDock").getInputStream());
            System.out.println("Reload token: " + token);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (token == null || token.isEmpty()) {
            return false;
        }

        String url = "indexInternal.es?action=internalDock&subAction=changeHangar&hangarId=" + hangarId
                + "&reloadToken=" + token;
        try {
            backpageAPI.getConnection(url, 2000).getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

}
