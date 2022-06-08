package com.deeme.modules;

import com.deeme.types.SharedFunctions;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.HangarChanger;
import com.deeme.types.config.PalladiumConfig;
import com.deeme.types.gui.ShipSupplier;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.entities.BasePoint;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.core.objects.OreTradeGui;
import com.github.manolo8.darkbot.extensions.features.Feature;

import com.github.manolo8.darkbot.modules.LootNCollectorModule;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.game.entities.Station;
import eu.darkbot.api.game.entities.Station.Refinery;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.game.other.Location;
import eu.darkbot.api.managers.AttackAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.managers.OreAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.managers.OreAPI.Ore;
import eu.darkbot.api.managers.StarSystemAPI.MapNotFoundException;
import eu.darkbot.shared.modules.MapModule;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Feature(name = "Palladium Hangar", description = "Collect palladium and change hangars to sell")
public class PaladiumModule extends LootNCollectorModule implements Configurable<PalladiumConfig> {
    protected PluginAPI api;
    protected OreAPI oreApi;
    protected HeroAPI heroapi;
    protected AttackAPI attackApi;
    protected MovementAPI movement;
    protected StatsAPI stats;
    private GameMap SELL_MAP;
    private GameMap ACTIVE_MAP;
    private Collection<? extends Station> bases;

    private Main main;
    private OreTradeGui oreTradeOld;
    private List<BasePoint> basesOld;

    private State currentStatus;

    private PalladiumConfig configPa;

    private long sellClick;
    private HangarChanger hangarChanger;
    private Integer hangarToChange = null;
    private int cargos = 0;
    private boolean firstTick = true;

    private long aditionalWaitingTime = 0;
    private State lastStatus;

    private enum State {
        WAIT("Waiting"),
        HANGAR_AND_MAP_BASE("Selling palladium"),
        DEPOSIT_FULL_SWITCHING_HANGAR("Deposit full, switching hangar"),
        SWITCHING_HANGAR("Changing the hangar"),
        LOOT_PALADIUM("Loot paladium"),
        HANGAR_PALA_OTHER_MAP("Hangar paladium - To 5-3"),
        SWITCHING_PALA_HANGAR("Switching to the palladium hangar"),
        DISCONNECTING("Disconnecting"),
        RELOAD_GAME("Reloading the game"),
        LOADING_HANGARS("Waiting - Loading hangars"),
        SEARCHING_PORTALS("Looking for a portal to change hangar"),
        DEFENSE_MODE("DEFENSE MODE"),
        WAITING_HANGARS("Waiting - For change hangars"),
        HANGAR_ERROR("Error when changing hangar"),
        HANGAR_ERROR_2("Error when changing hangar - Attempt 2"),
        SID_KO("Error - SID KO - Reconnecting the game"),
        NO_HANGAR_ERROR("Error - Hangars not configured");

        private final String message;

        State(String message) {
            this.message = message;
        }
    }

    @Override
    public void install(Main main) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            return;
        VerifierChecker.checkAuthenticity();
        super.install(main);
        this.main = main;
        this.hangarChanger = new HangarChanger(main, SELL_MAP, ACTIVE_MAP);
        this.oreTradeOld = main.guiManager.oreTrade;
        this.basesOld = main.mapManager.entities.basePoints;

        this.api = main.pluginAPI.getAPI(PluginAPI.class);
        this.heroapi = api.getAPI(HeroAPI.class);
        this.attackApi = api.getAPI(AttackAPI.class);
        this.movement = api.getAPI(MovementAPI.class);
        this.oreApi = api.getAPI(OreAPI.class);
        this.stats = api.getAPI(StatsAPI.class);

        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        this.bases = entities.getStations();

        StarSystemAPI starSystem = api.getAPI(StarSystemAPI.class);
        try {
            this.SELL_MAP = starSystem.getByName("5-2");
        } catch (MapNotFoundException e) {
            this.SELL_MAP = main.starManager.byName("5-2");
        }
        try {
            this.ACTIVE_MAP = starSystem.getByName("5-3");
        } catch (MapNotFoundException e) {
            this.ACTIVE_MAP = main.starManager.byName("5-3");
        }

