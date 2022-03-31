package com.deeme.types.backpage;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.backpage.HangarManager;
import com.github.manolo8.darkbot.backpage.hangar.Hangar;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.core.objects.Gui;
import com.github.manolo8.darkbot.core.objects.Map;
import com.github.manolo8.darkbot.modules.DisconnectModule;
import com.github.manolo8.darkbot.utils.Time;

import java.util.Arrays;
import java.util.List;

import static com.github.manolo8.darkbot.Main.API;

public class HangarChanger {

    public Integer activeHangar = null;
    private final Main main;
    private final HangarManager hangarManager;
    public long disconectTime = 0;
    private long lastStopChangeHangar = 0;
    private final Gui lostConnection;
    private final Gui logout;
    private final HeroManager hero;
    private final List<Map> maps;


    public HangarChanger(Main m, Map... maps) {
        this.main = m;
        this.hangarManager = main.backpage.hangarManager;
        this.lostConnection = main.guiManager.lostConnection;
        this.logout = main.guiManager.logout;
        this.maps = maps == null || maps.length == 0 ? null : Arrays.asList(maps);
        this.hero = main.hero;
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
        if (hero.map.id > 0 && (maps == null || maps.contains(hero.map))
                && !logout.visible && !main.hero.locationInfo.isMoving()) {
            System.out.println("Disconnecting...");
            logout.show(true);
            if (stop) {
                main.setRunning(false);
            }
            if (lostConnection.visible) {
                disconectTime = System.currentTimeMillis();
            }
        }
    }

    public void reloadAfterDisconnect(boolean play) {
        System.out.println("Reload...");
        disconectTime = 0;
        API.handleRefresh();
        if (play) {
            main.setRunning(true);
        }
        main.hero.drive.stop(true);
        main.hero.drive.checkMove();
    }

    public void disconnectChangeHangarAndReload(Integer hangar) {
        if (lastStopChangeHangar < System.currentTimeMillis() - 60000 && main.backpage.sidStatus().contains("OK")) {
            if (disconectTime == 0 && !main.hero.locationInfo.isMoving()) {
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
        return lostConnection.visible;
    }

    public void setDisconnectModule(String reason) {
        if (!isDisconnect() && !main.hero.map.gg && !(main.module instanceof DisconnectModule)) {
            System.out.println("Set Disconnect Module: "+ reason);
            main.setModule(new DisconnectModule(null, reason));
        }
    }

    public boolean changeHangar(Integer hangar, boolean inBase) {
        if ((inBase || lostConnection.visible) && hangar != null) {
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
            token = main.backpage.getReloadToken(main.backpage.getConnection("indexInternal.es?action=internalDock").getInputStream());
            System.out.println("Reload token: "+ token);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (token == null || token.isEmpty()) return false;

        String url = "indexInternal.es?action=internalDock&subAction=changeHangar&hangarId=" + hangarId + "&reloadToken="+token;
        try {
            main.backpage.getConnection(url, 2000).getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

}
