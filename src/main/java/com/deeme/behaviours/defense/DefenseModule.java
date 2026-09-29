package com.deeme.behaviours.defense;

import com.deeme.types.ShipAttacker;
import com.deeme.shared.TargetEngagementTracker;
import com.deeme.shared.movement.ExtraMovementLogic;
import com.deeme.shared.configchanger.ExtraCChangerLogic;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Pet;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.items.SelectableItem.Special;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.managers.PetAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.TemporalModule;

public class DefenseModule extends TemporalModule {
    private final HeroAPI heroapi;
    private final MovementAPI movement;
    private final PetAPI pet;

    private ShipAttacker shipAttacker;
    private DefenseConfig defenseConfig;
    private Entity target = null;

    private final TargetEngagementTracker engagementTracker;
    private final ExtraMovementLogic extraMovementLogic;
    private final ExtraCChangerLogic extraConfigChangerLogic;

    public DefenseModule(PluginAPI api, DefenseConfig defenseConfig, Entity target) {
        this(api, api.requireAPI(BotAPI.class),
                api.requireAPI(HeroAPI.class), defenseConfig, target);
    }

    @Inject
    public DefenseModule(PluginAPI api, BotAPI bot, HeroAPI hero, DefenseConfig defenseConfig, Entity target) {
        super(bot);
        this.heroapi = hero;
        this.movement = api.requireAPI(MovementAPI.class);
        this.pet = api.requireAPI(PetAPI.class);
        this.defenseConfig = defenseConfig;
        this.shipAttacker = new ShipAttacker(api, defenseConfig.ammoConfig, defenseConfig.humanizer);
        this.extraMovementLogic = new ExtraMovementLogic(api, defenseConfig.movementConfig);
        this.extraConfigChangerLogic = new ExtraCChangerLogic(api, defenseConfig.extraConfigChangerConfig);
        this.target = target;
        this.engagementTracker = new TargetEngagementTracker(hero);
    }

    @Override
    public boolean canRefresh() {
        return false;
    }

    @Override
    public String getStatus() {
        return "Defense Mode | " + shipAttacker.getStatus() + " | Time out:"
                + engagementTracker.getStatus(defenseConfig.maxSecondsTimeOut);
    }

    @Override
    public void onTickModule() {
        pet.setEnabled(true);
        try {
            if (isUnderAttack()) {
                timeOutCheck();
                setConfigToUse();
                shipAttacker.tryLockAndAttack();

                shipAttacker.useKeyWithConditions(defenseConfig.ability, null);
                shipAttacker.useKeyWithConditions(defenseConfig.ISH, Special.ISH_01);
                shipAttacker.useKeyWithConditions(defenseConfig.SMB, Special.SMB_01);
                shipAttacker.useKeyWithConditions(defenseConfig.PEM, Special.EMP_01);
                shipAttacker.useKeyWithConditions(defenseConfig.selectable1);
                shipAttacker.useKeyWithConditions(defenseConfig.selectable2);
                shipAttacker.useKeyWithConditions(defenseConfig.selectable3);
                shipAttacker.useKeyWithConditions(defenseConfig.selectable4);
                shipAttacker.useKeyWithConditions(defenseConfig.selectable5);
                shipAttacker.tryAttackOrFix();
                this.extraMovementLogic.tick();
            } else {
                target = null;
                super.goBack();
            }
        } catch (Exception e) {
            target = null;
            super.goBack();
        }
    }

    private void timeOutCheck() {
        if (engagementTracker.isTimeOut(shipAttacker.getTarget(), defenseConfig.maxSecondsTimeOut,
                defenseConfig.rangeForAttackedEnemy)) {
            target = null;
            super.goBack();
        }
    }

    private boolean isUnderAttack() {
        if (target == null
                || target.getId() == heroapi.getId() || !target.isValid() || target instanceof Pet) {
            return false;
        }

        if (shipAttacker.getTarget() != null && shipAttacker.getTarget().isValid()
                && shipAttacker.getTarget().getId() != heroapi.getId() && !(shipAttacker.getTarget() instanceof Pet)
                && (!defenseConfig.ignoreEnemies
                        || shipAttacker.getTarget().getLocationInfo()
                                .distanceTo(heroapi) < defenseConfig.rangeForAttackedEnemy)) {
            return true;
        }

        if (target.isValid()
                && target.getLocationInfo().distanceTo(heroapi) < defenseConfig.rangeForAttackedEnemy
                && movement.canMove(target)) {
            shipAttacker.setTarget((Ship) target);
            return true;
        }

        shipAttacker.resetDefenseData();
        return false;
    }

    private void setConfigToUse() {
        heroapi.setMode(extraConfigChangerLogic.getShipMode());
    }
}
