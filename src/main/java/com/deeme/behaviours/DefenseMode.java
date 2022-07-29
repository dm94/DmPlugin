package com.deeme.behaviours;

import com.deeme.types.SharedFunctions;
import com.deeme.types.ShipAttacker;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deeme.types.config.Defense;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.PercentRange;
import eu.darkbot.api.config.types.ShipMode;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.items.SelectableItem.Special;
import eu.darkbot.api.game.other.EntityInfo.Diplomacy;
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
    protected final Collection<? extends Player> players;
    protected final ConfigSetting<ShipMode> configOffensive;
    protected final ConfigSetting<ShipMode> configRun;
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
    public DefenseMode(PluginAPI api, HeroAPI hero, MovementAPI movement, AuthAPI auth, ConfigAPI configApi,
            EntitiesAPI entities, SafetyFinder safety) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog();

        this.api = api;
        this.heroapi = hero;
        this.movement = movement;
        this.safetyFinder = safety;
        this.players = entities.getPlayers();
        this.configOffensive = configApi.requireConfig("general.offensive");
        this.configRun = configApi.requireConfig("general.run");
        this.repairHpRange = configApi.requireConfig("general.safety.repair_hp_range");
        setup();
    }

    @Override
    public void setConfig(ConfigSetting<Defense> arg0) {
        this.defenseConfig = arg0.getValue();
        setup();
    }

    private void setup() {
        if (api == null || defenseConfig == null)
            return;

        this.shipAttacker = new ShipAttacker(api, defenseConfig);
    }

    @Override
    public void onTickBehavior() {
        if (shipAttacker != null && isUnderAttack()) {
            setConfigToUse();

            shipAttacker.doKillTargetTick();

            if (heroapi.getLocationInfo().distanceTo(shipAttacker.getTarget()) < 575) {
                shipAttacker.useKeyWithConditions(defenseConfig.ability, null);
            }

            if (defenseConfig.useBestRocket) {
                shipAttacker.changeRocket();
            }

            if (defenseConfig.useAbility) {
                shipAttacker.useHability();
            }

            shipAttacker.useKeyWithConditions(defenseConfig.ISH, Special.ISH_01);
            shipAttacker.useKeyWithConditions(defenseConfig.SMB, Special.SMB_01);
            shipAttacker.useKeyWithConditions(defenseConfig.PEM, Special.EMP_01);
            shipAttacker.useKeyWithConditions(defenseConfig.otherKey, null);

            shipAttacker.tryAttackOrFix();

            switch (defenseConfig.movementMode) {
                case 1:
                    shipAttacker.vsMove();
                    break;
                case 2:
                    safetyFinder.tick();
                    break;
                case 3:
                    if (!movement.isMoving() || movement.isOutOfMap()) {
                        movement.moveRandom();
                    }
                    break;
                case 4:
                    if (heroapi.getHealth().hpPercent() <= repairHpRange.getValue().getMin()) {
                        safetyFinder.tick();
                    } else {
                        shipAttacker.vsMove();
                    }
                    break;
            }
        }
    }

    private boolean isUnderAttack() {
        if (shipAttacker.getTarget() != null) {
            if (shipAttacker.getTarget().isValid() && shipAttacker.getTarget().getEntityInfo().isEnemy()) {
                return true;
            } else {
                shipAttacker.setTarget(null);
                return false;
            }
        }

        Entity target = SharedFunctions.getAttacker(heroapi, players, heroapi);
        if (target != null) {
            shipAttacker.setTarget((Ship) target);
            return true;
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
                    if (!(tar instanceof Npc)) {
                        shipAttacker.setTarget((Ship) tar);
                        return true;
                    }
                }

                target = SharedFunctions.getAttacker(ship, players, heroapi);
                if (target != null) {
                    shipAttacker.setTarget((Ship) target);
                    return true;
                }
            }
        }

        if (defenseConfig.goToGroup) {
            shipAttacker.goToMemberAttacked();
        }
        shipAttacker.resetDefenseData();

        return shipAttacker.getTarget() != null;
    }

    private void setConfigToUse() {
        if (defenseConfig.useSecondConfig && heroapi.getHealth().hpPercent() <= defenseConfig.healthToChange
                && heroapi.getHealth().shieldPercent() <= 0.1) {
            attackConfigLost = true;
        }

        if (attackConfigLost && defenseConfig.useSecondConfig) {
            shipAttacker.setMode(defenseConfig.secondConfig);
        } else {
            switch (defenseConfig.movementMode) {
                case 1:
                case 3:
                    shipAttacker.setMode(configOffensive.getValue(), defenseConfig.useBestFormation);
                    break;
                case 0:
                case 2:
                    shipAttacker.setMode(configRun.getValue(), defenseConfig.useBestFormation);
                    break;
                case 4:
                    if (heroapi.getHealth().hpPercent() <= repairHpRange.getValue().getMin()) {
                        shipAttacker.setMode(configRun.getValue(), defenseConfig.useBestFormation);
                    } else {
                        shipAttacker.setMode(configOffensive.getValue(), defenseConfig.useBestFormation);
                    }
                    break;
            }
        }
    }
}
