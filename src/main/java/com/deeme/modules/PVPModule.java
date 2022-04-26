package com.deeme.modules;

import com.deeme.types.DefenseLaserSupplier;
import com.deeme.types.VerifierChecker;
import com.deeme.types.config.ExtraKeyConditions;
import com.deeme.types.config.PVPConfig;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.api.DarkBoatAdapter;
import com.github.manolo8.darkbot.core.entities.Ship;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.core.itf.Module;
import com.github.manolo8.darkbot.core.manager.EffectManager;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.core.utils.Drive;
import com.github.manolo8.darkbot.core.utils.Location;
import com.github.manolo8.darkbot.extensions.features.Feature;
import com.github.manolo8.darkbot.modules.LootModule;
import com.github.manolo8.darkbot.modules.MapModule;
import com.github.manolo8.darkbot.modules.utils.SafetyFinder;

import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.game.items.SelectableItem.Special;
import eu.darkbot.api.managers.HeroItemsAPI;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.github.manolo8.darkbot.core.objects.facades.SettingsProxy.KeyBind.*;
import static com.github.manolo8.darkbot.Main.API;

@Feature(name = "PVP Module", description = "PVP Module")
public class PVPModule extends LootModule implements Module, Configurable<PVPConfig> {
    private PVPConfig pvpConfig;
    private Main main;
    private Drive drive;
    private SafetyFinder safety;
    private DefenseLaserSupplier laserSupplier;
    private HeroItemsAPI items;
    public Ship target;
    protected HeroManager hero;
    private Random rnd;

    private boolean attackConfigLost = false;
    protected boolean firstAttack;
    protected long isAttacking;
    protected int fixedTimes;
    protected Character lastShot;
    protected long laserTime;
    protected long fixTimes;
    protected long clickDelay;
    protected long refreshing;

    private double lastDistanceTarget = 1000;

