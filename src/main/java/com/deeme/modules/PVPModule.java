package com.deeme.modules;

import com.deeme.types.DefenseLaserSupplier;
import com.deeme.types.ShipAttacker;
import com.deeme.types.VerifierChecker;
import com.deeme.types.config.PVPConfig;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.entities.Ship;
import com.github.manolo8.darkbot.core.itf.Module;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.extensions.features.Feature;
import com.github.manolo8.darkbot.modules.utils.SafetyFinder;

import eu.darkbot.api.game.items.SelectableItem.Special;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.shared.modules.CollectorModule;

import java.util.Arrays;
@Feature(name = "PVP Module", description = "It is limited so as not to spoil the game")
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
        if (pvpConfig.move && target == null && collectorModule.canRefresh()) {
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
        if (!pvpConfig.move || safety.tick()) {
            getTarget();
            if (target == null || !target.isValid()) {
                attackConfigLost = false;
                target = null;
                shipAttacker.resetDefenseData();
                if (pvpConfig.move) {
                    if (pvpConfig.changeConfig) {
                        main.hero.roamMode();
                    }
                    collectorModule.onTickModule();
                }
                return;
            }
            shipAttacker.setTarget(target);

            if (pvpConfig.changeConfig) {
                setConfigToUse();
            }

            if (!main.mapManager.isTarget(target)) {
                shipAttacker.lockAndSetTarget();
                return;
            }

            if (pvpConfig.useBestRocket) {
                shipAttacker.changeRocket();
            }

            if (hero.getLocationInfo().distance(target) < 575) {
                shipAttacker.useKeyWithConditions(pvpConfig.ability, null);
            }

            shipAttacker.useKeyWithConditions(pvpConfig.ISH, Special.ISH_01);
            shipAttacker.useKeyWithConditions(pvpConfig.SMB, Special.SMB_01);
            shipAttacker.useKeyWithConditions(pvpConfig.PEM, Special.EMP_01);
            shipAttacker.useKeyWithConditions(pvpConfig.otherKey, null);

            shipAttacker.doKillTargetTick();
            if (pvpConfig.move) {
                shipAttacker.vsMove();
            }
        }
    }

    private boolean getTarget() {
        if (target != null) return true;

        target = shipAttacker.getEnemy(pvpConfig.rangeForEnemies);

        return target != null;
    }

    private void setConfigToUse() {
        if (attackConfigLost || hero.getHealth().shieldPercent() < 0.1 && hero.getHealth().hpPercent() < 0.3) {
            attackConfigLost = true;
            shipAttacker.setMode(main.config.GENERAL.RUN, pvpConfig.useBestFormation);
        } else if (pvpConfig.useRunConfig && target != null) {
            double distance = hero.locationInfo.distance(target);
            if (distance > 400 && distance > lastDistanceTarget && target.getSpeed() > hero.getSpeed()) {
                shipAttacker.setMode(main.config.GENERAL.RUN, pvpConfig.useBestFormation);
                lastDistanceTarget = distance;
            } else {
                shipAttacker.setMode(main.config.GENERAL.OFFENSIVE, pvpConfig.useBestFormation);
            }
        } else {
            shipAttacker.setMode(main.config.GENERAL.OFFENSIVE, pvpConfig.useBestFormation);
        }
    }
}
