package com.deeme.behaviours.defense;

import java.util.Collection;

import com.deeme.types.ShipAttacker;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.PercentRange;
import eu.darkbot.api.config.types.ShipMode;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.group.GroupMember;
import eu.darkbot.api.game.items.SelectableItem.Special;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.TemporalModule;
import eu.darkbot.shared.utils.SafetyFinder;
import eu.darkbot.shared.utils.SafetyFinder.Escaping;

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
    private DefenseConfig defenseConfig;
    private boolean attackConfigLost = false;
    private Entity target = null;

    public DefenseModule(PluginAPI api, DefenseConfig defenseConfig, Entity target) {
        this(api, api.requireAPI(BotAPI.class),
                api.requireAPI(HeroAPI.class),
                api.requireAPI(MovementAPI.class),
                api.requireAPI(ConfigAPI.class),
                api.requireAPI(EntitiesAPI.class),
                api.requireInstance(SafetyFinder.class), defenseConfig, target);

    }

    @Inject
    public DefenseModule(PluginAPI api, BotAPI bot, HeroAPI hero, MovementAPI movement, ConfigAPI configApi,
            EntitiesAPI entities, SafetyFinder safety, DefenseConfig defenseConfig, Entity target) {
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
                shipAttacker.useKeyWithConditions(defenseConfig.ISH, Special.ISH_01);
                shipAttacker.useKeyWithConditions(defenseConfig.SMB, Special.SMB_01);
                shipAttacker.useKeyWithConditions(defenseConfig.PEM, Special.EMP_01);
                shipAttacker.tryAttackOrFix();
                movementLogic();
            } else {
                target = null;
                super.goBack();
            }
        } catch (Exception e) {
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

        shipAttacker.resetDefenseData();
        return false;
    }

    private void setConfigToUse() {
        if (heroapi.getHealth().hpPercent() <= defenseConfig.healthToChange
                && heroapi.getHealth().shieldPercent() <= 0.1) {
            attackConfigLost = true;
        }

        if (attackConfigLost) {
            heroapi.setMode(configRun.getValue());
        } else {
            if (safetyFinder.state() != Escaping.ENEMY) {
                heroapi.setMode(configOffensive.getValue());
            } else {
                heroapi.setMode(configRun.getValue());
            }
        }
    }

    private void movementLogic() {
        switch (defenseConfig.movementMode) {
            case VSSAFETY:
                if (!safetyFinder.tick()) {
                    break;
                }
            case VS:
                shipAttacker.vsMove();
                break;
            case RANDOM:
                if (!movement.isMoving() || movement.isOutOfMap()) {
                    movement.moveRandom();
                }
                break;
            case GROUPVSSAFETY:
                if (safetyFinder.tick()) {
                    GroupMember groupMember = shipAttacker.getClosestMember();
                    if (groupMember != null) {
                        if (groupMember.getLocation().distanceTo(heroapi) < 1500) {
                            shipAttacker.vsMove();
                        } else {
                            movement.moveTo(groupMember.getLocation());
                        }
                    } else {
                        shipAttacker.vsMove();
                    }
                }
                break;
            case GROUPVS:
                GroupMember groupMember = shipAttacker.getClosestMember();
                if (groupMember != null) {
                    if (groupMember.getLocation().distanceTo(heroapi) < 1500) {
                        shipAttacker.vsMove();
                    } else {
                        movement.moveTo(groupMember.getLocation());
                    }
                } else {
                    shipAttacker.vsMove();
                }
                break;
            default:
                if (safetyFinder.tick()) {
                    shipAttacker.vsMove();
                }
                break;
        }
    }

}
