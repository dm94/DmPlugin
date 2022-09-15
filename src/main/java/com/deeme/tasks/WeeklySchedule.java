package com.deeme.tasks;

import com.deeme.modules.PallladiumHangar;
import com.deeme.modules.temporal.HangarSwitcher;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deeme.types.config.Hour;
import com.deeme.types.config.Profile;
import com.deeme.types.config.WeeklyConfig;
import com.deeme.types.gui.ShipSupplier;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.backpage.hangar.Hangar;
import com.github.manolo8.darkbot.modules.DisconnectModule;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.InstructionProvider;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.game.other.Gui;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.GameScreenAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.utils.Inject;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JLabel;

@Feature(name = "WeeklySchedule", description = "Use different module, map for a weekly schedule")
public class WeeklySchedule implements Task, Configurable<WeeklyConfig>, InstructionProvider {
    protected final PluginAPI api;
    protected final ExtensionsAPI extensionsAPI;
    protected final HeroAPI heroapi;
    protected final BotAPI botApi;
    private WeeklyConfig weeklyConfig;
    private Main main;
    private long nextCheck = 0;
    private boolean changingHangar = false;
    private boolean stopBot = false;
    private Gui lostConnectionGUI;
    Profile profileToUse = null;
    private Integer activeHangar = null;
    private long disconectTime = 0;
    private long nextCheckCurrentHangar = 0;
    private String lastCheck = "";

    @Override
    public String instructions() {
        return "You have 4 different profiles, the module will automatically change its configuration according to the timetable you have set. \n"
                +
                "Remember that you have to configure it in every config you are going to use. \n" +
                "The hangars to be used have to be in favourites";
    }

    public WeeklySchedule(Main main, PluginAPI api) {
        this(main, api, api.requireAPI(AuthAPI.class));
    }

    public JComponent beforeConfig() {
        JLabel hourNow = new JLabel("Last Check: " + lastCheck);
        return hourNow;
    }

    @Inject
    public WeeklySchedule(Main main, PluginAPI api, AuthAPI auth) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog();

        this.main = main;
        this.api = api;
        this.heroapi = api.getAPI(HeroAPI.class);
        this.botApi = api.getAPI(BotAPI.class);
        this.extensionsAPI = api.getAPI(ExtensionsAPI.class);

        GameScreenAPI gameScreenAPI = api.getAPI(GameScreenAPI.class);
        lostConnectionGUI = gameScreenAPI.getGui("lost_connection");

