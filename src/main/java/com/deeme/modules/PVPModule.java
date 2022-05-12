package com.deeme.modules;

import com.deeme.types.DefenseLaserSupplier;
import com.deeme.types.ShipAttacker;
import com.deeme.types.VerifierChecker;
import com.deeme.types.config.PVPConfig;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.core.entities.Ship;
import com.github.manolo8.darkbot.core.itf.Module;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.extensions.features.Feature;
import com.github.manolo8.darkbot.modules.utils.SafetyFinder;

import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem.Formation;
import eu.darkbot.api.game.items.SelectableItem.Special;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.shared.modules.CollectorModule;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
@Feature(name = "PVP Module", description = "PVP Module")
public class PVPModule implements Module, Configurable<PVPConfig> {
    private PVPConfig pvpConfig;
    private Main main;
    private HeroItemsAPI items;
    public Ship target;
    protected HeroManager hero;
    private ShipAttacker shipAttacker;

    private boolean attackConfigLost = false;
    protected boolean firstAttack;
    protected long isAttacking;
    protected int fixedTimes;
    protected Character lastShot;
    protected long laserTime;
    protected long fixTimes;
    protected long clickDelay;

    private SafetyFinder safety;
    private double lastDistanceTarget = 1000;
    protected CollectorModule collectorModule;

    @Override
    public void install(Main main) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) return;
        VerifierChecker.checkAuthenticity();
        this.main = main;
        this.hero = main.hero;
        this.safety = new SafetyFinder(main);
        this.collectorModule = new CollectorModule(main.pluginAPI);
        this.items = main.pluginAPI.getAPI(HeroItemsAPI.class);
        setup();
    }

    @Override
    public String status() {
        return safety.state() != SafetyFinder.Escaping.NONE ? safety.status() : target != null ? "Attacking an enemy" : collectorModule.getStatus() ;
    }

    @Override
    public void setConfig(PVPConfig pvpConfig) {
        this.pvpConfig = pvpConfig;
        setup();
    }

    @Override
    public boolean canRefresh() {
        if (target == null && collectorModule.canRefresh()) {
            return safety.tick();
        }
        return false;
    }

    private void setup() {
        if (main == null || pvpConfig == null) return;

        this.shipAttacker = new ShipAttacker(main,  new DefenseLaserSupplier(main, items, main.config.LOOT.SAB, main.config.LOOT.RSB.ENABLED));
    }

    @Override
    public void tick() {
        if (safety.tick()) {
            getTarget();
            if (target == null || !target.isValid()) {
                attackConfigLost = false;
                target = null;
                shipAttacker.resetDefenseData();
                main.hero.roamMode();
                collectorModule.onTickModule();
                return;
            }
            shipAttacker.setTarget(target);

            setConfigToUse();

            if (!main.mapManager.isTarget(target)) {
                shipAttacker.lockAndSetTarget();
                return;
            }

            if (pvpConfig.useBestRocket) {
                shipAttacker.changeRocket();
            }

            if (hero.getLocationInfo().distance(target) < 575 && shipAttacker.useKeyWithConditions(pvpConfig.ability, null)) {
                pvpConfig.ability.lastUse = System.currentTimeMillis();
            }

            if (shipAttacker.useKeyWithConditions(pvpConfig.ISH, Special.ISH_01)) pvpConfig.ISH.lastUse = System.currentTimeMillis();

            if (shipAttacker.useKeyWithConditions(pvpConfig.SMB, Special.SMB_01)) pvpConfig.SMB.lastUse = System.currentTimeMillis();

            if (shipAttacker.useKeyWithConditions(pvpConfig.PEM, Special.EMP_01)) pvpConfig.PEM.lastUse = System.currentTimeMillis();

            if (shipAttacker.useKeyWithConditions(pvpConfig.otherKey, null)) pvpConfig.otherKey.lastUse = System.currentTimeMillis();

            shipAttacker.doKillTargetTick();
            shipAttacker.vsMove();
        }
    }

    private boolean getTarget() {
        if (target != null) return true;

        List<Ship> ships = main.mapManager.entities.ships.stream()
        .filter(s -> (s.playerInfo.isEnemy())).sorted(Comparator.comparingDouble(s -> s.locationInfo.distance(main.hero))).collect(Collectors.toList());

        if (ships.isEmpty()) return false;

        target = ships.get(0);

        return target != null;
    }

    private void setConfigToUse() {
        if (attackConfigLost || hero.getHealth().shieldPercent() < 0.1 && hero.getHealth().hpPercent() < 0.3) {
            attackConfigLost = true;
            setMode(main.config.GENERAL.RUN);
        } else if (pvpConfig.useRunConfig && target != null) {
            double distance = hero.locationInfo.distance(target);
            if (distance > 400 && distance > lastDistanceTarget && target.getSpeed() > hero.getSpeed()) {
                setMode(main.config.GENERAL.RUN);
                lastDistanceTarget = distance;
            } else {
                setMode(main.config.GENERAL.OFFENSIVE);
            }
        } else {
            setMode(main.config.GENERAL.OFFENSIVE);
        }
    }

    private void setMode(Config.ShipConfig config) {
        if (pvpConfig.useBestFormation) {
            Formation formation = shipAttacker.getBestFormation();
            if (formation != null) {
                if (!main.hero.isInFormation(formation)) {
                    Boolean canChange = items.getItem(formation, ItemFlag.USABLE, ItemFlag.READY).isPresent();
                    if (canChange) {
                        main.hero.setMode(config.CONFIG, main.facadeManager.slotBars.getKeyBind(formation));
                    } else {
                        main.hero.setMode(config);
                    }
                }
            }
        } else {
            main.hero.setMode(config);
        }
    }
}
