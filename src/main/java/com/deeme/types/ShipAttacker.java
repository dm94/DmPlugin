package com.deeme.types;

import com.deeme.types.config.Defense;
import com.deeme.types.config.ExtraKeyConditions;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.core.api.DarkBoatAdapter;
import com.github.manolo8.darkbot.core.entities.Ship;
import com.github.manolo8.darkbot.core.manager.EffectManager;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.core.objects.facades.SettingsProxy;
import com.github.manolo8.darkbot.core.objects.group.GroupMember;
import com.github.manolo8.darkbot.core.utils.Drive;
import com.github.manolo8.darkbot.core.utils.Location;
import com.github.manolo8.darkbot.modules.utils.SafetyFinder;

import org.jetbrains.annotations.Nullable;

import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Formation;
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.game.items.SelectableItem.Special;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.HeroItemsAPI;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.github.manolo8.darkbot.core.objects.facades.SettingsProxy.KeyBind.*;
import static com.github.manolo8.darkbot.Main.API;

public class ShipAttacker {
    protected final SettingsProxy keybinds;
    private final HeroItemsAPI items;
    protected Config config;
    protected HeroManager hero;
    protected Drive drive;
    private Main main;
    private SafetyFinder safety;
    private DefenseLaserSupplier laserSupplier;
    private FormationSupplier formationSupplier;
    private RocketSupplier rocketSupplier;

    public Ship target;

    protected long laserTime;
    protected long fixTimes;
    protected long clickDelay;
    protected boolean sab;
    private Defense defense = null;
    private boolean attackConfigLost = false;
    private Random rnd;

    protected boolean firstAttack;
    protected long isAttacking;
    protected int fixedTimes;
    protected Character lastShot;
    protected long rocketTime;

    public ShipAttacker(Main main) {
        this.main = main;
        this.hero = main.hero;
        this.config = main.config;
        this.drive = hero.drive;
        this.safety = new SafetyFinder(main);
        this.rnd = new Random();
        this.keybinds = main.facadeManager.settings;
        this.items = main.pluginAPI.getAPI(HeroItemsAPI.class);
        this.laserSupplier = new DefenseLaserSupplier(main, items, main.config.LOOT.SAB, main.config.LOOT.RSB.ENABLED);
        this.formationSupplier = new FormationSupplier(main, items);
        this.rocketSupplier = new RocketSupplier(main, items);
    }

    public ShipAttacker(Main main, Defense defense) {
        this(main);
        this.defense = defense;
        this.laserSupplier = new DefenseLaserSupplier(main, items, defense);
    }

    public ShipAttacker(Main main, DefenseLaserSupplier defenseLaserSupplier) {
        this(main);
        this.laserSupplier = defenseLaserSupplier;
    }

    public void tick() {
        if (defense != null) {
            isUnderAttack();
            if (target == null) return;
            setConfigToUse();

            doKillTargetTick();

            if (hero.getLocationInfo().distance(target) < 575) {
                useKeyWithConditions(defense.ability, null);
            }

            if (defense.useBestRocket) {
                changeRocket();
            }

            useKeyWithConditions(defense.ISH, Special.ISH_01);
            useKeyWithConditions(defense.SMB, Special.SMB_01);
            useKeyWithConditions(defense.PEM, Special.EMP_01);
            useKeyWithConditions(defense.otherKey, null);

            tryAttackOrFix();

            if (defense.movementMode == 1) {
                vsMove();
            } else if (defense.movementMode == 2) {
                safety.tick();
            } else if (defense.movementMode == 3) {
                if (!drive.isMoving() || drive.isOutOfMap()) drive.moveRandom();
            } else if (defense.movementMode == 4) {
                if (hero.getHealth().hpPercent() <= config.GENERAL.SAFETY.REPAIR_HP_RANGE.min){
                    safety.tick();
                } else {
                    vsMove();
                }
            }
        }
    }

    public void uninstall() {
        safety.uninstall();
    }

    public boolean takeControl() {
        return defense.movementMode == 0;
    }

