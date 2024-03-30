package com.deeme.types;

import com.deeme.behaviours.defense.AmmoConfig;
import com.deeme.modules.sentinel.Humanizer;
import com.deeme.types.config.ExtraKeyConditions;
import com.deeme.types.config.ExtraKeyConditionsSelectable;
import com.deeme.types.suppliers.DefenseLaserSupplier;
import com.github.manolo8.darkbot.core.api.Capability;
import com.github.manolo8.darkbot.core.objects.facades.SettingsProxy;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.types.ShipMode;
import eu.darkbot.api.config.types.ShipMode.ShipModeImpl;
import eu.darkbot.api.game.entities.Pet;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.enums.EntityEffect;
import eu.darkbot.api.game.group.GroupMember;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Formation;
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.GroupAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.managers.MovementAPI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Random;
import java.util.Optional;

import static com.github.manolo8.darkbot.Main.API;

public class ShipAttacker {
    private final HeroAPI heroapi;
    private final HeroItemsAPI items;
    private final MovementAPI movement;
    private final ConfigAPI configAPI;
    private final GroupAPI group;
    private final SettingsProxy settingsProxy;
    private final Collection<? extends Player> allShips;
    private final Collection<? extends Portal> allPortals;

    private DefenseLaserSupplier laserSupplier;
    private ConditionsManagement conditionsManagement;
    private Ship target;

    private long laserTime;
    private long clickDelay;
    private long keyDelay;

    private Random rnd;

    private int lastAttacked;

    private boolean firstAttack;
    private long isAttacking;
    private int fixedTimes;

    private Character attackLaserKey;

    private Humanizer humanizerConfig;
    private AmmoConfig ammoConfig;

    public ShipAttacker(PluginAPI api, AmmoConfig ammoConfig, Humanizer humanizerConfig) {
        this.heroapi = api.requireAPI(HeroAPI.class);
        this.movement = api.requireAPI(MovementAPI.class);
        this.configAPI = api.requireAPI(ConfigAPI.class);
        this.group = api.requireAPI(GroupAPI.class);
        EntitiesAPI entities = api.requireAPI(EntitiesAPI.class);
        this.allShips = entities.getPlayers();
        this.allPortals = entities.getPortals();
        this.rnd = new Random();
        this.items = api.requireAPI(HeroItemsAPI.class);
        this.humanizerConfig = new Humanizer();
        this.humanizerConfig.maxRandomTime = 0;

        this.settingsProxy = api.requireInstance(SettingsProxy.class);
        attackLaserKey = settingsProxy.getCharacterOf(SettingsProxy.KeyBind.ATTACK_LASER).orElse(null);

        this.conditionsManagement = new ConditionsManagement(api, items);
        this.humanizerConfig = humanizerConfig;
        this.ammoConfig = ammoConfig;
        this.laserSupplier = new DefenseLaserSupplier(api, heroapi, items, ammoConfig.sab, ammoConfig.useRSB);
    }

    public String getStatus() {
        return getTarget() != null ? "Killing " + getTarget().getEntityInfo().getUsername() : "Idle";
    }

    public Ship getTarget() {
        if (target != null && target.isValid()) {
            return target;
        } else {
            return null;
        }
    }

    private void tryLockTarget() {
        if (target == null || !target.isValid()) {
            return;
        }

        if (lastAttacked != target.getId()) {
            lastAttacked = target.getId();
            firstAttack = true;

            clickDelay = System.currentTimeMillis();
            if (humanizerConfig.addRandomTime) {
                clickDelay = System.currentTimeMillis() + (rnd.nextInt(humanizerConfig.maxRandomTime) * 1000);
            }
        }

        fixedTimes = 0;
        laserTime = 0;
        firstAttack = false;
        if (heroapi.getLocationInfo().distanceTo(target) < 700
                && (heroapi.getTarget() == null || heroapi.getTarget().getId() != target.getId())) {
            if (System.currentTimeMillis() > clickDelay) {
                heroapi.setLocalTarget(target);
                target.trySelect(false);
                clickDelay = System.currentTimeMillis();
            }
        } else {
            movement.moveTo(target.getDestination().orElse(target.getLocationInfo()));
        }
    }

    public void setTarget(Ship target) {
        this.target = target;
    }

    public void tryLockAndAttack() {
        if (target == null) {
            return;
        }

        if (heroapi.isAttacking(target)) {
            tryAttackOrFix();
        } else {
            tryLockTarget();
        }
    }

    public void tryAttackOrFix() {
        if (System.currentTimeMillis() < laserTime) {
            return;
        }
        if (clickDelay > System.currentTimeMillis()) {
            return;
        }

        Optional<SelectableItem> lastLaser = getAttackItem();
        if (lastLaser.isPresent() && (heroapi.getLaser() == null || !heroapi.getLaser().equals(lastLaser.get()))) {
            conditionsManagement.useSelectableReadyWhenReady(lastLaser.get());
        }

        if (!firstAttack) {
            firstAttack = true;
            sendAttack(1500, 5000, true);
        } else if (!heroapi.isAttacking(target) || !heroapi.isAiming(target)) {
            sendAttack(1500, 5000, false);
        } else if (target.getHealth().hpDecreasedIn(1500) || target.hasEffect(EntityEffect.NPC_ISH)
                || target.hasEffect(EntityEffect.ISH)
                || heroapi.getLocationInfo().distanceTo(target) > 700) {
            isAttacking = Math.max(isAttacking, System.currentTimeMillis() + 2000);
        } else if (System.currentTimeMillis() > isAttacking) {
            sendAttack(1500, ++fixedTimes * 3000L, false);
        }
    }

