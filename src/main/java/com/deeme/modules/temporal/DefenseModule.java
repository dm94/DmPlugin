package com.deeme.modules.temporal;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.deeme.types.SharedFunctions;
import com.deeme.types.ShipAttacker;
import com.deeme.types.config.Defense;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.PercentRange;
import eu.darkbot.api.config.types.ShipMode;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.group.GroupMember;
import eu.darkbot.api.game.items.SelectableItem.Special;
import eu.darkbot.api.game.other.EntityInfo.Diplomacy;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.TemporalModule;
import eu.darkbot.shared.utils.SafetyFinder;

public class DefenseModule extends TemporalModule {
    protected final PluginAPI api;
    protected final HeroAPI heroapi;
    protected final MovementAPI movement;
    protected final SafetyFinder safetyFinder;
    protected final Collection<? extends Player> players;
    protected final ConfigSetting<ShipMode> configOffensive;
    protected final ConfigSetting<ShipMode> configRun;
    protected final ConfigSetting<PercentRange> repairHpRange;
    private ShipAttacker shipAttacker;
    private Defense defenseConfig;
    private boolean attackConfigLost = false;
    private Entity target = null;

    public DefenseModule(PluginAPI api, Defense defenseConfig, Entity target) {
        this(api, api.requireAPI(BotAPI.class),
                api.requireAPI(HeroAPI.class),
                api.requireAPI(MovementAPI.class),
                api.requireAPI(ConfigAPI.class),
                api.requireAPI(EntitiesAPI.class),
                api.requireInstance(SafetyFinder.class), defenseConfig, target);

    }

    @Inject
    public DefenseModule(PluginAPI api, BotAPI bot, HeroAPI hero, MovementAPI movement, ConfigAPI configApi,
            EntitiesAPI entities, SafetyFinder safety, Defense defenseConfig, Entity target) {
        super(bot);
        this.api = api;
        this.heroapi = hero;
        this.movement = movement;
        this.safetyFinder = safety;
        this.players = entities.getPlayers();
        this.configOffensive = configApi.requireConfig("general.offensive");
        this.configRun = configApi.requireConfig("general.run");
        this.repairHpRange = configApi.requireConfig("general.safety.repair_hp_range");
        this.defenseConfig = defenseConfig;
        this.shipAttacker = new ShipAttacker(api, defenseConfig);
        this.target = target;
    }

    @Override
    public boolean canRefresh() {
        return false;
    }

    @Override
    public String getStatus() {
        return "Defense Mode | " + shipAttacker.getStatus() + " | " + safetyFinder.status();
    }

    @Override
    public void onTickModule() {
        try {
            if (isUnderAttack()) {
                setConfigToUse();
                shipAttacker.tryLockAndAttack();

                shipAttacker.useKeyWithConditions(defenseConfig.ability, null);

                shipAttacker.useKeyWithConditions(defenseConfig.ability, null);
                shipAttacker.useKeyWithConditions(defenseConfig.ISH, Special.ISH_01);
                shipAttacker.useKeyWithConditions(defenseConfig.SMB, Special.SMB_01);
                shipAttacker.useKeyWithConditions(defenseConfig.PEM, Special.EMP_01);
                shipAttacker.useKeyWithConditions(defenseConfig.otherKey, null);
                shipAttacker.tryAttackOrFix();
                movementLogic();
            } else {
                target = null;
                super.goBack();
            }
        } catch (Exception e) {
            System.out.println(e);
            super.goBack();
        }
    }

