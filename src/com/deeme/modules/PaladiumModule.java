package com.deeme.modules;

import com.deeme.types.SharedFunctions;
import com.deeme.types.backpage.HangarChange;
import com.deeme.types.gui.AdvertisingMessage;
import com.deeme.types.gui.ShipSupplier;
import com.github.manolo8.darkbot.Main;
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

import java.util.List;

@Feature(name = "Palladium Hangar", description = "Collect palladium and change hangars to sell")
public class PaladiumModule extends LootNCollectorModule implements Module, Configurable<PaladiumModule.PaladiumConfig>, InstructionProvider {

    private HeroManager hero;
    private Drive drive;
    private StatsManager statsManager;

    private Main main;

    private State currentStatus;

    private PaladiumConfig configPa;

    private Map SELL_MAP;

    private List<BasePoint> bases;
    private OreTradeGui oreTrade;
    private long sellClick;

    private HangarChange hangarChange;
    private String hangarToChange = "";

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
        HANGAR_AND_MAP_BASE ( "Selling palladium"),
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
        DEFENSE_MODE("DEFENSE MODE");;

        private String message;

        State(String message) {
            this.message = message;
        }
    }

    @Override
    public void install(Main main) {
        super.install(main);
        this.main = main;
        this.SELL_MAP = main.starManager.byName("5-2");
        this.hero = main.hero;
        this.drive = main.hero.drive;
        this.config = main.config;
        this.statsManager = main.statsManager;
        this.bases = main.mapManager.entities.basePoints;
        this.oreTrade = main.guiManager.oreTrade;
        this.hangarChange = new HangarChange(main);

        currentStatus = State.WAIT;

        AdvertisingMessage.showAdverMessage();

        if (!main.hero.map.gg) {
            AdvertisingMessage.newUpdateMessage(main.featureRegistry.getFeatureDefinition(this));
        }
        setup();
    }

    @Override
    public void setConfig(PaladiumConfig paladiumConfig) {
        this.configPa = paladiumConfig;
        this.configPa.updateHangarList = true;
        setup();
    }

    private void setup() {
        if (main == null || configPa == null) return;
    }

    @Override
    public boolean canRefresh() {
        return hero.target == null;
    }

    @Override
    public void tickStopped() {
        updateHangarList();
        if (currentStatus == State.SWITCHING_PALA_HANGAR || currentStatus == State.DEPOSIT_FULL_SWITCHING_HANGAR ||
                currentStatus == State.DISCONNECTING || currentStatus == State.SWITCHING_HANGAR || currentStatus == State.RELOAD_GAME) {
            if (hangarChange.hangarActive != null && !hangarChange.hangarActive.isEmpty()) {
                if (hangarChange.disconectTime == 0 && !hangarChange.isDisconnect()) {
                    currentStatus = State.DISCONNECTING;
                    hangarChange.disconnect(true);
                } else if (hangarChange.isDisconnect() && currentStatus == State.DISCONNECTING && hangarChange.disconectTime <= System.currentTimeMillis() - 30000) {
                    currentStatus = State.SWITCHING_HANGAR;
                    hangarChange.changeHangar(hangarToChange,false);
                } else if (hangarChange.disconectTime <= System.currentTimeMillis() - 40000 && currentStatus == State.SWITCHING_HANGAR) {
                    currentStatus = State.RELOAD_GAME;
                    hangarChange.reloadAfterDisconnect(true);
                }
            }
            if (!hangarChange.isDisconnect() && currentStatus != State.DISCONNECTING && main.pingManager.ping > 0) {
                main.setRunning(true);
                hangarChange.disconectTime = 0;
                drive.stop(true);
            }
        }
    }


    public static class PaladiumConfig {

        @Option(value = "Update HangarList", description = "Mark it to update the hangar list")
        public boolean updateHangarList = true;

        @Option(value = "Go portal to change", description = "Go to the portal to change the hangar")
        public boolean goPortalChange = true;

        @Option(value = "Hangar Palladium", description = "Ship 5-3 Hangar ID")
        @Editor(JListField.class)
        @Options(ShipSupplier.class)
        public String hangarPalladium = "";

        @Option(value = "Hangar Base", description = "Ship 5-2 Hangar ID")
        @Editor(JListField.class)
        @Options(ShipSupplier.class)
        public String hangarBase = "";

    }

    @Override
    public String status() {
        return  currentStatus.message + " | " + super.status();
    }

    @Override
    public String stoppedStatus() {
        return currentStatus.message;
    }

    @Override
    public void tick() {
        if (!AdvertisingMessage.hasAccepted) {
            currentStatus = State.NO_ACCEPT;
            return;
        }
        updateHangarList();

        if (hangarChange.hangarActive != null && !hangarChange.hangarActive.isEmpty()) {
            if (statsManager.deposit >= statsManager.depositTotal) {
                if (hangarChange.hangarActive.equals(configPa.hangarBase)) {
                    this.currentStatus = State.HANGAR_AND_MAP_BASE;
                    sell();
                } else if (canBeDisconnected()) {
                    this.currentStatus = State.DEPOSIT_FULL_SWITCHING_HANGAR;
                    hangarToChange = configPa.hangarBase;
                    main.setRunning(false);
                } else if (!main.guiManager.lostConnection.visible && !main.guiManager.logout.visible) {
                    super.tick();
                    currentStatus = State.SEARCHING_PORTALS;
                }

            } else if (hangarChange.hangarActive.equals(configPa.hangarPalladium) &&
                    !main.guiManager.lostConnection.visible && !main.guiManager.logout.visible) {
                if (hero.map.id == 93) {
                    this.currentStatus = State.LOOT_PALADIUM;
                    super.tick();
                } else if (!main.guiManager.lostConnection.visible && !main.guiManager.logout.visible) {
                    this.currentStatus = State.HANGAR_PALA_OTHER_MAP;
                    hero.roamMode();
                    this.main.setModule(new MapModule()).setTarget(this.main.starManager.byId(93));
                }
            } else if (configPa.hangarPalladium.length()>0){
                this.currentStatus = State.SWITCHING_PALA_HANGAR;
                hangarToChange = configPa.hangarPalladium;
                main.setRunning(false);
            }
        } else {
            super.canRefresh();
            hangarChange.updateHangarActive();
            currentStatus = State.LOADING_HANGARS;
        }
    }

    private boolean canBeDisconnected() {
        if (configPa.goPortalChange) {
            return super.canRefresh();
        } else if(hero.health.hpPercent() > 0.90 && SharedFunctions.getAttacker(hero,main,hero,null) != null) {
            return true;
        }

        return false;
    }

    private void sell() {
        pet.setEnabled(false);
        if (hero.map != SELL_MAP) main.setModule(new MapModule()).setTarget(SELL_MAP);
        else bases.stream().filter(b -> b.locationInfo.isLoaded()).findFirst().ifPresent(base -> {
            if (drive.movingTo().distance(base.locationInfo.now) > 200) { // Move to base
                double angle = base.locationInfo.now.angle(hero.locationInfo.now) + Math.random() * 0.2 - 0.1;
                drive.move(Location.of(base.locationInfo.now, angle, 100 + (100 * Math.random())));
            } else if (!hero.locationInfo.isMoving() && oreTrade.showTrade(true,base)
                    && System.currentTimeMillis() - 60_000 > sellClick) {
                oreTrade.sellOre(OreTradeGui.Ore.PALLADIUM);
                sellClick = System.currentTimeMillis();
            }
        });
    }

    private void updateHangarList() {
        if (configPa.updateHangarList) {
            currentStatus = State.LOADING_HANGARS;
            String sid = statsManager.sid, instance = statsManager.instance;
            if (!(sid == null || sid.isEmpty() || instance == null || instance.isEmpty())) {
                main.backpage.hangarManager.updateHangarData(60000);
                configPa.updateHangarList = !ShipSupplier.updateOwnedShips(main.backpage.hangarManager.getShipInfos());
            }
        }
    }

}