        this.currentStatus = State.WAIT;
        this.lastStatus = State.WAIT;
    }

    @Override
    public void setConfig(PalladiumConfig palladiumConfig) {
        this.configPa = palladiumConfig;
    }

    @Override
    public boolean canRefresh() {
        return !heroapi.isMoving() && !attackApi.hasTarget();
    }

    @Override
    public void tickStopped() {
        if (main.repairManager.isDead()) {
            main.guiManager.tryRevive();
        }
        if (lastStatus != currentStatus) {
            lastStatus = currentStatus;
            aditionalWaitingTime = System.currentTimeMillis() + (configPa.aditionalWaitingTime * 1000);
        }
        if (aditionalWaitingTime > System.currentTimeMillis()) {
            return;
        }

        tryUpdateHangarList();

        if (configPa.collectHangar == null || configPa.sellHangar == null) {
            currentStatus = State.NO_HANGAR_ERROR;
            return;
        }

        if (currentStatus == State.SID_KO) {
            hangarChanger.reloadAfterDisconnect(true);
            return;
        }

        if (currentStatus == State.SWITCHING_PALA_HANGAR || currentStatus == State.DEPOSIT_FULL_SWITCHING_HANGAR ||
                currentStatus == State.DISCONNECTING || currentStatus == State.SWITCHING_HANGAR
                || currentStatus == State.RELOAD_GAME ||
                currentStatus == State.WAITING_HANGARS || currentStatus == State.HANGAR_ERROR
                || currentStatus == State.HANGAR_ERROR_2) {
            if (hangarChanger.activeHangar != null) {
                if (hangarChanger.disconectTime == 0 && !hangarChanger.isDisconnect()) {
                    if (!canBeDisconnected()) {
                        main.setRunning(true);
                    } else {
                        currentStatus = State.DISCONNECTING;
                        hangarChanger.disconnect(true);
                        hangarChanger.disconectTime = System.currentTimeMillis();
                    }
                } else if (hangarChanger.isDisconnect() &&
                        (currentStatus == State.DISCONNECTING || currentStatus == State.HANGAR_ERROR
                                || currentStatus == State.HANGAR_ERROR_2)
                        &&
                        hangarChanger.disconectTime <= System.currentTimeMillis() - 30000) {
                    if (hangarChanger.changeHangar(hangarToChange, false)) {
                        currentStatus = State.SWITCHING_HANGAR;
                    } else {
                        if (currentStatus == State.DISCONNECTING) {
                            currentStatus = State.HANGAR_ERROR;
                        } else if (currentStatus == State.HANGAR_ERROR) {
                            currentStatus = State.HANGAR_ERROR_2;
                        } else if (currentStatus == State.HANGAR_ERROR_2) {
                            currentStatus = State.SID_KO;
                        }
                        hangarChanger.disconectTime = System.currentTimeMillis();
                    }
                } else if (hangarChanger.disconectTime <= System.currentTimeMillis() - 40000
                        && currentStatus == State.SWITCHING_HANGAR) {
                    currentStatus = State.WAITING_HANGARS;
                } else if (hangarChanger.disconectTime <= System.currentTimeMillis() - 50000
                        && currentStatus == State.WAITING_HANGARS) {
                    currentStatus = State.RELOAD_GAME;
                    hangarChanger.reloadAfterDisconnect(true);
                } else if (!hangarChanger.isDisconnect() && currentStatus == State.DISCONNECTING
                        && hangarChanger.disconectTime <= System.currentTimeMillis() - 60000) {
                    hangarChanger.disconnect(true);
                    hangarChanger.disconectTime = System.currentTimeMillis();
                }
            } else if (currentStatus != State.LOADING_HANGARS) {
                currentStatus = State.LOADING_HANGARS;
                hangarChanger.updateHangarActive();
                hangarChanger.reloadAfterDisconnect(true);
                return;
            }
            if (!hangarChanger.isDisconnect() && currentStatus != State.DISCONNECTING
                    && (heroapi.getMap() == SELL_MAP || heroapi.getMap() == ACTIVE_MAP)) {
                currentStatus = State.WAIT;
                this.firstTick = true;
                main.setRunning(true);
                hangarChanger.disconectTime = 0;
                movement.stop(true);
            }
        }
    }

    @Override
    public String status() {
        return currentStatus.message + " | " + cargos + " | " + super.status();
    }

    @Override
    public String stoppedStatus() {
        return currentStatus.message;
    }

    @Override
    public void tick() {
        if (lastStatus != currentStatus) {
            lastStatus = currentStatus;
            aditionalWaitingTime = System.currentTimeMillis() + (configPa.aditionalWaitingTime * 1000);
        }
        if (aditionalWaitingTime > System.currentTimeMillis()) {
            return;
        }
        tryUpdateHangarList();

        if (State.RELOAD_GAME == currentStatus) {
            this.firstTick = true;
        }

        if (hangarChanger.activeHangar == null) {
            currentStatus = State.LOADING_HANGARS;
            hangarChanger.updateHangarActive();
            return;
        }

        if (this.firstTick) {
            this.firstTick = false;
            return;
        }

        if (hangarChanger.activeHangar.equals(configPa.sellHangar) && oreApi.getAmount(Ore.PALLADIUM) > 15) {
            this.currentStatus = State.HANGAR_AND_MAP_BASE;
            sellOld();
        } else if (stats.getCargo() >= stats.getMaxCargo()
                && oreApi.getAmount(Ore.PALLADIUM) > 15) {
            if (hangarChanger.activeHangar.equals(configPa.sellHangar)) {
                this.currentStatus = State.HANGAR_AND_MAP_BASE;
                sellOld();
            } else if (configPa.sellHangar != null && hangarChanger.activeHangar != null
                    && configPa.sellHangar != hangarChanger.activeHangar && canBeDisconnected()) {
                this.currentStatus = State.DEPOSIT_FULL_SWITCHING_HANGAR;
                hangarToChange = configPa.sellHangar;
                main.setRunning(false);
            } else if (!hangarChanger.lostConnectionGUI.isVisible() && !hangarChanger.logout.isVisible()) {
                super.tick();
                currentStatus = State.SEARCHING_PORTALS;
            }
        } else if (hangarChanger.activeHangar.equals(configPa.collectHangar) &&
                !hangarChanger.lostConnectionGUI.isVisible() && !hangarChanger.logout.isVisible()) {
            if (heroapi.getMap() != null && heroapi.getMap().getId() == this.ACTIVE_MAP.getId()) {
                this.currentStatus = State.LOOT_PALADIUM;
                super.tick();
            } else {
                this.currentStatus = State.HANGAR_PALA_OTHER_MAP;
                heroapi.setRoamMode();
                if (configPa.sellOnDie && oreApi.getAmount(Ore.PALLADIUM) > 15) {
                    sellOld();
                } else if (System.currentTimeMillis() - 500 > sellClick
                        && oreTradeOld.showTrade(false, (BasePoint) null)) {
                    this.main.setModule(api.requireInstance(MapModule.class)).setTarget(this.ACTIVE_MAP);
                }
            }
        } else if (configPa.collectHangar != null && hangarChanger.activeHangar != null
                && configPa.collectHangar != hangarChanger.activeHangar) {
            this.currentStatus = State.SWITCHING_PALA_HANGAR;
            hangarToChange = configPa.collectHangar;
            main.setRunning(false);
        }
    }

    private boolean canBeDisconnected() {
        if (configPa.goPortalChange)
            return super.canRefresh();
        return !heroapi.isAttacking() && !SharedFunctions.hasAttacker(heroapi, main);
    }

    private void sell() {
        pet.setEnabled(false);
        if (heroapi.getMap() != SELL_MAP) {
            this.main.setModule(api.requireInstance(MapModule.class)).setTarget(this.SELL_MAP);
        } else {
            bases.stream().filter(b -> b instanceof Refinery && b.getLocationInfo().isInitialized())
                    .findFirst().map(Refinery.class::cast).ifPresent(base -> {
                        if (heroapi.distanceTo(base.getLocationInfo()) > 200) {
                            double angle = base.angleTo(heroapi) + Math.random() * 0.2 - 0.1;
                            movement.moveTo(Location.of(base.getLocationInfo().getCurrent(), angle,
                                    100 + (100 * Math.random())));
                        } else if (!heroapi.isMoving() && oreApi.showTrade(true, base)
                                && System.currentTimeMillis() - 60_000 > sellClick) {
                            oreApi.sellOre(Ore.PALLADIUM);
                            sellClick = System.currentTimeMillis();
                            if (oreApi.getAmount(Ore.PALLADIUM) < 15) {
                                cargos++;
                                oreApi.showTrade(false, base);
                            }
                        }
                    });
        }
    }

    private void sellOld() {
        pet.setEnabled(false);
        if (heroapi.getMap() != SELL_MAP)
            this.main.setModule(api.requireInstance(MapModule.class)).setTarget(this.SELL_MAP);
        else
            basesOld.stream().filter(b -> b.getLocationInfo().isInitialized()).findFirst().ifPresent(base -> {
                if (heroapi.distanceTo(base.getLocationInfo().getCurrent()) > 200) {
                    double angle = base.getLocationInfo().getCurrent().angleTo(heroapi.getLocationInfo().getCurrent())
                            + Math.random() * 0.2 - 0.1;
                    movement.moveTo(
                            Location.of(base.getLocationInfo().getCurrent(), angle, 100 + (100 * Math.random())));
                } else if (!hero.locationInfo.isMoving() && oreTradeOld.showTrade(true, base)
                        && System.currentTimeMillis() - 60_000 > sellClick) {
                    oreTradeOld.sellOre(OreTradeGui.Ore.PALLADIUM);
                    sellClick = System.currentTimeMillis();
                    cargos++;
                }
            });
    }

    private void tryUpdateHangarList() {
        if (!configPa.updateHangarList) {
            return;
        }
        if (!main.backpage.isInstanceValid()) {
            return;
        }

        currentStatus = State.LOADING_HANGARS;

        try {
            this.main.backpage.hangarManager.updateHangarList();
            if (ShipSupplier.updateOwnedShips(
                    this.main.backpage.hangarManager.getHangarList().getData().getRet().getShipInfos())) {
                configPa.updateHangarList = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
