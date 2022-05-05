package com.deeme.types;

import com.deeme.types.config.Defense;
import com.deeme.types.config.ExtraKeyConditions;
import com.deeme.types.DefenseLaserSupplier;
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
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.game.items.SelectableItem.Special;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.HeroItemsAPI;

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

    public Ship target;

    protected long laserTime;
    protected long fixTimes;
    protected long clickDelay;
    protected boolean sab;
    private Defense defense;
    private boolean attackConfigLost = false;
    private Random rnd;

    protected boolean firstAttack;
    protected long isAttacking;
    protected int fixedTimes;
    protected Character lastShot;

    public ShipAttacker(Main main, Defense defense) {
        this.main = main;
        this.hero = main.hero;
        this.config = main.config;
        this.drive = hero.drive;
        this.defense = defense;
        this.safety = new SafetyFinder(main);
        this.rnd = new Random();
        this.keybinds = main.facadeManager.settings;
        this.items = main.pluginAPI.getAPI(HeroItemsAPI.class);

        if (defense == null) {
            this.laserSupplier = new DefenseLaserSupplier(main, items, main.config.LOOT.SAB, false);
        } else {
            this.laserSupplier = new DefenseLaserSupplier(main, items, defense);
        }
    }

    public void tick() {
        isUnderAttack();
        if (target == null) return;
        setConfigToUse();

        doKillTargetTick();

        if (hero.getLocationInfo().distance(target) < 575 &&
                useKeyWithConditions(defense.ability, null)) {
            defense.ability.lastUse = System.currentTimeMillis();
        }

        if (useKeyWithConditions(defense.ISH, Special.ISH_01)) defense.ISH.lastUse = System.currentTimeMillis();

        if (useKeyWithConditions(defense.SMB, Special.SMB_01)) defense.SMB.lastUse = System.currentTimeMillis();

        if (useKeyWithConditions(defense.PEM, Special.EMP_01)) defense.PEM.lastUse = System.currentTimeMillis();

        if (useKeyWithConditions(defense.otherKey, null)) defense.otherKey.lastUse = System.currentTimeMillis();

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
            hero.setMode(defense.secondConfig);
        } else {
            hero.attackMode();
        }

    }

    public void lockAndSetTarget() {
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
        else if (API instanceof DarkBoatAdapter) API.keyboardClick(keybinds.getCharCode(ATTACK_LASER));
        else target.trySelect(true);
    }


    private Character getAttackKey() {
        Laser laser = laserSupplier.get();

        if (laser != null) {
            Character key = main.facadeManager.slotBars.getKeyBind(laser);
            if (key != null) return key;
        }

        return defense.ammoKey;
    }

    private void vsMove() {
        double distance = hero.getLocationInfo().now.distance(target.getLocationInfo().now);
        Location targetLoc = target.getLocationInfo().destinationInTime(400);

        if (distance > 500) {
            if (drive.canMove(target.getLocationInfo().now)) {
                drive.move(target.getLocationInfo().now);
                main.hero.roamMode();
            } else {
                resetDefenseData();
            }

        } else {
            drive.move(Location.of(targetLoc, rnd.nextInt(360), distance));
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
                        return target != null;
                    }

                    target = SharedFunctions.getAttacker(ship,main,this.hero,target);
                    if (target != null) {
                        return target != null;
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

    private void resetDefenseData() {
        attackConfigLost = false;
        target = null;
        fixedTimes = 0;
    }
}