    private boolean isUnderAttack() {
        if (shipAttacker.getTarget() != null && shipAttacker.getTarget().isValid()
                && shipAttacker.getTarget().getEntityInfo().isEnemy()
                && (!defenseConfig.ignoreEnemies
                        || shipAttacker.getTarget().getLocationInfo().distanceTo(heroapi) < 1500)) {
            return true;
        }

        if (target != null && target.isValid()
                && (!defenseConfig.ignoreEnemies || target.getLocationInfo().distanceTo(heroapi) < 1500)) {
            shipAttacker.setTarget((Ship) target);
            return true;
        }

        Entity newTarget = null;
        if (defenseConfig.respondAttacks) {
            newTarget = SharedFunctions.getAttacker(heroapi, players, heroapi);
            if (newTarget != null && newTarget.isValid()) {
                shipAttacker.setTarget((Ship) target);
                return true;
            }
        }

        List<Player> ships = players.stream()
                .filter(s -> (defenseConfig.helpAllies && s.getEntityInfo().getClanDiplomacy() == Diplomacy.ALLIED)
                        ||
                        (defenseConfig.helpEveryone && !s.getEntityInfo().isEnemy())
                        || (defenseConfig.helpGroup && shipAttacker.inGroupAttacked(s.getId())))
                .collect(Collectors.toList());

        if (!ships.isEmpty()) {
            for (Player ship : ships) {
                if (defenseConfig.helpAttack && ship.isAttacking() && ship.getTarget() != null) {
                    Entity tar = ship.getTarget();
                    if (!(tar instanceof Npc) && tar.isValid()) {
                        shipAttacker.setTarget((Ship) tar);
                        return true;
                    }
                }

                newTarget = SharedFunctions.getAttacker(ship, players, heroapi);
                if (newTarget != null) {
                    shipAttacker.setTarget((Ship) target);
                    return true;
                }
            }
        }

        if (defenseConfig.goToGroup) {
            shipAttacker.goToMemberAttacked();
        }
        shipAttacker.resetDefenseData();

        if (shipAttacker.getTarget() != null && shipAttacker.getTarget().isValid()
                && (!defenseConfig.ignoreEnemies
                        || shipAttacker.getTarget().getLocationInfo().distanceTo(heroapi) < 1500)) {
            shipAttacker.setTarget(null);
        }

        return shipAttacker.getTarget() != null;
    }

    private void setConfigToUse() {
        if (defenseConfig.useSecondConfig && heroapi.getHealth().hpPercent() <= defenseConfig.healthToChange
                && heroapi.getHealth().shieldPercent() <= 0.1) {
            attackConfigLost = true;
        }

        if (attackConfigLost && defenseConfig.useSecondConfig) {
            heroapi.setMode(defenseConfig.secondConfig);
        } else {
            switch (defenseConfig.newMovementMode) {
                case 0:
                    heroapi.setMode(configRun.getValue());
                    break;
                case 1:
                case 2:
                case 5:
                    heroapi.setMode(configOffensive.getValue());
                    break;
                case 4:
                case 3:
                    if (heroapi.getHealth().hpPercent() <= repairHpRange.getValue().getMin()) {
                        heroapi.setMode(configRun.getValue());
                    } else {
                        heroapi.setMode(configOffensive.getValue());
                    }
                    break;
            }
        }
    }

    private void movementLogic() {
        switch (defenseConfig.newMovementMode) {
            case 0:
                safetyFinder.tick();
                break;
            case 1:
                shipAttacker.vsMove();
                break;
            case 2:
                if (!movement.isMoving() || movement.isOutOfMap()) {
                    movement.moveRandom();
                }
                break;
            case 3:
                if (heroapi.getHealth().hpPercent() <= repairHpRange.getValue().getMin()) {
                    safetyFinder.tick();
                } else {
                    shipAttacker.vsMove();
                }
                break;
            case 4:
                if (heroapi.getHealth().hpPercent() <= repairHpRange.getValue().getMin()) {
                    safetyFinder.tick();
                } else {
                    GroupMember groupMember = shipAttacker.getClosestMember();
                    if (groupMember != null) {
                        if (groupMember.getLocation().distanceTo(heroapi) < 1000) {
                            shipAttacker.vsMove();
                        } else {
                            movement.moveTo(groupMember.getLocation());
                        }
                    } else {
                        shipAttacker.vsMove();
                    }
                }
                break;
            case 5:
                GroupMember groupMember = shipAttacker.getClosestMember();
                if (groupMember != null) {
                    if (groupMember.getLocation().distanceTo(heroapi) < 1000) {
                        shipAttacker.vsMove();
                    } else {
                        movement.moveTo(groupMember.getLocation());
                    }
                } else {
                    shipAttacker.vsMove();
                }
                break;
        }
    }

}
