package com.deeme.modules;

import java.util.Arrays;
import java.util.Collection;

import com.deeme.modules.temporal.HangarSwitcher;
import com.deeme.types.SharedFunctions;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deeme.types.config.PalladiumConfig;
import com.deeme.types.gui.ShipSupplier;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.backpage.hangar.Hangar;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.api.game.entities.Station;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.game.other.Gui;
import eu.darkbot.api.game.other.Location;
import eu.darkbot.api.managers.AttackAPI;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BackpageAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.GameScreenAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.OreAPI;
import eu.darkbot.api.managers.OreAPI.Ore;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.LootCollectorModule;
import eu.darkbot.shared.modules.MapModule;
import eu.darkbot.shared.utils.SafetyFinder;

@Feature(name = "Palladium Hangar", description = "Collect palladium and change hangars to sell")
public class PalladiumHangar extends LootCollectorModule implements Configurable<PalladiumConfig> {
    private final Main main;
    private final PluginAPI api;
    private final BotAPI botApi;
    private final OreAPI oreApi;
    private final HeroAPI heroapi;
    private final AttackAPI attackApi;
    private final StatsAPI stats;
    private final BackpageAPI backpage;
    private final FeatureInfo<?> featureInfo;
    private GameMap sellMap;
    private GameMap activeMap;
    private Collection<? extends Station> bases;
    private Gui tradeGui;

    private PalladiumConfig configPa;
    private State currentStatus;

    private long sellClick = 0;
    private long aditionalWaitingTime = 0;
    private State lastStatus;
    private Integer activeHangar = null;
    private boolean updateHangarList = true;

    private long nextCheck = 0;

    private enum State {
        WAIT("Waiting"),
        HANGAR_AND_MAP_BASE("Selling palladium"),
        DEPOSIT_FULL_SWITCHING_HANGAR("Deposit full, switching hangar"),
        SWITCHING_HANGAR("Changing the hangar"),
        LOOT_PALADIUM("Loot paladium"),
        HANGAR_PALA_OTHER_MAP("Hangar paladium - To 5-3"),
        SWITCHING_PALA_HANGAR("Switching to the palladium hangar"),
        LOADING_HANGARS("Waiting - Loading hangars"),
        SEARCHING_PORTALS("Looking for a portal to change hangar"),
        TRAVELLING_TO_TRADE("Travelling to trade"),
        SELLING("Selling palladium"),
        ERROR_NO_HANGAR("Error - No active hangar");

        private final String message;

        State(String message) {
            this.message = message;
        }
    }

    public PalladiumHangar(Main main, PluginAPI api) {
        this(main, api, api.requireAPI(AuthAPI.class), api.requireInstance(SafetyFinder.class));
    }

    @Inject
    public PalladiumHangar(Main main, PluginAPI api, AuthAPI auth, SafetyFinder safety) {
        super(api);
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog(api.requireAPI(ExtensionsAPI.class).getFeatureInfo(this.getClass()), auth.getAuthId());

        this.main = main;
        this.api = api;
        this.botApi = api.requireAPI(BotAPI.class);
        this.heroapi = api.requireAPI(HeroAPI.class);
        this.attackApi = api.requireAPI(AttackAPI.class);
        this.oreApi = api.requireAPI(OreAPI.class);
        this.stats = api.requireAPI(StatsAPI.class);
        this.backpage = api.requireAPI(BackpageAPI.class);

        ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
        this.featureInfo = extensionsAPI.getFeatureInfo(this.getClass());

        GameScreenAPI gameScreenAPI = api.requireAPI(GameScreenAPI.class);
        this.tradeGui = gameScreenAPI.getGui("ore_trade");

        EntitiesAPI entities = api.requireAPI(EntitiesAPI.class);
        this.bases = entities.getStations();

        StarSystemAPI starSystem = api.requireAPI(StarSystemAPI.class);
        this.sellMap = starSystem.findMap("5-2").orElse(starSystem.findMap(92).orElse(null));
        this.activeMap = starSystem.findMap("5-3").orElse(starSystem.findMap(93).orElse(null));

        this.currentStatus = State.WAIT;
        this.lastStatus = State.WAIT;
        this.activeHangar = null;
        this.updateHangarList = true;
        this.nextCheck = 0;
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
        if (configPa.goPortalChange && !(canRefresh() && super.canRefresh())) {
            return false;
        }

        return heroapi.isValid() && !heroapi.isAttacking() && !SharedFunctions.hasAttacker(heroapi, api);
    }

    @Override
    public String getStatus() {
        return currentStatus.message + " | " + super.getStatus();
    }

