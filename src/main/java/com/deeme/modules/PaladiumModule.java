package com.deeme.modules;

import com.deeme.types.SharedFunctions;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.HangarChanger;
import com.deeme.types.gui.ShipSupplier;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.backpage.HangarManager;
import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.config.types.Options;
import com.github.manolo8.darkbot.core.entities.BasePoint;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.core.itf.InstructionProvider;
import com.github.manolo8.darkbot.core.itf.Module;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.core.manager.StatsManager;
import com.github.manolo8.darkbot.core.objects.Map;
import com.github.manolo8.darkbot.core.objects.OreTradeGui;
import com.github.manolo8.darkbot.core.utils.Drive;
import com.github.manolo8.darkbot.core.utils.Location;
import com.github.manolo8.darkbot.extensions.features.Feature;
import com.github.manolo8.darkbot.gui.tree.components.JListField;
import com.github.manolo8.darkbot.modules.LootNCollectorModule;
import com.github.manolo8.darkbot.modules.MapModule;

import java.util.Arrays;
import java.util.List;

@Feature(name = "Palladium Hangar", description = "Collect palladium and change hangars to sell")
public class PaladiumModule extends LootNCollectorModule implements Module, Configurable<PaladiumModule.PalladiumConfig>, InstructionProvider {

    private HeroManager hero;
    private Drive drive;
    private StatsManager statsManager;

    private Main main;

    private State currentStatus;

    private PalladiumConfig configPa;

    private Map SELL_MAP;
    private Map ACTIVE_MAP;

    private List<BasePoint> bases;
    private OreTradeGui oreTrade;
    private long sellClick;

    private HangarManager hangarManager;
    private HangarChanger hangarChanger;
    private Integer hangarToChange = null;
    private int cargos = 0;

    @Override
    public String instructions() {
        return "Palladium Hangar Module: \n"+
                "It is necessary that a portal allows refresh to change the hangar \n" +
                "Use in 5-2 a ship with less cargo than 5-3 \n" +
                "Select palladium hangar and 5-2 hangar \n" +
                "If the hangar list does not appear, click on \"Update HangarList\", close the config windows and it will be updated within minutes.";
    }

    private enum State {
        WAIT ("Waiting"),
        HANGAR_AND_MAP_BASE ("Selling palladium"),
        DEPOSIT_FULL_SWITCHING_HANGAR("Deposit full, switching hangar"),
        SWITCHING_HANGAR("Changing the hangar"),
        LOOT_PALADIUM("Loot paladium"),
        HANGAR_PALA_OTHER_MAP("Hangar paladium - To 5-3"),
        SWITCHING_PALA_HANGAR("Switching to the palladium hangar"),
        DISCONNECTING("Disconnecting"),
        RELOAD_GAME("Reloading the game"),
        NO_ACCEPT("You haven't opened the link"),
        LOADING_HANGARS("Waiting - Loading hangars"),
        SEARCHING_PORTALS("Looking for a portal to change hangar"),
        DEFENSE_MODE("DEFENSE MODE");

        private final String message;

        State(String message) {
            this.message = message;
        }
    }