    @Override
    public void install(Main main) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) return;
        VerifierChecker.checkAuthenticity();
        this.main = main;
        this.hero = main.hero;
        this.drive = main.hero.drive;
        this.rnd = new Random();
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
    public void uninstall() {
        safety.uninstall();
    }

    @Override
    public boolean canRefresh() {
        if (target == null) refreshing = System.currentTimeMillis() + 10000;
        return target == null && safety.state() == SafetyFinder.Escaping.WAITING;
    }

    private void setup() {
        if (main == null || pvpConfig == null) return;

        this.laserSupplier = new DefenseLaserSupplier(main, items, pvpConfig.SAB, pvpConfig.useRSB);
    }

    @Override
    public void tick() {
        if (checkDangerousAndCurrentMap()) {

            getTarget();
            if (target == null) {
                super.tick();
            }

            setConfigToUse();

            if (!main.mapManager.isTarget(target)) {
                lockAndSetTarget();
                return;
            }

            if (hero.getLocationInfo().distance(target) < 575 &&
                    useKeyWithConditions(pvpConfig.ability, null)) {
                        pvpConfig.ability.lastUse = System.currentTimeMillis();
            }

            if (useKeyWithConditions(pvpConfig.ISH, Special.ISH_01)) pvpConfig.ISH.lastUse = System.currentTimeMillis();

            if (useKeyWithConditions(pvpConfig.SMB, Special.SMB_01)) pvpConfig.SMB.lastUse = System.currentTimeMillis();

            if (useKeyWithConditions(pvpConfig.PEM, Special.EMP_01)) pvpConfig.PEM.lastUse = System.currentTimeMillis();

            if (useKeyWithConditions(pvpConfig.otherKey, null)) pvpConfig.otherKey.lastUse = System.currentTimeMillis();

            tryAttackOrFix();

            vsMove();
        }
    }

    protected boolean checkDangerousAndCurrentMap() {
        safety.setRefreshing(System.currentTimeMillis() <= refreshing);
        return safety.tick() && checkMap();
    }

    protected boolean checkMap() {
        if (main.config.GENERAL.WORKING_MAP != this.hero.map.id && !main.mapManager.entities.portals.isEmpty()) {
            this.main.setModule(new MapModule())
                    .setTarget(this.main.starManager.byId(this.main.config.GENERAL.WORKING_MAP));
            return false;
        }
        return true;
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
        if (hero.getHealth().shieldPercent() < 0.1 && hero.getHealth().hpPercent() < 0.3) {
            attackConfigLost = true;
        }

        if (pvpConfig.useRunConfig) {
            double distance = hero.locationInfo.distance(target);

            if (distance > 500 && distance > lastDistanceTarget) {
                hero.runMode();
                lastDistanceTarget = distance;
            }
        }

        if (attackConfigLost) {
            hero.runMode();
        } else {
            hero.attackMode();
        }
    }

    private void lockAndSetTarget() {
        if (hero.getLocalTarget() == target && firstAttack) {
            // On npc death, lock goes away before the npc does, sometimes the bot would try to lock the dead npc.
            // This adds a bit of delay when any cause makes you lose the lock, until you try to re-lock.
            clickDelay = System.currentTimeMillis();
        }

        fixedTimes = 0;
        laserTime = 0;
        firstAttack = false;
        if (hero.locationInfo.distance(target) < 800 && System.currentTimeMillis() - clickDelay > 500) {
            hero.setLocalTarget(target);
            target.trySelect(false);
            clickDelay = System.currentTimeMillis();
        } else {
            drive.move(target);
        }
    }

    private boolean useKeyWithConditions(ExtraKeyConditions extra, SelectableItem selectableItem) {
        if (System.currentTimeMillis() - clickDelay < 1000) return false;

        if (extra.enable) {
            boolean isReady = false;
            if (selectableItem != null) {
                isReady = items.getItem(selectableItem, ItemFlag.USABLE, ItemFlag.READY).isPresent();
            } else {
                isReady = extra.lastUse < System.currentTimeMillis() - (extra.countdown*1000);
            }

            if (isReady && hero.getHealth().hpPercent() < extra.HEALTH_RANGE.max && hero.getHealth().hpPercent() > extra.HEALTH_RANGE.min
                    && target.getHealth().hpPercent() < extra.HEALTH_ENEMY_RANGE.max && target.getHealth().hpPercent() > extra.HEALTH_ENEMY_RANGE.min) {
                if (selectableItem != null) {
                    items.useItem(selectableItem);
                } else if (extra.Key != null) {
                    API.keyboardClick(extra.Key);
                }
                clickDelay = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }

    protected void tryAttackOrFix() {
        if (System.currentTimeMillis() < laserTime) return;

        if (!firstAttack) {
            firstAttack = true;
            sendAttack(1500, 5000, true);
        } else if (lastShot != getAttackKey()) {
            sendAttack(250, 5000, true);
        } else if (!hero.isAttacking(target) || !hero.isAiming(target)) {
            sendAttack(1500, 5000, false);
        } else if (target.health.hpDecreasedIn(1500) || target.hasEffect(EffectManager.Effect.NPC_ISH)
                || hero.locationInfo.distance(target) > 700) {
            isAttacking = Math.max(isAttacking, System.currentTimeMillis() + 2000);
        } else if (System.currentTimeMillis() > isAttacking) {
            sendAttack(1500, ++fixedTimes * 3000L, false);
        }
    }

    private void sendAttack(long minWait, long bugTime, boolean normal) {
        laserTime = System.currentTimeMillis() + minWait;
        isAttacking = Math.max(isAttacking, laserTime + bugTime);
        if (normal) API.keyboardClick(lastShot = getAttackKey());
        else if (API instanceof DarkBoatAdapter) API.keyboardClick(main.facadeManager.settings.getCharCode(ATTACK_LASER));
        else target.trySelect(true);
    }


    private Character getAttackKey() {
        Laser laser = laserSupplier.get();

        if (laser != null) {
            Character key = main.facadeManager.slotBars.getKeyBind(laser);
            if (key != null) return key;
        }

        return pvpConfig.ammoKey;
    }

    private void vsMove() {
        double distance = hero.getLocationInfo().now.distance(target.getLocationInfo().now);
        Location targetLoc = target.getLocationInfo().destinationInTime(400);

        if (distance > 700) {
            if (drive.canMove(target.getLocationInfo().now)) {
                drive.move(target.getLocationInfo().now);
            } else {
                resetDefenseData();
            }

        } else {
            drive.move(Location.of(targetLoc, rnd.nextInt(360), distance));
        }

    }

    private void resetDefenseData() {
        attackConfigLost = false;
        target = null;
        fixedTimes = 0;
        lastDistanceTarget = 1000;
    }

}