    @Override
    public void onTickStopped() {
        tryUpdateHangarList();
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

        if (backpage.isInstanceValid() && this.backpage.getSidStatus().contains("OK")) {
            tryUpdateHangarList();
            if (activeHangar == null) {
                currentStatus = State.LOADING_HANGARS;
                updateHangarActive();
                return;
            }
        } else if (configPa.ignoreSID) {
            tickOnSidKO();
            return;
        }

        if (activeHangar == null) {
            this.currentStatus = State.ERROR_NO_HANGAR;
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
                setHangarSwitcher(configPa.sellHangar);
            } else {
                pet.setEnabled(true);
                super.onTickModule();
                currentStatus = State.SEARCHING_PORTALS;
            }
        } else if (activeHangar.equals(configPa.collectHangar)) {
            farmLogic();
        } else if (configPa.collectHangar != null
                && !configPa.collectHangar.equals(
                        activeHangar)) {
            this.currentStatus = State.SWITCHING_PALA_HANGAR;
            setHangarSwitcher(configPa.collectHangar);
        }
    }

    private void setHangarSwitcher(Integer hangar) {
        if (botApi.getModule().getClass() != HangarSwitcher.class) {
            this.activeHangar = null;
            botApi.setModule(new HangarSwitcher(main, api, hangar, configPa.aditionalWaitingTime));
        }
    }

    private void tickOnSidKO() {
        if (stats.getCargo() >= stats.getMaxCargo() && oreApi.getAmount(Ore.PALLADIUM) > 15) {
            sell();
        } else {
            farmLogic();
        }
    }

    private void sell() {
        pet.setEnabled(false);
        if (heroapi.getMap() != sellMap) {
            this.botApi.setModule(api.requireInstance(MapModule.class)).setTarget(this.sellMap);
            return;
        }

        Station.Refinery base = bases.stream()
                .filter(b -> b instanceof Station.Refinery && b.getLocationInfo().isInitialized())
                .map(Station.Refinery.class::cast)
                .findFirst().orElse(null);

        if (base == null) {
            return;
        }

        if (movement.getDestination().distanceTo(base) > 200) {
            this.currentStatus = State.TRAVELLING_TO_TRADE;
            double angle = base.angleTo(heroapi) + Math.random() * 0.2 - 0.1;
            movement.moveTo(Location.of(base, angle, 100 + (100 * Math.random())));
        } else if (!heroapi.isMoving() && oreApi.showTrade(true, base)
                && System.currentTimeMillis() > sellClick) {
            this.currentStatus = State.SELLING;
            oreApi.sellOre(OreAPI.Ore.PALLADIUM);
            sellClick = System.currentTimeMillis() + (configPa.aditionalWaitingTime * 1000);
        }
    }

    private void farmLogic() {
        if (heroapi.getMap() != null && heroapi.getMap().getId() == this.activeMap.getId()) {
            this.currentStatus = State.LOOT_PALADIUM;
            hideTradeGui();
            pet.setEnabled(true);
            super.onTickModule();
        } else {
            this.currentStatus = State.HANGAR_PALA_OTHER_MAP;
            heroapi.setRoamMode();
            if (configPa.sellOnDie && oreApi.getAmount(Ore.PALLADIUM) > 15) {
                sell();
            } else {
                hideTradeGui();
                this.botApi.setModule(api.requireInstance(MapModule.class)).setTarget(this.activeMap);
            }
        }
    }

    private void hideTradeGui() {
        oreApi.showTrade(false, null);

        if (tradeGui != null && tradeGui.isVisible()) {
            tradeGui.setVisible(false);
        }
    }

    public void updateHangarActive() {
        try {
            this.main.backpage.hangarManager.updateHangarList();
            activeHangar = this.main.backpage.hangarManager.getHangarList().getData().getRet().getHangars().stream()
                    .filter(Hangar::isActive)
                    .map(Hangar::getHangarId)
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
            activeHangar = null;
        }
    }

    private void tryUpdateHangarList() {
        if (!updateHangarList || !backpage.isInstanceValid() || !backpage.getSidStatus().contains("OK")
                || nextCheck > System.currentTimeMillis()) {
            return;
        }

        nextCheck = System.currentTimeMillis() + 10000;
        currentStatus = State.LOADING_HANGARS;

        try {
            this.main.backpage.hangarManager.updateHangarList();
            if (ShipSupplier.updateOwnedShips(
                    this.main.backpage.hangarManager.getHangarList().getData().getRet().getShipInfos())) {
                updateHangarList = false;
            }
        } catch (Exception e) {
            featureInfo.addWarning("tryUpdateHangarList", e.getLocalizedMessage());
        }
    }

}
