package com.deeme.modules;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.deeme.modules.temporal.HangarSwitcher;
import com.deeme.types.SharedFunctions;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deeme.types.config.PalladiumConfig;
import com.deeme.types.gui.ShipSupplier;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.backpage.hangar.Hangar;
import com.github.manolo8.darkbot.core.entities.BasePoint;
import com.github.manolo8.darkbot.core.objects.OreTradeGui;
import com.github.manolo8.darkbot.extensions.util.Version;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.game.entities.Station;
import eu.darkbot.api.game.entities.Station.Refinery;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.game.other.Gui;
import eu.darkbot.api.game.other.Location;
import eu.darkbot.api.managers.AttackAPI;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.GameScreenAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.managers.OreAPI;
import eu.darkbot.api.managers.PetAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.managers.OreAPI.Ore;
import eu.darkbot.api.managers.StarSystemAPI.MapNotFoundException;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.LootCollectorModule;
import eu.darkbot.shared.modules.MapModule;
import eu.darkbot.shared.utils.SafetyFinder;

@Feature(name = "Palladium Hangar", description = "Collect palladium and change hangars to sell")
public class PallladiumHangar implements Module, Configurable<PalladiumConfig> {
    protected final Main main;
    protected final PluginAPI api;
    protected final BotAPI botApi;
    protected final OreAPI oreApi;
    protected final HeroAPI heroapi;
    protected final AttackAPI attackApi;
    protected final MovementAPI movement;
    protected final StatsAPI stats;
    protected final PetAPI pet;
    private GameMap sellMap;
    private GameMap activeMap;
    private Collection<? extends Station> bases;
    private Gui tradeGui;

    private PalladiumConfig configPa;
    protected LootCollectorModule lootModule;
    private State currentStatus;

    private OreTradeGui oreTradeOld;
    private List<BasePoint> basesOld;

    private long sellClick;
    private long aditionalWaitingTime = 0;
    private State lastStatus;
    private Integer activeHangar = null;

    private enum State {
        WAIT("Waiting"),
        HANGAR_AND_MAP_BASE("Selling palladium"),
        DEPOSIT_FULL_SWITCHING_HANGAR("Deposit full, switching hangar"),
        SWITCHING_HANGAR("Changing the hangar"),
        LOOT_PALADIUM("Loot paladium"),
        HANGAR_PALA_OTHER_MAP("Hangar paladium - To 5-3"),
        SWITCHING_PALA_HANGAR("Switching to the palladium hangar"),
        LOADING_HANGARS("Waiting - Loading hangars"),
        SEARCHING_PORTALS("Looking for a portal to change hangar");

        private final String message;

        State(String message) {
            this.message = message;
        }
    }

    public PallladiumHangar(Main main, PluginAPI api) {
        this(main, api, api.requireAPI(AuthAPI.class), api.requireInstance(SafetyFinder.class));
    }

    @Inject
    public PallladiumHangar(Main main, PluginAPI api, AuthAPI auth, SafetyFinder safety) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog();

        this.main = main;
        this.api = main.pluginAPI.getAPI(PluginAPI.class);
        this.botApi = api.getAPI(BotAPI.class);
        this.heroapi = api.getAPI(HeroAPI.class);
        this.attackApi = api.getAPI(AttackAPI.class);
        this.movement = api.getAPI(MovementAPI.class);
        this.oreApi = api.getAPI(OreAPI.class);
        this.stats = api.getAPI(StatsAPI.class);
        this.pet = api.getAPI(PetAPI.class);
        GameScreenAPI gameScreenAPI = api.getAPI(GameScreenAPI.class);
        tradeGui = gameScreenAPI.getGui("ore_trade");

        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        this.bases = entities.getStations();

        this.oreTradeOld = main.guiManager.oreTrade;
        this.basesOld = main.mapManager.entities.basePoints;

        StarSystemAPI starSystem = api.getAPI(StarSystemAPI.class);
        try {
            this.sellMap = starSystem.getByName("5-2");
        } catch (MapNotFoundException e) {
            this.sellMap = main.starManager.byName("5-2");
        }
        try {
            this.activeMap = starSystem.getByName("5-3");
        } catch (MapNotFoundException e) {
            this.activeMap = main.starManager.byName("5-3");
        }

