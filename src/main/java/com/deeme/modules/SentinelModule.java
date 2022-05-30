package com.deeme.modules;

import com.deeme.types.ShipAttacker;
import com.deeme.types.VerifierChecker;
import com.deeme.types.config.SentinelConfig;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.entities.Ship;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.core.itf.InstructionProvider;
import com.github.manolo8.darkbot.core.itf.Module;
import com.github.manolo8.darkbot.core.objects.group.GroupMember;
import com.github.manolo8.darkbot.extensions.features.Feature;
import com.github.manolo8.darkbot.modules.MapModule;
import com.github.manolo8.darkbot.modules.utils.SafetyFinder;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.NpcFlag;
import eu.darkbot.api.config.types.ShipMode;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.items.SelectableItem.Cpu;
import eu.darkbot.api.game.items.SelectableItem.Formation;
import eu.darkbot.api.game.other.Locatable;
import eu.darkbot.api.game.other.Location;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.AttackAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.shared.modules.CollectorModule;

import java.util.Arrays;

@Feature(name = "Sentinel", description = "Follow the main ship or the group leader and do the same")
public class SentinelModule implements Module, Configurable<SentinelConfig>, InstructionProvider {
    protected PluginAPI api;
    protected HeroAPI heroapi;
    protected MovementAPI movement;
    protected AttackAPI attacker;
    protected ConfigSetting<Integer> workingMap;
    protected ConfigSetting<Integer> maxCircleIterations;
    protected ConfigSetting<ShipMode> configOffensive;
    protected ConfigSetting<ShipMode> configRun;
    protected ConfigSetting<ShipMode> configRoam;

    private SentinelConfig sConfig;
    private Ship sentinel;
    private Main main;
    private SafetyFinder safety;
    private State currentStatus;
    private ShipAttacker shipAttacker;
    protected CollectorModule collectorModule;
    protected boolean isNpc = false;
    protected boolean backwards = false;
    protected int masterID = 0;
    protected long maximumWaitingTime = 0;
    protected int lastMap = 0;
    protected long lastTimeAttack = 0;
    protected int groupLeaderID = 0;

    private enum State {
        INIT ("Init"),
        WAIT ("Waiting for group invitation"),
        WAIT_GROUP_LOADING ("Waiting while loading the group"),
        TRAVELLING_TO_MASTER ("Travelling to the master's map"),
        FOLLOWING_MASTER("Following the master"),
        HELPING_MASTER("Helping the master"),
        TRAVELING_TO_ENEMY("Travelling to the enemy"),
        TRAVELING_TO_WORKING_MAP("Travelling to the working map to wait");

        private final String message;

        State(String message) {
            this.message = message;
        }
    }