    private void sendAttack(long minWait, long bugTime, boolean normal) {
        laserTime = System.currentTimeMillis() + minWait;
        isAttacking = Math.max(isAttacking, laserTime + bugTime);

        if (normal) {
            API.keyboardClick(attackLaserKey);
        } else if (API.hasCapability(Capability.ALL_KEYBINDS_SUPPORT)) {
            this.settingsProxy.pressKeybind(SettingsProxy.KeyBind.ATTACK_LASER);
        } else if (target != null && target.isValid()) {
            target.trySelect(true);
        }
    }

    private Laser getBestLaserAmmo() {
        return laserSupplier.get();
    }

    private Optional<SelectableItem> getAttackItem() {
        if (!ammoConfig.enableAmmoConfig) {
            return Optional.empty();
        }

        Laser laser = getBestLaserAmmo();
        if (laser != null) {
            return Optional.of(laser);
        }

        if (ammoConfig != null) {
            return Optional.ofNullable(SharedFunctions.getItemById(ammoConfig.defaultLaser));
        }

        return Optional.empty();
    }

    public boolean useKeyWithConditions(ExtraKeyConditions extra, SelectableItem selectableItem) {
        if (extra.enable) {
            if (selectableItem == null && extra.key != null) {
                selectableItem = items.getItem(extra.key);
            }

            if (selectableItem != null && heroapi.getHealth().hpPercent() < extra.healthRange.max
                    && heroapi.getHealth().hpPercent() > extra.healthRange.min
                    && heroapi.getLocalTarget() != null
                    && heroapi.getLocalTarget().getHealth().hpPercent() < extra.healthEnemyRange.max
                    && heroapi.getLocalTarget().getHealth().hpPercent() > extra.healthEnemyRange.min) {

                if (System.currentTimeMillis() - keyDelay < 500) {
                    return false;
                }

                if (conditionsManagement.useSelectableReadyWhenReady(selectableItem)) {
                    keyDelay = System.currentTimeMillis();
                    return true;
                }
            }
        }
        return false;
    }

    public boolean useSelectableReadyWhenReady(SelectableItem selectableItem) {
        if (conditionsManagement.useSelectableReadyWhenReady(selectableItem)) {
            keyDelay = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public boolean useKeyWithConditions(ExtraKeyConditionsSelectable extra) {
        return conditionsManagement.useKeyWithConditions(extra);
    }

    public boolean inGroupAttacked(int id) {
        if (group.hasGroup()) {
            for (GroupMember member : group.getMembers()) {
                if (!member.isDead() && member.getId() == id && member.isAttacked()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean inGroup(int id) {
        if (group.hasGroup()) {
            return group.getMembers().stream().anyMatch(g -> g.getId() == id);
        }
        return false;
    }

    public void goToMemberAttacked() {
        GroupMember member = SharedFunctions.getMemberGroupAttacked(group, heroapi, configAPI);
        if (member != null) {
            movement.moveTo(member.getLocation());
        }
    }

    public void setMode(ShipMode config, Formation formation) {
        if (formation != null && !heroapi.isInFormation(formation)) {
            if (items.getItem(formation, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                ShipModeImpl mode = new ShipModeImpl(config.getConfiguration(), formation);
                heroapi.setMode(mode);
            } else {
                heroapi.setMode(config);
            }
        } else {
            heroapi.setMode(config);
        }
    }

    public Ship getEnemy(int maxDistance) {
        return getEnemy(maxDistance, new ArrayList<>());
    }

    public Ship getEnemy(int maxDistance, ArrayList<Integer> playersToIgnore) {
        boolean ableToAttack = heroapi.getMap().isPvp()
                || allPortals.stream().noneMatch(p -> heroapi.distanceTo(p) < 1500);
        return allShips.stream()
                .filter(Ship::isValid)
                .filter(s -> s.getId() != heroapi.getId()
                        && !playersToIgnore.contains(s.getId())
                        && s.getEntityInfo().isEnemy()
                        && (ableToAttack || s.isAttacking())
                        && !s.hasEffect(290)
                        && !(s instanceof Pet)
                        && !inGroup(s.getId())
                        && movement.canMove(s)
                        && s.getLocationInfo().distanceTo(heroapi) <= maxDistance)

                .sorted(Comparator.comparingDouble(s -> s.getLocationInfo().distanceTo(heroapi))).findAny()
                .orElse(null);
    }

    public void resetDefenseData() {
        target = null;
        fixedTimes = 0;
        firstAttack = true;
    }
}