        this.lootModule = new LootCollectorModule(api);
        this.currentStatus = State.WAIT;
        this.lastStatus = State.WAIT;
        this.activeHangar = null;
    }

    @Override
    public void setConfig(ConfigSetting<PalladiumConfig> arg0) {
        this.configPa = arg0.getValue();
    }

    @Override
    public boolean canRefresh() {
        return !heroapi.isMoving() && !attackApi.hasTarget();
    }

    private boolean canBeDisconnected() {
        if (configPa.goPortalChange) {
            return canRefresh();
        }
        return !heroapi.isAttacking() && !SharedFunctions.hasAttacker(heroapi, main);
    }

    @Override
    public String getStatus() {
        return currentStatus.message + " | " + lootModule.getStatus();
    }

    @Override
    public void onTickModule() {
        if (lastStatus != currentStatus) {
            lastStatus = currentStatus;
            aditionalWaitingTime = System.currentTimeMillis() + (configPa.aditionalWaitingTime * 1000);
        }
        if (aditionalWaitingTime > System.currentTimeMillis()) {
            return;
        }
        tryUpdateHangarList();

        if (activeHangar == null) {
            currentStatus = State.LOADING_HANGARS;
            updateHangarActive();
            return;
        }

        if (activeHangar.equals(configPa.sellHangar) && oreApi.getAmount(Ore.PALLADIUM) > 15) {
            this.currentStatus = State.HANGAR_AND_MAP_BASE;
            sell();
        } else if (stats.getCargo() >= stats.getMaxCargo()
                && oreApi.getAmount(Ore.PALLADIUM) > 15) {
            if (activeHangar.equals(configPa.sellHangar)) {
                this.currentStatus = State.HANGAR_AND_MAP_BASE;
                sell();
            } else if (configPa.sellHangar != null && !configPa.sellHangar.equals(activeHangar)
                    && canBeDisconnected()) {
                this.currentStatus = State.DEPOSIT_FULL_SWITCHING_HANGAR;
                if (botApi.getModule().getClass() != HangarSwitcher.class) {
                    this.activeHangar = null;
                    botApi.setModule(new HangarSwitcher(main, api, configPa.sellHangar));
                }
            } else {
                pet.setEnabled(true);
                lootModule.onTickModule();
                currentStatus = State.SEARCHING_PORTALS;
            }
        } else if (activeHangar.equals(configPa.collectHangar)) {
            if (heroapi.getMap() != null && heroapi.getMap().getId() == this.activeMap.getId()) {
                this.currentStatus = State.LOOT_PALADIUM;
                if (tradeGui != null && tradeGui.isVisible()) {
                    oreApi.showTrade(false, null);
                    tradeGui.setVisible(false);
                }
                pet.setEnabled(true);
                lootModule.onTickModule();
            } else {
                this.currentStatus = State.HANGAR_PALA_OTHER_MAP;
                heroapi.setRoamMode();
                if (configPa.sellOnDie && oreApi.getAmount(Ore.PALLADIUM) > 15) {
                    sell();
                } else {
                    if (tradeGui != null && tradeGui.isVisible()) {
                        tradeGui.setVisible(false);
                    }
                    this.main.setModule(api.requireInstance(MapModule.class)).setTarget(this.activeMap);
                }
            }
        } else if (configPa.collectHangar != null
                && !configPa.collectHangar.equals(
                        activeHangar)) {
            this.currentStatus = State.SWITCHING_PALA_HANGAR;
            if (botApi.getModule().getClass() != HangarSwitcher.class) {
                this.activeHangar = null;
                botApi.setModule(new HangarSwitcher(main, api, configPa.collectHangar));
            }
        }

    }

    private void sell() {
        if (botApi.getVersion().compareTo(new Version("1.13.17 beta 109 alpha 13")) > 1) {
            sellNew();
        } else {
            sellOld();
        }
    }

    private void sellNew() {
        pet.setEnabled(false);
        if (heroapi.getMap() != sellMap) {
            this.main.setModule(api.requireInstance(MapModule.class)).setTarget(this.sellMap);
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
                                oreApi.showTrade(false, base);
                            }
                        }
                    });
        }
    }

    private void sellOld() {
        pet.setEnabled(false);
        if (heroapi.getMap() != sellMap)
            this.main.setModule(api.requireInstance(MapModule.class)).setTarget(this.sellMap);
        else
            basesOld.stream().filter(b -> b.getLocationInfo().isInitialized()).findFirst().ifPresent(base -> {
                if (heroapi.distanceTo(base.getLocationInfo().getCurrent()) > 200) {
                    double angle = base.getLocationInfo().getCurrent().angleTo(heroapi.getLocationInfo().getCurrent())
                            + Math.random() * 0.2 - 0.1;
                    movement.moveTo(
                            Location.of(base.getLocationInfo().getCurrent(), angle, 100 + (100 * Math.random())));
                } else if (!heroapi.isMoving() && oreTradeOld.showTrade(true, base)
                        && System.currentTimeMillis() - 60_000 > sellClick) {
                    oreTradeOld.sellOre(OreTradeGui.Ore.PALLADIUM);
                    sellClick = System.currentTimeMillis();
                }
            });
    }

    public void updateHangarActive() {
        try {
            this.main.backpage.hangarManager.updateHangarList();
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