    @Override
    public void install(Main main) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) return;
        VerifierChecker.checkAuthenticity();
        this.main = main;
        this.safety = new SafetyFinder(main);
        currentStatus = State.INIT;
        this.shipAttacker = new ShipAttacker(main);

        this.api = main.pluginAPI.getAPI(PluginAPI.class);
        this.heroapi = api.getAPI(HeroAPI.class);
        this.movement = api.getAPI(MovementAPI.class);
        this.attacker = api.getAPI(AttackAPI.class);
        ConfigAPI configApi = api.getAPI(ConfigAPI.class);
        this.collectorModule = new CollectorModule(api);

        this.workingMap = configApi.requireConfig("general.working_map");
        this.maxCircleIterations = configApi.requireConfig("loot.max_circle_iterations");
        this.configOffensive = configApi.requireConfig("general.offensive");
        this.configRun = configApi.requireConfig("general.run");
        this.configRoam = configApi.requireConfig("general.roam");
    }

    @Override
    public void uninstall() {
        safety.uninstall();
    }

    @Override
    public boolean canRefresh() {
        if (!sConfig.collectorActive || collectorModule.canRefresh()) {
            return safety.tick();
        }
        return false;
    }

    @Override
    public String status() {
        return safety.state() != SafetyFinder.Escaping.NONE ? safety.status() : currentStatus.message + " | " + (isNpc ? attacker.getStatus() : "") ;
    }

    @Override
    public void setConfig(SentinelConfig sentinelConfig) {
        this.sConfig = sentinelConfig;
    }

    @Override
    public String instructions() {
        return "Sentinel Module: \n" +
                "It's important that the main ship is in a group \n" +
                "Following priority: Master ID > Tag > Group Leader \n "+
                "If a \"Sentinel Tag\" is not defined, it will follow the group leader \n";
    }

    @Override
    public void tick() {
        if ((sConfig.ignoreSecurity || safety.tick()) && (!sConfig.collectorActive || collectorModule.canRefresh())) {
            main.guiManager.pet.setEnabled(true);
            if (shipAround()) {
                lastMap = heroapi.getMap() != null ? heroapi.getMap().getId() : null;
                if (isAttacking()) {
                    lastTimeAttack = System.currentTimeMillis();
                    currentStatus = State.TRAVELING_TO_ENEMY;
                    if (isNpc) {
                        npcMove();
                    } else {
                        shipAttacker.vsMove();
                    }
                } else {
                    currentStatus = State.FOLLOWING_MASTER;
                    setMode(configRoam.getValue());
                    if (heroapi.distanceTo(sentinel.getLocationInfo().getCurrent()) > sConfig.rangeToLider) {
                        movement.moveTo(sentinel.getLocationInfo().getCurrent());
                    } else if (sConfig.collectorActive) {
                        collectorModule.findBox();
                        if (collectorModule.currentBox != null && sentinel.getLocationInfo().distanceTo(collectorModule.currentBox) < sConfig.rangeToLider) {
                            collectorModule.tryCollectNearestBox();
                        }
                    }

                    if (sConfig.autoCloak.autoCloakShip && !heroapi.isInvisible() && lastTimeAttack < (System.currentTimeMillis() + (sConfig.autoCloak.secondsOfWaiting * 1000))) {
                        shipAttacker.useSelectableReadyWhenReady(Cpu.CL04K);
                    }
                }
            } else {
                groupLeaderID = 0;
                if (main.guiManager.group.hasGroup()) {
                    goToLeader();
                } else {
                    if (lastMap != heroapi.getMap().getId()) {
                        maximumWaitingTime = System.currentTimeMillis() + 120000;
                    }
                    acceptGroupSentinel();
                    if (lastMap != heroapi.getMap().getId() && currentStatus != State.WAIT_GROUP_LOADING  && currentStatus != State.WAIT) {
                        currentStatus = State.WAIT_GROUP_LOADING;
                        maximumWaitingTime = System.currentTimeMillis() + 120000;
                    } else if (maximumWaitingTime <= System.currentTimeMillis()) {
                        currentStatus =  State.WAIT;
                        if (workingMap.getValue() != heroapi.getMap().getId() && !main.mapManager.entities.portals.isEmpty()) {
                            currentStatus = State.TRAVELING_TO_WORKING_MAP;
                            this.main.setModule(new MapModule())
                                    .setTarget(this.main.starManager.byId(workingMap.getValue()));
                        } else if (sConfig.collectorActive) {
                            collectorModule.onTickModule();
                        }
                    }
                }
            }
        }
    }

    private boolean isAttacking() {
        Entity target = sentinel.isAttacking() || sentinel.getTarget() != null ? sentinel.getTarget() : null;
        Entity targetEnemy = null;

        if (target == null) {
            target = main.mapManager.entities.ships.stream()
                .filter(s -> (sentinel.getTarget() != null && sentinel.getTarget().getId() == s.getId()) && s.playerInfo.isEnemy())
                .findAny().orElse(null);
        }

        if (target == null && sConfig.autoAttack.autoAttackEnemies) {
            targetEnemy = shipAttacker.getEnemy(sConfig.autoAttack.rangeForEnemies);
            isNpc = false;
            shipAttacker.setTarget((Ship) targetEnemy);
            setMode(configOffensive.getValue());
            shipAttacker.doKillTargetTick();
        }

        if (target == null) { return false; }

        if (target instanceof Npc && sConfig.autoAttack.helpAttackNPCs) {
            isNpc = true;
            attacker.setTarget((Npc) target);
            setMode(configOffensive.getValue(), (Npc) target);
            attacker.tryLockAndAttack();
        } else if (sConfig.autoAttack.helpAttackPlayers) {
            isNpc = false;
            shipAttacker.setTarget((Ship) target);
            setMode(configOffensive.getValue());
            shipAttacker.doKillTargetTick();
        }
        currentStatus = State.HELPING_MASTER;

        return attacker.getTarget() != null;
    }

    private boolean shipAround() {
        if (main.mapManager.entities.ships == null) { return false; } 

        sentinel = main.mapManager.entities.ships.stream()
                .filter(ship ->  (ship.getId() != heroapi.getId() && (
                    ship.getId() == masterID ||
                    (sConfig.MASTER_ID != 0 && ship.getId() == sConfig.MASTER_ID) || 
                    sConfig.SENTINEL_TAG.has(main.config.PLAYER_INFOS.get(ship.getId())) ||
                    (groupLeaderID != 0 && ship.getId() == groupLeaderID)
                    )))
                .findAny().orElse(null);

        return sentinel != null;
    }

    private void goToLeader() {
        for (GroupMember m : main.guiManager.group.group.members) {
            if (m.getId() != heroapi.getId() && ((sConfig.MASTER_ID != 0 && m.getId() == sConfig.MASTER_ID) || 
                sConfig.SENTINEL_TAG.has(main.config.PLAYER_INFOS.get(m.getId())) || 
                (sConfig.followGroupLeader && m.isLeader()))) {
                    if (m.isLeader()) {
                        groupLeaderID = m.getId();
                    }
                    masterID = m.getId();
                    if (m.getMapId() == heroapi.getMap().getId()) {
                        movement.moveTo(m.getLocation());
                        currentStatus = State.FOLLOWING_MASTER;
                    } else {
                        main.setModule(new MapModule()).setTarget(main.starManager.byId(m.getMapId()));
                        currentStatus = State.TRAVELLING_TO_MASTER;
                    }
                    return;
            }
        }
    }

    private void acceptGroupSentinel() {
        if (!main.guiManager.group.invites.isEmpty() && !main.guiManager.group.visible) {
            main.guiManager.group.show(true);
        }

        main.guiManager.group.invites.stream()
                .filter(in -> in.isIncoming() && (sConfig.SENTINEL_TAG == null ||
                        sConfig.SENTINEL_TAG.has(main.config.PLAYER_INFOS.get(in.getInviter().getId()))))
                .findFirst()
                .ifPresent(inv -> main.guiManager.group.acceptInvite(inv));
    }

    private void setMode(ShipMode config, Npc npc) {
        if (npc != null && npc.getInfo().getFormation() != null) {
            try {
                Formation formation = npc.getInfo().getFormation().get();
                if (formation != null) {
                    shipAttacker.setMode(config, formation);
                } else {
                    setMode(config);
                }
            } catch(Exception e) {
                setMode(config);
            }
        } else {
            setMode(config);
        }
    }

    private void setMode(ShipMode config) {
        if (sConfig.copyMasterFormation && sentinel != null) {
            int sentinelFormation = sentinel.formationId;

            if (!heroapi.isInFormation(sentinelFormation)) {
                Formation formation = Formation.of(sentinelFormation);

                if (formation != null) {
                    shipAttacker.setMode(config, formation);
                } else {
                    shipAttacker.setMode(config, sConfig.useBestFormation);
                }
            }
        } else {
            shipAttacker.setMode(config, sConfig.useBestFormation);
        }
    }

    protected double getRadius(Lockable target) {
        if (!(target instanceof Npc)) {
            return 550;
        }
        return attacker.modifyRadius(((Npc) target).getInfo().getRadius());
    }

    protected void npcMove() {
        if (!attacker.hasTarget()) return;
        Lockable target = attacker.getTarget();

        Location direction = movement.getDestination();
        Location targetLoc = target.getLocationInfo().destinationInTime(400);

        double distance = heroapi.distanceTo(attacker.getTarget());
        double angle = targetLoc.angleTo(heroapi);
        double radius = getRadius(target);
        double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
        boolean noCircle = attacker.hasExtraFlag(NpcFlag.NO_CIRCLE);

        if (radius > 750) noCircle = false;

        double angleDiff;
        if (noCircle) {
            double dist = targetLoc.distanceTo(direction);
            double minRad = Math.max(0, Math.min(radius - 200, radius * 0.5));
            if (dist <= radius && dist >= minRad) {
                setNPCConfig(direction);
                return;
            }
            distance = minRad + Math.random() * (radius - minRad - 10);
            angleDiff = (Math.random() * 0.1) - 0.05;
        } else {
            double maxRadFix = radius / 2,
                    radiusFix = (int) Math.max(Math.min(radius - distance, maxRadFix), -maxRadFix);
            distance = (radius += radiusFix);
            // Moved distance + speed - distance to chosen radius same angle, divided by radius
            angleDiff = Math.max((heroapi.getSpeed() * 0.625) + (Math.max(200, speed) * 0.625)
                    - heroapi.distanceTo(Location.of(targetLoc, angle, radius)), 0) / radius;
        }
        direction = getBestDir(targetLoc, angle, angleDiff, distance);

        while (!movement.canMove(direction) && distance < 10000)
            direction.toAngle(targetLoc, angle += backwards ? -0.3 : 0.3, distance += 2);
        if (distance >= 10000) direction.toAngle(targetLoc, angle, 500);

        setNPCConfig(direction);

        movement.moveTo(direction);
    }

    protected Location getBestDir(Locatable targetLoc, double angle, double angleDiff, double distance) {
        int maxCircleIterations = this.maxCircleIterations.getValue(), iteration = 1;
        double forwardScore = 0, backScore = 0;
        do {
            forwardScore += score(Locatable.of(targetLoc, angle + (angleDiff * iteration), distance));
            backScore += score(Locatable.of(targetLoc, angle - (angleDiff * iteration), distance));
            if (forwardScore < 0 != backScore < 0 || Math.abs(forwardScore - backScore) > 300) break;
        } while (iteration++ < maxCircleIterations);

        if (iteration <= maxCircleIterations) backwards = backScore > forwardScore;
        return Location.of(targetLoc, angle + angleDiff * (backwards ? -1 : 1), distance);
    }

    protected double score(Locatable loc) {
        return (movement.canMove(loc) ? 0 : -1000) - main.mapManager.entities.npcs.stream() // Consider barrier as bad as 1000 radius units.
                .filter(n -> attacker.getTarget() != n)
                .mapToDouble(n -> Math.max(0, n.getInfo().getRadius() - n.distanceTo(loc)))
                .sum();
    }

    protected void setNPCConfig(Location direction) {
        Npc target = attacker.getTargetAs(Npc.class);
        if (target == null || !target.isValid()) setMode(configRoam.getValue());
        else if (main.config.LOOT.RUN_CONFIG_IN_CIRCLE
                && target.getHealth().hpPercent() < 0.25
                && heroapi.getLocationInfo().getCurrent().distanceTo(direction) > target.getInfo().getRadius() * 2) setMode(configRun.getValue());
        else if (heroapi.getLocationInfo().getCurrent().distanceTo(direction) > target.getInfo().getRadius() * 3) setMode(configRoam.getValue());
        else setMode(configOffensive.getValue());
    }
}
