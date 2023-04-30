package com.deeme.modules.temporal;

import com.deeme.types.SharedFunctions;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.backpage.HangarManager;
import com.github.manolo8.darkbot.backpage.hangar.Hangar;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.game.other.Gui;
import eu.darkbot.api.managers.BackpageAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.GameScreenAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.TemporalModule;

public class HangarSwitcher extends TemporalModule {
    protected final PluginAPI api;
    protected final HeroAPI heroapi;
    protected final BackpageAPI backpageAPI;

    protected final Gui lostConnectionGUI;
    protected final Gui logout;

    private Integer activeHangar = null;
    private Integer hangarToChage = null;
    private final Main main;
    private final HangarManager hangarManager;
    private State currentStatus;

    private long waitinUntil = 0;
    private int hangarTry = 0;
    private boolean hangarChanged = false;
    private int checkCount = 0;
    private int additionalTime = 0;

    private enum State {
        WAIT("Waiting"),
        SWITCHING_HANGAR("Changing the hangar"),
        HANGAR_CHANGED("Hangar changed"),
        DISCONNECTING("Disconnecting"),
        RELOAD_GAME("Reloading the game"),
        LOADING_HANGARS("Waiting - Loading hangars"),
        WAITING_HANGARS("Waiting - For change hangars"),
        SID_KO("Error - SID KO - Reconnecting the game"),
        NO_HANGAR_ERROR("Error - Hangars not configured");

        private final String message;

        State(String message) {
            this.message = message;
        }
    }

    public HangarSwitcher(Main m, PluginAPI api, Integer hangar, Integer additionalTime) {
        this(m, api, api.requireAPI(BotAPI.class), hangar);
        this.additionalTime = additionalTime;
    }

    public HangarSwitcher(Main m, PluginAPI api, Integer hangar) {
        this(m, api, api.requireAPI(BotAPI.class), hangar);
    }

    @Inject
    public HangarSwitcher(Main m, PluginAPI api, BotAPI bot, Integer hangar) {
        super(bot);
        this.main = m;
        this.hangarManager = main.backpage.hangarManager;

        this.api = api;
        this.heroapi = api.getAPI(HeroAPI.class);
        this.backpageAPI = api.getAPI(BackpageAPI.class);
        this.hangarToChage = hangar;
        this.checkCount = 0;

        GameScreenAPI gameScreenAPI = api.getAPI(GameScreenAPI.class);
        this.lostConnectionGUI = gameScreenAPI.getGui("lost_connection");
        this.logout = gameScreenAPI.getGui("logout");

        this.currentStatus = State.WAIT;
    }

    @Override
    public String getStatus() {
        return "Hangar Switcher | " + currentStatus.message + waitingTime() + " | Attempts: " + hangarTry;
    }

    @Override
    public String getStoppedStatus() {
        return this.getStatus();
    }

    @Override
    public void goBack() {
        bot.setRunning(true);
        bot.handleRefresh();
        super.goBack();
    }

    @Override
    public void onTickStopped() {
        this.onTickModule();
    }

    @Override
    public void onTickModule() {
        try {
            if (hangarToChage == null) {
                this.currentStatus = State.NO_HANGAR_ERROR;
                goBack();
            }
            if (waitinUntil > System.currentTimeMillis()) {
                return;
            }
            if (activeHangar == null) {
                updateHangarActive();
            } else if (!activeHangar.equals(hangarToChage)) {
                if (!heroapi.isMoving()) {
                    if (isDisconnect()) {
                        if (!this.backpageAPI.isInstanceValid() || !this.backpageAPI.getSidStatus().contains("OK")) {
                            this.currentStatus = State.SID_KO;
                            addWaitingTime(60);
                        } else {
                            if (hangarChanged) {
                                addWaitingTime(10);
                                activeHangar = null;
                                checkCount++;
                                if (checkCount > 6) {
                                    hangarChanged = false;
                                }
                            } else if (hangarTry >= 3) {
                                this.currentStatus = State.RELOAD_GAME;
                                goBack();
                            } else {
                                this.currentStatus = State.SWITCHING_HANGAR;
                                if (hangarManager.changeHangar(String.valueOf(hangarToChage))) {
                                    hangarChanged = true;
                                    this.currentStatus = State.HANGAR_CHANGED;
                                    addWaitingTime(10);
                                } else if (hangarTry < 4) {
                                    hangarTry++;
                                    addWaitingTime(20);
                                }
                                this.activeHangar = null;
                            }
                        }
                    } else if (!heroapi.isAttacking() && !SharedFunctions.hasAttacker(heroapi, main)) {
                        disconnect();
                    } else {
                        goBack();
                    }
                } else {
                    goBack();
                }
            } else {
                goBack();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addWaitingTime(Integer time) {
        waitinUntil = System.currentTimeMillis() + ((time + additionalTime) * 1000);
    }

    private String waitingTime() {
        if (waitinUntil > System.currentTimeMillis()) {
            return " | Waiting " + (System.currentTimeMillis() - waitinUntil);
        }
        return "";
    }

    private boolean isDisconnect() {
        return lostConnectionGUI != null && lostConnectionGUI.isVisible();
    }

    private void disconnect() {
        this.currentStatus = State.DISCONNECTING;
        bot.setRunning(false);
        if (heroapi.getMap() != null && heroapi.getMap().getId() > 0
                && !logout.isVisible() && !heroapi.isMoving()) {
            logout.setVisible(true);
        }
    }

    private void updateHangarActive() {
        this.currentStatus = State.LOADING_HANGARS;
        try {
            hangarManager.updateHangarList();
            activeHangar = hangarManager.getHangarList().getData().getRet().getHangars().stream()
                    .filter(Hangar::isActive)
                    .map(Hangar::getHangarId)
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
            addWaitingTime(20);
            activeHangar = null;
        }
    }
}
