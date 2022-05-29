package com.deeme.behaviours;

import com.deeme.types.SharedFunctions;
import com.deeme.types.ShipAttacker;
import com.deeme.types.VerifierChecker;
import com.deeme.types.config.Defense;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.PercentRange;
import eu.darkbot.api.config.types.ShipMode;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.items.SelectableItem.Special;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.utils.SafetyFinder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Feature(name = "Defense Mode", description = "Add enemy defense options")
public class DefenseMode implements Behavior, Configurable<Defense> {
    protected final PluginAPI api;
    protected final HeroAPI heroapi;
    protected final MovementAPI movement;
    protected final SafetyFinder safetyFinder;
    protected final Collection<? extends Ship> allShips;
    protected final ConfigSetting<ShipMode> configOffensive;
    protected final ConfigSetting<PercentRange> repairHpRange;
    private ShipAttacker shipAttacker;
    private Defense defenseConfig;
    private boolean attackConfigLost = false;

    public DefenseMode(PluginAPI api) {
        this(api, api.requireAPI(HeroAPI.class),
                api.requireAPI(MovementAPI.class),
                api.requireAPI(AuthAPI.class),
                api.requireAPI(ConfigAPI.class),
                api.requireAPI(EntitiesAPI.class),
                api.requireInstance(SafetyFinder.class));
    }

    @Inject
    public DefenseMode(PluginAPI api, HeroAPI hero, MovementAPI movement, AuthAPI auth, ConfigAPI configApi, EntitiesAPI entities, SafetyFinder safety) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        this.api = api;
        this.heroapi = hero;
        this.movement = movement;
        this.safetyFinder = safety;
        this.allShips = entities.getShips();
        this.configOffensive = configApi.requireConfig("general.offensive");
        this.repairHpRange = configApi.requireConfig("general.safety.repair_hp_range");
        setup();
    }

    @Override
    public void setConfig(ConfigSetting<Defense> arg0) {
        this.defenseConfig = arg0.getValue();
        setup();
    }

    private void setup() {
        if (api == null || defenseConfig == null) return;

        this.shipAttacker = new ShipAttacker(api, defenseConfig);
    }

    @Override
    public void onTickBehavior() {
        if (shipAttacker != null) {
            isUnderAttack();
            if (shipAttacker.getTarget() == null) return;
            setConfigToUse();

            shipAttacker.doKillTargetTick();

            if (heroapi.getLocationInfo().distanceTo(shipAttacker.getTarget()) < 575) {
                shipAttacker.useKeyWithConditions(defenseConfig.ability, null);
            }

            if (defenseConfig.useBestRocket) {
                shipAttacker.changeRocket();
            }

            shipAttacker.useKeyWithConditions(defenseConfig.ISH, Special.ISH_01);
            shipAttacker.useKeyWithConditions(defenseConfig.SMB, Special.SMB_01);
            shipAttacker.useKeyWithConditions(defenseConfig.PEM, Special.EMP_01);
            shipAttacker.useKeyWithConditions(defenseConfig.otherKey, null);

            shipAttacker.tryAttackOrFix();

            if (defenseConfig.movementMode == 1) {
                shipAttacker.vsMove();
            } else if (defenseConfig.movementMode == 2) {
                safetyFinder.tick();
            } else if (defenseConfig.movementMode == 3) {
                if (!movement.isMoving() || movement.isOutOfMap()) movement.moveRandom();
            } else if (defenseConfig.movementMode == 4) {
                if (heroapi.getHealth().hpPercent() <= repairHpRange.getValue().getMin()){
                    safetyFinder.tick();
                } else {
                    shipAttacker.vsMove();
                }
            }
        }
    }

    private boolean isUnderAttack() {
        if (shipAttacker.getTarget() != null && !shipAttacker.getTarget().isValid() &&
            shipAttacker.getTarget().getEntityInfo().isEnemy()) {
            return true;
        }

        Ship tempShip = SharedFunctions.getAttacker(heroapi,allShips, heroapi);
        if (tempShip != null) {
            shipAttacker.setTarget(tempShip);
            return true;
        }

        if (shipAttacker.getTarget() == null) {
            List<Ship> ships = allShips.stream()
                    .filter(s -> (defenseConfig.helpAllies && s.getEntityInfo().getClanDiplomacy().ordinal() == 1) ||
                            (defenseConfig.helpEveryone && !s.getEntityInfo().isEnemy())
                            || (defenseConfig.helpGroup && shipAttacker.inGroupAttacked(s.getId())))
                    .collect(Collectors.toList());

            if (!ships.isEmpty()) {
                for (Ship ship : ships) {
                    if (defenseConfig.helpAttack && ship.isAttacking() && ship.getTarget() != null) {
                        shipAttacker.setTarget((Ship) ship.getTarget());
                        return true;
                    }

                    tempShip = SharedFunctions.getAttacker(ship, allShips, heroapi);
                    if (tempShip != null) {
                        shipAttacker.setTarget(tempShip);
                        return true;
                    }
                }
            }
        }

        if (shipAttacker.getTarget() == null) {
            if (defenseConfig.goToGroup) {
                shipAttacker.goToMemberAttacked();
            }
            shipAttacker.resetDefenseData();
        }

        return shipAttacker.getTarget() != null;
    }

    private void setConfigToUse() {
        if (defenseConfig.useSecondConfig &&
            heroapi.getHealth().shieldPercent() < 0.1 && defenseConfig.healthToChange <= heroapi.getHealth().shieldPercent()){
            attackConfigLost = true;
        }

        if (attackConfigLost && defenseConfig.useSecondConfig) {
            shipAttacker.setMode(defenseConfig.secondConfig, false);
        } else {
            shipAttacker.setMode(configOffensive.getValue());
        }
    }
}