        this.nextCheck = 0;
        this.profileToUse = null;
        this.activeHangar = null;
    }

    @Override
    public void setConfig(ConfigSetting<WeeklyConfig> arg0) {
        this.weeklyConfig = arg0.getValue();
        for (int i = 0; i < 24; i++) {
            String oneHour = String.format("%02d", i);
            Hour hour = this.weeklyConfig.Hours_Changes.get(oneHour);
            if (hour == null) {
                hour = new Hour();
                if (!oneHour.equals("ERROR") && !oneHour.isEmpty()) {
                    weeklyConfig.Hours_Changes.put(oneHour, hour);
                }
            }
        }

        this.weeklyConfig.updateHangarList = true;
    }

    @Override
    public void onBackgroundTick() {
        if (stopBot || changingHangar) {
            onTickTask();
        }
    }

    @Override
    public void onTickTask() {
        if (!weeklyConfig.activate) {
            return;
        }

        tryUpdateHangarList();
        if (heroapi.getMap().isGG()) {
            return;
        }

        if (weeklyConfig.changeHangar && profileToUse != null && profileToUse.hangarId != null
                && !isRunningPalladiumModule()) {
            if (activeHangar != null) {
                if (!profileToUse.hangarId.equals(activeHangar)) {
                    if (isDisconnect()) {
                        if (botApi.getModule().getClass() != HangarSwitcher.class) {
                            botApi.setModule(new HangarSwitcher(main, api, profileToUse.hangarId));
                        }
                        this.activeHangar = null;
                    } else if (botApi.getModule().getClass() != DisconnectModule.class) {
                        botApi.setModule(new DisconnectModule(null, "WeeklySchedule: To change hangar"));
                    }
                    changingHangar = true;
                } else {
                    changingHangar = false;
                }
            } else {
                updateHangarActive();
            }
        } else {
            changingHangar = false;
        }
        if (!changingHangar) {
            updateProfileToUse();

            if (stopBot) {
                if (!heroapi.getMap().isGG() && !isDisconnect()) {
                    disconectTime = System.currentTimeMillis();
                    if (botApi.getModule().getClass() != DisconnectModule.class) {
                        botApi.setModule(new DisconnectModule(null, "Stop by WeeklySchedule"));
                    }
                }
            } else if (disconectTime > 0) {
                disconectTime = 0;
                Main.API.handleRefresh();
                botApi.setRunning(true);
            }
        }
    }

    private void updateProfileToUse() {
        if (nextCheck < System.currentTimeMillis()) {
            LocalDateTime da = LocalDateTime.now();
            int currentHour = da.getHour();
            lastCheck = String.format("%02d", da.getHour()) + ":" + String.format("%02d", da.getMinute());
            Hour hour = this.weeklyConfig.Hours_Changes.get(String.format("%02d", currentHour));
            String profile = "";
            if (hour != null) {
                DayOfWeek currentDay = da.getDayOfWeek();
                if (currentDay == DayOfWeek.MONDAY) {
                    profile = hour.mon;
                } else if (currentDay == DayOfWeek.TUESDAY) {
                    profile = hour.tue;
                } else if (currentDay == DayOfWeek.WEDNESDAY) {
                    profile = hour.wed;
                } else if (currentDay == DayOfWeek.THURSDAY) {
                    profile = hour.thu;
                } else if (currentDay == DayOfWeek.FRIDAY) {
                    profile = hour.fri;
                } else if (currentDay == DayOfWeek.SATURDAY) {
                    profile = hour.sat;
                } else if (currentDay == DayOfWeek.SUNDAY) {
                    profile = hour.sun;
                }

                profileToUse = weeklyConfig.profile1;

                if (profile.contains("Stop")) {
                    stopBot = true;
                    return;
                } else {
                    stopBot = false;
                }

                if (profile.contains("P2")) {
                    profileToUse = weeklyConfig.profile2;
                } else if (profile.contains("P3")) {
                    profileToUse = weeklyConfig.profile3;
                } else if (profile.contains("P4")) {
                    profileToUse = weeklyConfig.profile4;
                }

                setProfile();
                nextCheck = System.currentTimeMillis() + 60000;
            }
        }
    }

    private void setProfile() {
        if (extensionsAPI.getFeatureInfo(this.getClass()).isEnabled()) {
            if (profileToUse != null && heroapi.getMap() != null && !heroapi.getMap().isGG()) {
                main.setConfig(profileToUse.BOT_PROFILE);
            }
        }
    }

    private boolean isRunningPalladiumModule() {
        return botApi.getModule().getClass() == PallladiumHangar.class;
    }

    private void tryUpdateHangarList() {
        if (!weeklyConfig.updateHangarList || changingHangar || !main.backpage.isInstanceValid()) {
            return;
        }

        try {
            this.main.backpage.hangarManager.updateHangarList();
            if (ShipSupplier.updateOwnedShips(
                    this.main.backpage.hangarManager.getHangarList().getData().getRet().getShipInfos())) {
                this.weeklyConfig.updateHangarList = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isDisconnect() {
        return lostConnectionGUI != null && lostConnectionGUI.isVisible();
    }

    private void updateHangarActive() {
        if (nextCheckCurrentHangar > System.currentTimeMillis() || !main.backpage.isInstanceValid()) {
            return;
        }
        try {
            nextCheckCurrentHangar = System.currentTimeMillis() + 30000;
            this.main.backpage.hangarManager.updateHangarList();
            this.main.backpage.hangarManager.updateCurrentHangar();
            activeHangar = this.main.backpage.hangarManager.getHangarList().getData().getRet().getHangars().stream()
                    .filter(Hangar::isActive)
                    .map(Hangar::getHangarId)
                    .findFirst()
                    .orElse(null);

            System.out.println("Current hangar: " + activeHangar);
        } catch (Exception ignored) {
            activeHangar = null;
        }
    }
}
