package com.deeme.modules;

import com.deeme.types.DefenseLaserSupplier;
import com.deeme.types.ShipAttacker;
import com.deeme.types.VerifierChecker;
import com.deeme.types.config.PVPConfig;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.entities.Ship;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.extensions.features.Feature;
import com.github.manolo8.darkbot.modules.LootModule;
import com.github.manolo8.darkbot.modules.utils.SafetyFinder;

import eu.darkbot.api.game.items.SelectableItem.Special;
import eu.darkbot.api.managers.HeroItemsAPI;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
@Feature(name = "PVP Module", description = "PVP Module")
public class PVPModule extends LootModule implements Configurable<PVPConfig> {
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

    private double lastDistanceTarget = 1000;

    @Override
    public void install(Main main) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) return;
        VerifierChecker.checkAuthenticity();
        this.main = main;
        this.hero = main.hero;
        this.safety = new SafetyFinder(main);
        this.items = main.pluginAPI.getAPI(HeroItemsAPI.class);
        setup();
    }

    @Override
    public void setConfig(PVPConfig defenseConfig) {
        this.pvpConfig = defenseConfig;
        setup();
    }

    @Override
    public boolean canRefresh() {
        if (target == null) refreshing = System.currentTimeMillis() + 10000;
        return target == null && safety.state() == SafetyFinder.Escaping.WAITING;
    }

    private void setup() {
        if (main == null || pvpConfig == null) return;

        this.shipAttacker = new ShipAttacker(main,  new DefenseLaserSupplier(main, items, pvpConfig.SAB, pvpConfig.useRSB));
    }

    @Override
    public void tick() {
        if (checkDangerousAndCurrentMap()) {

            getTarget();
            if (target == null) {
                attackConfigLost = false;
                shipAttacker.resetDefenseData();
                super.tick();
            }

            shipAttacker.setTarget(target);

            setConfigToUse();

            if (!main.mapManager.isTarget(target)) {
                shipAttacker.lockAndSetTarget();
                return;
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
        if (target != null) return false;

        List<Ship> ships = main.mapManager.entities.ships.stream()
        .filter(s -> (s.playerInfo.isEnemy())).sorted(Comparator.comparingDouble(s -> s.locationInfo.distance(main.hero))).collect(Collectors.toList());

        if (ships.isEmpty()) return false;

        target = ships.get(0);

        return target != null;
    }

    private void setConfigToUse() {
        if (attackConfigLost || hero.getHealth().shieldPercent() < 0.1 && hero.getHealth().hpPercent() < 0.3) {
            attackConfigLost = true;
            hero.runMode();
        } else if (pvpConfig.useRunConfig) {
            double distance = hero.locationInfo.distance(target);

            if (distance > 500 && distance > lastDistanceTarget) {
                hero.runMode();
                lastDistanceTarget = distance;
            }
        } else {
            hero.attackMode();
        }
    }
}
