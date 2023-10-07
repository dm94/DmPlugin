package com.deeme.modules;

import com.deeme.modules.pvp.AntiPushLogic;
import com.deeme.modules.pvp.PVPConfig;
import com.deeme.types.SharedFunctions;
import com.deeme.types.ShipAttacker;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemetool.general.movement.ExtraMovementLogic;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.items.SelectableItem.Special;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.managers.PetAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.CollectorModule;
import eu.darkbot.shared.modules.MapModule;
import eu.darkbot.shared.utils.SafetyFinder;

import java.util.Arrays;
import java.util.Collection;

@Feature(name = "PVP Module", description = "It is limited so as not to spoil the game")
public class PVPModule implements Module, Configurable<PVPConfig> {
    private PVPConfig pvpConfig;
    private Ship target;
    private ShipAttacker shipAttacker;

    protected final PluginAPI api;
    protected final HeroAPI heroapi;
    protected final MovementAPI movement;
    protected final StarSystemAPI starSystem;
    protected final BotAPI bot;
    protected final PetAPI pet;

    protected final Collection<? extends Portal> portals;
    protected final Collection<? extends Player> players;

    protected final ConfigSetting<Integer> workingMap;

    private boolean attackConfigLost = false;
    protected boolean firstAttack;
    protected long isAttacking;
    protected int fixedTimes;
    protected Character lastShot;
    protected long laserTime;
    protected long fixTimes;
    protected long clickDelay;
    protected long lastTimeAttack = 0;

    private SafetyFinder safety;
    private ExtraMovementLogic extraMovementLogic;
    private double lastDistanceTarget = 1000;
    protected CollectorModule collectorModule;

    private boolean isConfigAttackFull = false;
    private boolean isCongigRunFull = false;

    private long nextAttackCheck = 0;
    private int timeOut = 0;

    private AntiPushLogic antiPushLogic;

    public PVPModule(PluginAPI api) {
        this(api, api.requireAPI(HeroAPI.class),
                api.requireAPI(AuthAPI.class),
                api.requireAPI(ConfigAPI.class),
                api.requireAPI(MovementAPI.class),
                api.requireInstance(SafetyFinder.class));
    }

    @Inject
    public PVPModule(PluginAPI api, HeroAPI hero, AuthAPI auth, ConfigAPI configApi, MovementAPI movement,
            SafetyFinder safety) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.discordCheck(api.getAPI(ExtensionsAPI.class).getFeatureInfo(this.getClass()), auth.getAuthId());
        Utils.showDonateDialog();

        this.api = api;
        this.heroapi = hero;
        this.safety = safety;
        this.movement = movement;
        this.starSystem = api.getAPI(StarSystemAPI.class);
        this.bot = api.getAPI(BotAPI.class);
        this.pet = api.getAPI(PetAPI.class);
        this.workingMap = configApi.requireConfig("general.working_map");

        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        this.portals = entities.getPortals();
        this.players = entities.getPlayers();

        this.collectorModule = new CollectorModule(api);

