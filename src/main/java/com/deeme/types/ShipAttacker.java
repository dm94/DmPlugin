package com.deeme.types;

import com.deeme.types.config.Defense;
import com.deeme.types.config.ExtraKeyConditions;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.core.entities.Ship;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.core.objects.group.GroupMember;
import com.github.manolo8.darkbot.core.utils.Drive;
import com.github.manolo8.darkbot.core.utils.Location;
import com.github.manolo8.darkbot.modules.utils.SafetyFinder;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.github.manolo8.darkbot.Main.API;

public class ShipAttacker {
    protected Config config;
    protected HeroManager hero;
    protected Drive drive;
    private Main main;
    private SafetyFinder safety;

    public Ship target;

    protected long laserTime;
    protected long fixTimes;
    protected long clickDelay;
    protected boolean sab;
    private Defense defense;
    private Character currentAmmo;
    private boolean attackConfigLost = false;
    private Random rnd;

    private long lastRsbUse = 0;

    public ShipAttacker(Main main, Defense defense) {
        this.main = main;
        this.hero = main.hero;
        this.config = main.config;
        this.drive = hero.drive;
        this.defense = defense;
        this.safety = new SafetyFinder(main);
        this.rnd = new Random();
    }

    public void tick() {
        isUnderAttack();
        if (target == null) return;
        setConfigToUse();

        if (!main.mapManager.isTarget(target)) {
            lockAndSetTarget();
            return;
        }

        if (hero.locationInfo.distance(target) < 575 &&
                useKeyWithConditions(defense.ability)) {
            defense.ability.lastUse = System.currentTimeMillis();
        }

        if (useKeyWithConditions(defense.ISH)) defense.ISH.lastUse = System.currentTimeMillis();

        if (useKeyWithConditions(defense.SMB)) defense.SMB.lastUse = System.currentTimeMillis();

        if (useKeyWithConditions(defense.PEM)) defense.PEM.lastUse = System.currentTimeMillis();

        if (useKeyWithConditions(defense.otherKey)) defense.otherKey.lastUse = System.currentTimeMillis();

        mixerRSB();
        tryAttackOrFix();

        if (defense.movementMode == 1) {
            vsMove();
        } else if (defense.movementMode == 2) {
            safety.tick();
        } else if (defense.movementMode == 3) {
            if (!drive.isMoving() || drive.isOutOfMap()) drive.moveRandom();
        } else if (defense.movementMode == 4) {
            if (hero.health.hpPercent() <= config.GENERAL.SAFETY.REPAIR_HP_RANGE.min){
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
                hero.health.shieldPercent() < 0.1 && !shouldSab() && defense.healthToChange <= hero.health.shieldPercent()){
            attackConfigLost = true;
        }

        if (attackConfigLost && defense.useSecondConfig) {
            hero.setMode(defense.secondConfig);
        } else {
            hero.attackMode();
        }

    }

    void lockAndSetTarget() {
        if (hero.locationInfo.distance(target) > 700 || System.currentTimeMillis() - clickDelay < 400) return;
        hero.setLocalTarget(target);
        setRadiusAndClick(true);
        clickDelay = System.currentTimeMillis();
        fixTimes = 0;
        laserTime = clickDelay + 50;
    }

    protected void tryAttackOrFix() {
        boolean bugged = hero.isAttacking(target)
                && (!hero.isAiming(target) || (!target.health.hpDecreasedIn(3000) && hero.locationInfo.distance(target) < 650))
                && System.currentTimeMillis() > (laserTime + fixTimes * 3000);
        boolean sabChanged = shouldSab() != sab;
        if ((sabChanged || !hero.isAttacking(target) || bugged) && System.currentTimeMillis() > laserTime) {
            laserTime = System.currentTimeMillis() + 750;
            if (!bugged || sabChanged) changeAmmo(getAttackKey());
            else {
                setRadiusAndClick(false);
                fixTimes++;
            }
        }
    }

    private boolean shouldSab() {
        return defense.SAB.ENABLED && hero.getHealth().shieldPercent() <= defense.SAB.PERCENT
        && target.getHealth().getShield() > defense.SAB.NPC_AMOUNT
        && (defense.SAB.CONDITION == null || defense.SAB.CONDITION.get(this.main.pluginAPI).toBoolean());
    }

    private char getAttackKey() {
        if (sab = shouldSab()) return defense.SAB.KEY;
        return defense.ammoKey;
    }

    private void changeAmmo(Character ammo) {
        API.keyboardClick(ammo);
        currentAmmo = ammo;
    }

    private void setRadiusAndClick(boolean single) {
        target.clickable.setRadius(800);
        drive.clickCenter(single, target.locationInfo.now);
        target.clickable.setRadius(0);
    }

    private void vsMove() {
        double distance = hero.locationInfo.now.distance(target.locationInfo.now);
        Location targetLoc = target.locationInfo.destinationInTime(400);

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

    private boolean useKeyWithConditions(ExtraKeyConditions extra) {
        if (System.currentTimeMillis() - clickDelay < 1000) return false;

        if (extra.Key != null && extra.lastUse < System.currentTimeMillis() - (extra.countdown*1000)) {
            if (hero.health.hpPercent() < extra.HEALTH_RANGE.max && hero.health.hpPercent() > extra.HEALTH_RANGE.min
                    && target.health.hpPercent() < extra.HEALTH_ENEMY_RANGE.max && target.health.hpPercent() > extra.HEALTH_ENEMY_RANGE.min) {
                API.keyboardClick(extra.Key);
                clickDelay = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }

    protected void mixerRSB() {
        if (defense.RSB != null) {
            if (lastRsbUse < System.currentTimeMillis() - 3500 && currentAmmo != defense.RSB) {
                changeAmmo(defense.RSB);
                lastRsbUse = System.currentTimeMillis();
                return;
            } else if (currentAmmo == defense.RSB) {
                changeAmmo(getAttackKey());
                return;
            }
        }
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

            if (ships.isEmpty()) return false;

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

        if (target == null) resetDefenseData();

        return target != null;
    }

    private boolean inGroupAttacked(int id) {
        if (main.guiManager.group == null) return false;

        if (main.guiManager.group.group.isValid()) {
            for (GroupMember member : main.guiManager.group.group.members) {
                if (member.id == id && member.isAttacked) {
                    return true;
                }
            }
        }
        return false;
    }

    private void resetDefenseData() {
        attackConfigLost = false;
        currentAmmo = null;
        target = null;
    }

}