    private void setConfigToUse() {
        if (defense.useSecondConfig &&
                hero.getHealth().shieldPercent() < 0.1 && defense.healthToChange <= hero.getHealth().shieldPercent()){
            attackConfigLost = true;
        }

        if (attackConfigLost && defense.useSecondConfig) {
            setMode(defense.secondConfig, false);
        } else {
            setMode(main.config.GENERAL.OFFENSIVE);
        }

    }

    public void lockAndSetTarget() {
        if (hero.getLocalTarget() == target && firstAttack) {
            clickDelay = System.currentTimeMillis();
        }

        fixedTimes = 0;
        laserTime = 0;
        firstAttack = false;
        if (hero.locationInfo.distance(target) < 800 && System.currentTimeMillis() - clickDelay > 500) {
            hero.setLocalTarget(target);
            target.trySelect(false);
            clickDelay = System.currentTimeMillis();
        }
    }

    public void setTarget(@Nullable Lockable target) {
        if (target != null && !(target instanceof Ship))
            throw new IllegalArgumentException("Only Ship attacking is supported by this implementation");
        this.target = (Ship) target;
    }

    public void doKillTargetTick() {
        if (target == null) return;
        if (!main.mapManager.isTarget(target)) {
            lockAndSetTarget();
            return;
        }

        tryAttackOrFix();
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
        } else if (target.health.hpDecreasedIn(1500) || target.hasEffect(EffectManager.Effect.NPC_ISH) || target.hasEffect(EffectManager.Effect.ISH)
                || hero.locationInfo.distance(target) > 700) {
            isAttacking = Math.max(isAttacking, System.currentTimeMillis() + 2000);
        } else if (System.currentTimeMillis() > isAttacking) {
            sendAttack(1500, ++fixedTimes * 3000L, false);
        }
    }

    private void sendAttack(long minWait, long bugTime, boolean normal) {
        laserTime = System.currentTimeMillis() + minWait;
        isAttacking = Math.max(isAttacking, laserTime + bugTime);
        if (normal) {
            lastShot = getAttackKey();
            API.keyboardClick(lastShot);
        }
        else if (API instanceof DarkBoatAdapter) API.keyboardClick(keybinds.getCharCode(ATTACK_LASER));
        else target.trySelect(true);
    }

    private Character getAttackKey() {
        return getAttackKey(main.config.LOOT.AMMO_KEY);
    }

    private Laser getBestLaser() {
        return laserSupplier.get();
    }

    private Character getAttackKey(Character defaultAmmo) {
        Laser laser = getBestLaser();

        if (laser != null) {
            Character key = main.facadeManager.slotBars.getKeyBind(laser);
            if (key != null) return key;
        }

        if (defense != null) {
            return defense.ammoKey;
        }
        return defaultAmmo;
    }

    private SelectableItem getBestRocket() {
        return rocketSupplier.get();
    }

    public SelectableItem getBestFormation() {
        return formationSupplier.get();
    }

    public void changeRocket() {
        if (System.currentTimeMillis() < rocketTime) return;
        SelectableItem rocket = getBestRocket();

        if (rocket != null && !main.hero.getRocket().getId().equals(rocket.getId()) && useSelectableReadyWhenReady(rocket)) {
            rocketTime = System.currentTimeMillis() + 2000;
        }
    }

    public void vsMove() {
        double distance = hero.getLocationInfo().now.distance(target.getLocationInfo().now);
        Location targetLoc = target.getLocationInfo().destinationInTime(400);

        if (distance > 400) {
            if (drive.canMove(target.getLocationInfo().now)) {
                drive.move(target.getLocationInfo().now);
                if (target.getSpeed() > main.hero.getSpeed()) {
                    main.hero.runMode();
                }
            } else {
                resetDefenseData();
            }
        } else {
            drive.move(Location.of(targetLoc, rnd.nextInt(360), distance));
        }
    }

    public boolean useKeyWithConditions(ExtraKeyConditions extra, SelectableItem selectableItem) {
        if (extra.enable) {
            if (selectableItem == null && extra.Key != null) {
                selectableItem = this.main.facadeManager.slotBars.getItem(extra.Key);
            }

            if (hero.getHealth().hpPercent() < extra.HEALTH_RANGE.max && hero.getHealth().hpPercent() > extra.HEALTH_RANGE.min
                    && target.getHealth().hpPercent() < extra.HEALTH_ENEMY_RANGE.max && target.getHealth().hpPercent() > extra.HEALTH_ENEMY_RANGE.min) {
                        return useSelectableReadyWhenReady(selectableItem);
            }
        }
        return false;
    }

    public boolean useSelectableReadyWhenReady(SelectableItem selectableItem) {
        if (System.currentTimeMillis() - clickDelay < 1000) return false;
        if (selectableItem == null)  return false; 

        boolean isReady = items.getItem(selectableItem, ItemFlag.USABLE, ItemFlag.READY).isPresent();

        if (isReady && items.useItem(selectableItem).isSuccessful()) {
            clickDelay = System.currentTimeMillis();
            return true;
        }

        return false;
    }

    public boolean isUnderAttack() {
        if (!defense.respondAttacks) return false;

        target = SharedFunctions.getAttacker(this.hero,main,this.hero,target);

        if (target == null) {
            List<Ship> ships = main.mapManager.entities.ships.stream()
                    .filter(s -> (s.playerInfo.clanDiplomacy == 1 && defense.helpAllies) ||
                            (defense.helpEveryone && !s.playerInfo.isEnemy())
                            || (defense.helpGroup && inGroupAttacked(s.id)))
                    .collect(Collectors.toList());

            if (!ships.isEmpty()) {
                for (Ship ship : ships) {
                    if (defense.helpAttack && ship.isAttacking() && ship.getTarget() != null) {
                        target = (Ship) ship.getTarget();
                        return true;
                    }

                    target = SharedFunctions.getAttacker(ship,main,this.hero,target);
                    if (target != null) {
                        return true;
                    }
                }
            }
        }

        if (target == null && defense.goToGroup) {
            GroupMember member = getMemberGroupAttacked();
            if (member != null) {
                drive.moveTo(member.getLocation());
            }
        }

        if (target == null) resetDefenseData();

        return target != null;
    }

    private boolean inGroupAttacked(int id) {
        if (main.guiManager.group == null) return false;

        if (main.guiManager.group.hasGroup()) {
            for (GroupMember member : main.guiManager.group.group.members) {
                if (member.id == id && member.isAttacked()) {
                    return true;
                }
            }
        }
        return false;
    }

    private GroupMember getMemberGroupAttacked() {
        if (main.guiManager.group == null) return null;

        if (main.guiManager.group.hasGroup()) {
            for (GroupMember member : main.guiManager.group.group.members) {
                if (member.getMapId() == hero.getMap().getId() && member.isAttacked()) {
                    return member;
                }
            }
        }
        return null;
    }

    public void setMode(Config.ShipConfig config) {
        if (defense != null) {
            setMode(config, defense.useBestFormation);
        } else {
            setMode(config, true);
        }
    }

    public void setMode(Config.ShipConfig config, boolean useBestFormation) {
        if (useBestFormation) {
            Formation formation = (Formation) getBestFormation();
            if (formation != null && !main.hero.isInFormation(formation)) {
                if (items.getItem(formation, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                    main.hero.setMode(config.CONFIG, main.facadeManager.slotBars.getKeyBind(formation));
                } else {
                    main.hero.setMode(config);
                }
            }
        } else {
            main.hero.setMode(config);
        }
    }

    public Ship getEnemy(int maxDistance) {
        return main.mapManager.entities.ships.stream()
        .filter(s -> (s.playerInfo.isEnemy() && s.locationInfo.distance(main.hero) <= maxDistance)).sorted(Comparator.comparingDouble(s -> s.locationInfo.distance(main.hero))).findAny().orElse(null);
    }

    public void resetDefenseData() {
        attackConfigLost = false;
        target = null;
        fixedTimes = 0;
    }
}