        setup();
    }

    @Override
    public String getStatus() {
        if (safety.state() != SafetyFinder.Escaping.NONE) {
            return safety.status();
        } else if (target != null) {
            return shipAttacker.getStatus() + " | Time out:" + timeOut
                    + "/" + pvpConfig.maxSecondsTimeOut;
        }
        return collectorModule.getStatus();
    }

    @Override
    public void setConfig(ConfigSetting<PVPConfig> arg0) {
        this.pvpConfig = arg0.getValue();
        setup();
    }

    @Override
    public boolean canRefresh() {
        if (pvpConfig.move && target == null && collectorModule.canRefresh()) {
            return safety.tick();
        }
        return false;
    }

    private void setup() {
        if (api == null || pvpConfig == null)
            return;

        this.shipAttacker = new ShipAttacker(api, pvpConfig.SAB, pvpConfig.useRSB, pvpConfig.humanizer);
        this.antiPushLogic = new AntiPushLogic(this.heroapi, api.getAPI(StatsAPI.class), this.pvpConfig.antiPush);
        this.extraMovementLogic = new ExtraMovementLogic(api, heroapi, movement, pvpConfig.movementConfig);
    }

    @Override
    public void onTickModule() {
        pet.setEnabled(true);
        if (!pvpConfig.move || (safety.tick() && checkMap())) {
            if (hasTarget()) {
                attackLogic();
            } else {
                attackConfigLost = false;
                target = null;
                shipAttacker.resetDefenseData();
                rechargeShields();
                roamingLogic();
            }
        }
        antiPushLogic.registerTarget(target);
    }

    private void attackLogic() {
        isCongigRunFull = false;
        isConfigAttackFull = false;
        lastTimeAttack = System.currentTimeMillis();
        if (pvpConfig.changeConfig) {
            setConfigToUse();
        }

        shipAttacker.tryLockAndAttack();

        shipAttacker.useKeyWithConditions(pvpConfig.ability, null);
        shipAttacker.useKeyWithConditions(pvpConfig.ISH, Special.ISH_01);
        shipAttacker.useKeyWithConditions(pvpConfig.SMB, Special.SMB_01);
        shipAttacker.useKeyWithConditions(pvpConfig.PEM, Special.EMP_01);

        shipAttacker.tryAttackOrFix();

        if (pvpConfig.move) {
            extraMovementLogic.tick();
        }
        timeOutCheck();
    }

    private void timeOutCheck() {
        if (nextAttackCheck < System.currentTimeMillis()) {
            nextAttackCheck = System.currentTimeMillis() + 1000;
            if (heroapi.isAttacking(shipAttacker.getTarget())) {
                timeOut = 0;
            } else {
                timeOut++;
                if (timeOut >= pvpConfig.maxSecondsTimeOut) {
                    target = null;
                }
            }
        }
    }

    private void rechargeShields() {
        if (pvpConfig.rechargeShields) {
            if (!isConfigAttackFull) {
                heroapi.setAttackMode(null);
                if ((heroapi.getHealth().getMaxShield() > 10000
                        && heroapi.getHealth().shieldPercent() > 0.9)
                        || heroapi.getHealth().getShield() >= heroapi.getHealth().getMaxShield()) {
                    isConfigAttackFull = true;
                }
            } else if (!isCongigRunFull) {
                heroapi.setRunMode();
                if ((heroapi.getHealth().getMaxShield() > 10000
                        && heroapi.getHealth().shieldPercent() > 0.9)
                        || heroapi.getHealth().getShield() >= heroapi.getHealth().getMaxShield()) {
                    isCongigRunFull = true;
                }
            }
        }
    }

    private void roamingLogic() {
        if (pvpConfig.move) {
            if ((pvpConfig.rechargeShields && isConfigAttackFull && isCongigRunFull)
                    || (!pvpConfig.rechargeShields && pvpConfig.changeConfig)) {
                heroapi.setRoamMode();
            }
            if (pvpConfig.collectorActive) {
                collectorModule.onTickModule();
            } else if (!movement.isMoving() || movement.isOutOfMap()) {
                movement.moveRandom();
            }
        }
    }

    private boolean checkMap() {
        GameMap map = starSystem.findMap(workingMap.getValue()).orElse(null);
        if (map != null && !portals.isEmpty() && map != starSystem.getCurrentMap()) {
            this.bot.setModule(api.requireInstance(MapModule.class)).setTarget(map);
            return false;
        }
        return true;
    }

    private boolean hasTarget() {
        if (target != null && target.isValid() && !shipAttacker.inGroup(target.getId())
                && target.getLocationInfo().distanceTo(heroapi) < pvpConfig.rangeForAttackedEnemy) {
            return true;
        }

        if (!isUnderAttack()) {
            target = shipAttacker.getEnemy(pvpConfig.rangeForEnemies, antiPushLogic.getIgnoredPlayers());
            shipAttacker.setTarget(target);
        }

        return target != null && target.isValid();
    }

    private void setConfigToUse() {
        if (attackConfigLost || heroapi.getHealth().shieldPercent() < 0.1 && heroapi.getHealth().hpPercent() < 0.3) {
            attackConfigLost = true;
            heroapi.setRunMode();
        } else if (pvpConfig.useRunConfig && target != null) {
            double distance = heroapi.getLocationInfo().distanceTo(target);
            if (distance > 500 && distance > lastDistanceTarget && target.getSpeed() >= heroapi.getSpeed()) {
                heroapi.setRunMode();
                lastDistanceTarget = distance;
                return;
            } else {
                heroapi.setAttackMode(null);
            }
        } else {
            heroapi.setAttackMode(null);
        }
        lastDistanceTarget = 1500;
    }

    private boolean isUnderAttack() {
        Entity targetAttacker = SharedFunctions.getAttacker(heroapi, players, heroapi);
        if (targetAttacker != null && targetAttacker.isValid()) {
            target = (Ship) targetAttacker;
            shipAttacker.setTarget(target);
            return true;
        }
        shipAttacker.resetDefenseData();
        attackConfigLost = false;
        target = null;

        return false;
    }
}