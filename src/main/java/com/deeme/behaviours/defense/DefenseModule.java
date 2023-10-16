package com.deeme.behaviours.defense;

import java.util.Collection;

import com.deeme.types.ShipAttacker;
import com.deemetool.general.movement.ExtraMovementLogic;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.PercentRange;
import eu.darkbot.api.config.types.ShipMode;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.items.SelectableItem.Special;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.managers.PetAPI;
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
    protected final PetAPI pet;

    private ShipAttacker shipAttacker;
    private DefenseConfig defenseConfig;
    private boolean attackConfigLost = false;
    private Entity target = null;

    private long nextAttackCheck = 0;
    private ExtraMovementLogic extraMovementLogic;

    private int timeOut = 0;
    protected static final int DISTANCE_TO_USE_VS_MODE = 1500;

    public DefenseModule(PluginAPI api, DefenseConfig defenseConfig, Entity target) {
        this(api, api.requireAPI(BotAPI.class),
                api.requireAPI(HeroAPI.class), defenseConfig, target);
    }

    @Inject
    public DefenseModule(PluginAPI api, BotAPI bot, HeroAPI hero, DefenseConfig defenseConfig, Entity target) {
        super(bot);
        this.api = api;
        this.heroapi = hero;
        this.movement = api.requireAPI(MovementAPI.class);
        this.safetyFinder = api.requireInstance(SafetyFinder.class);
        this.pet = api.getAPI(PetAPI.class);
        EntitiesAPI entities = api.requireAPI(EntitiesAPI.class);
        ConfigAPI configApi = api.requireAPI(ConfigAPI.class);
        this.players = entities.getPlayers();
        this.configOffensive = configApi.requireConfig("general.offensive");
        this.configRun = configApi.requireConfig("general.run");
        this.repairHpRange = configApi.requireConfig("general.safety.repair_hp_range");
        this.defenseConfig = defenseConfig;
        this.shipAttacker = new ShipAttacker(api, defenseConfig);
        this.extraMovementLogic = new ExtraMovementLogic(api, defenseConfig.movementConfig);
        this.target = target;
        this.nextAttackCheck = 0;
        this.timeOut = 0;
    }

    @Override
    public boolean canRefresh() {
        return false;
    }

    @Override
    public String getStatus() {
        return "Defense Mode | " + shipAttacker.getStatus() + " | " + safetyFinder.status() + " | Time out:" + timeOut
                + "/" + defenseConfig.maxSecondsTimeOut;
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
            System.err.println(e.getLocalizedMessage());
            target = null;
            super.goBack();
        }
    }

    private void timeOutCheck() {
        if (nextAttackCheck < System.currentTimeMillis()) {
            nextAttackCheck = System.currentTimeMillis() + 1000;
            if (shipAttacker.getTarget() != null && heroapi.isAttacking(shipAttacker.getTarget())
                    && shipAttacker.getTarget().getLocationInfo()
                            .distanceTo(heroapi) < defenseConfig.rangeForAttackedEnemy) {
                timeOut = 0;
            } else {
                timeOut++;
                if (timeOut >= defenseConfig.maxSecondsTimeOut) {
                    target = null;
                    super.goBack();
                }
            }
        }
    }

    private boolean isUnderAttack() {
        if (target == null
                || target.getId() == heroapi.getId() || !target.isValid()) {
            return false;
        }

        if (shipAttacker.getTarget() != null && shipAttacker.getTarget().isValid()
                && shipAttacker.getTarget().getId() != heroapi.getId()
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
        if (heroapi.getHealth().hpPercent() <= defenseConfig.healthToChange) {
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
}
