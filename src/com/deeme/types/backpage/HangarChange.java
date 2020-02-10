package com.deeme.types.backpage;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.backpage.HangarManager;
import com.github.manolo8.darkbot.core.objects.Gui;
import com.github.manolo8.darkbot.modules.DisconnectModule;
import com.github.manolo8.darkbot.utils.Time;

import static com.github.manolo8.darkbot.Main.API;

public class HangarChange {

    public static transient String hangarActive = "";
    private Main main;
    private HangarManager hangarManager;
    public static transient long disconectTime = 0;
    private static transient long lastStopChangeHangar = 0;
    private Gui lostConnection;
    private Gui logout;

    public HangarChange(Main m) {
        this.main = m;
        this.hangarManager = main.backpage.hangarManager;
        this.lostConnection = main.guiManager.lostConnection;
        this.logout = main.guiManager.logout;
    }

    public void updateHangarActive() {
        try {
            hangarManager.updateHangars();
            hangarActive = hangarManager.getActiveHangar();
        } catch (Exception e){}
    }

    public void changeHangar(String hangar, boolean inBase) {
        if ((inBase || lostConnection.visible) && hangar.length() > 0) {
            System.out.println("Hangar change to: " + hangar);
            if (changeHangar(hangar.trim())) {
                hangarActive = "";
                updateHangarActive();
            }
        }
    }

    public void disconnect(boolean stop) {
        if (!lostConnection.visible && !logout.visible && !main.hero.locationInfo.isMoving()) {
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
    }

    public void disconnectChangeHangarAndReload(String hangar) {
        if (lastStopChangeHangar < System.currentTimeMillis() - 60000 && main.backpage.sidStatus().contains("OK")) {
            if (disconectTime == 0 && !main.hero.locationInfo.isMoving()) {
                lastStopChangeHangar = System.currentTimeMillis();
                disconnect(true);
                Time.sleep(40000);
                changeHangar(hangar,false);
                Time.sleep(40000);
                reloadAfterDisconnect(true);
            }
        }
    }

    public boolean isDisconnect() {
        return lostConnection.visible;
    }

    public void setDisconnectModule(String reason) {
        if (!isDisconnect() && !main.hero.map.gg && main.module.getClass().getCanonicalName() != DisconnectModule.class.getCanonicalName()) {
            System.out.println("Set Disconnect Module: "+ reason);
            main.setModule(new DisconnectModule(null,reason));
        }
    }

    private boolean changeHangar(String hangarId) {
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