    @Override
    public void install(Main main) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) return;
        VerifierChecker.checkAuthenticity();
        super.install(main);
        this.main = main;
        this.SELL_MAP = main.starManager.byName("5-2");
        this.ACTIVE_MAP = main.starManager.byName("5-3");
        this.hero = main.hero;
        this.drive = main.hero.drive;
        this.config = main.config;
        this.statsManager = main.statsManager;
        this.bases = main.mapManager.entities.basePoints;
        this.oreTrade = main.guiManager.oreTrade;
        this.hangarManager = main.backpage.hangarManager;
        this.hangarChanger = new HangarChanger(main, SELL_MAP, ACTIVE_MAP);
        currentStatus = State.WAIT;
    }

    @Override
    public void setConfig(PalladiumConfig palladiumConfig) {
        this.configPa = palladiumConfig;
    }

    @Override
    public boolean canRefresh() {
        return hero.target == null;
    }

    @Override
    public void tickStopped() {
        tryUpdateHangarList();
        if (currentStatus == State.SWITCHING_PALA_HANGAR || currentStatus == State.DEPOSIT_FULL_SWITCHING_HANGAR ||
                currentStatus == State.DISCONNECTING || currentStatus == State.SWITCHING_HANGAR || currentStatus == State.RELOAD_GAME) {
            if (hangarChanger.activeHangar != null) {
                if (hangarChanger.disconectTime == 0 && !hangarChanger.isDisconnect()) {
                    currentStatus = State.DISCONNECTING;
                    hangarChanger.disconnect(true);
                } else if (hangarChanger.isDisconnect() && currentStatus == State.DISCONNECTING && hangarChanger.disconectTime <= System.currentTimeMillis() - 30000) {
                    currentStatus = State.SWITCHING_HANGAR;
                    hangarChanger.changeHangar(hangarToChange, false);
                } else if (hangarChanger.disconectTime <= System.currentTimeMillis() - 40000 && currentStatus == State.SWITCHING_HANGAR) {
                    currentStatus = State.RELOAD_GAME;
                    hangarChanger.reloadAfterDisconnect(true);
                }
            }
            if (!hangarChanger.isDisconnect() && currentStatus != State.DISCONNECTING && (hero.map == SELL_MAP || hero.map == ACTIVE_MAP)) {
                main.setRunning(true);
                hangarChanger.disconectTime = 0;
                drive.stop(true);
            }
        }
    }


    public static class PalladiumConfig {

        @Option(value = "Update HangarList", description = "Mark it to update the hangar list")
        public transient boolean updateHangarList = true;

        @Option(value = "Travel to portal before switch", description = "Go to the portal to change the hangar")
        public boolean goPortalChange = true;

        @Option(value = "Collecting Hangar (5-3)", description = "Ship 5-3 Hangar ID")
        @Editor(JListField.class)
        @Options(ShipSupplier.class)
        public Integer collectHangar = -1;

        @Option(value = "Selling Hangar (5-2)", description = "Ship 5-2 Hangar ID")
        @Editor(JListField.class)
        @Options(ShipSupplier.class)
        public Integer sellHangar = -1;

    }

    @Override
    public String status() {
        return  currentStatus.message + " | " + cargos + " | " + super.status();
    }

    @Override
    public String stoppedStatus() {
        return currentStatus.message;
    }

    @Override
    public void tick() {
        tryUpdateHangarList();

        if (hangarChanger.activeHangar == null) {
            currentStatus = State.LOADING_HANGARS;
            hangarChanger.updateHangarActive();
            return;
        }

        if (statsManager.deposit >= statsManager.depositTotal) {
            if (hangarChanger.activeHangar.equals(configPa.sellHangar)) {
                this.currentStatus = State.HANGAR_AND_MAP_BASE;
                sell();
            } else if (canBeDisconnected()) {
                this.currentStatus = State.DEPOSIT_FULL_SWITCHING_HANGAR;
                hangarToChange = configPa.sellHangar;
                main.setRunning(false);
            } else if (!main.guiManager.lostConnection.visible && !main.guiManager.logout.visible) {
                super.tick();
                currentStatus = State.SEARCHING_PORTALS;
            }

        } else if (hangarChanger.activeHangar.equals(configPa.collectHangar) &&
                !main.guiManager.lostConnection.visible && !main.guiManager.logout.visible) {
            if (hero.map.id == 93) {
                this.currentStatus = State.LOOT_PALADIUM;
                super.tick();
            } else {
                this.currentStatus = State.HANGAR_PALA_OTHER_MAP;
                hero.roamMode();
                this.main.setModule(new MapModule()).setTarget(this.main.starManager.byId(93));
            }
        } else if (configPa.collectHangar != null) {
            this.currentStatus = State.SWITCHING_PALA_HANGAR;
            hangarToChange = configPa.collectHangar;
            main.setRunning(false);
        }
    }

    private boolean canBeDisconnected() {
        if (configPa.goPortalChange) return super.canRefresh();
        else return hero.health.hpPercent() >= 0.9 && SharedFunctions.getAttacker(hero, main, hero, null) == null;
    }

    private void sell() {
        pet.setEnabled(false);
        if (hero.map != SELL_MAP) main.setModule(new MapModule()).setTarget(SELL_MAP);
        else bases.stream().filter(b -> b.locationInfo.isLoaded()).findFirst().ifPresent(base -> {
            if (drive.movingTo().distance(base.locationInfo.now) > 200) { // Move to base
                double angle = Math.random() * Math.PI * 2;
                double distance = 100 + Math.random() * 100;
                drive.move(Location.of(base.locationInfo.now, angle, distance));
            } else if (!hero.locationInfo.isMoving() && oreTrade.showTrade(true, base)
                    && System.currentTimeMillis() - 60_000 > sellClick) {
                oreTrade.sellOre(OreTradeGui.Ore.PALLADIUM);
                sellClick = System.currentTimeMillis();
                cargos++;
            }
        });
    }

    private void tryUpdateHangarList() {
        if (!configPa.updateHangarList) return;
        currentStatus = State.LOADING_HANGARS;

        try {
            hangarManager.updateHangarList();
            if (ShipSupplier.updateOwnedShips(hangarManager.getHangarList().getData().getRet().getShipInfos()))
                configPa.updateHangarList = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
