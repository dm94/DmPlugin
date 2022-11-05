package com.deeme.modules;

import com.deeme.types.SharedFunctions;
import com.deeme.types.ShipAttacker;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deeme.types.config.SentinelConfig;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.Config;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.NpcFlag;
import eu.darkbot.api.config.types.ShipMode;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.extensions.InstructionProvider;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.items.SelectableItem.Cpu;
import eu.darkbot.api.game.items.SelectableItem.Formation;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.game.other.Locatable;
import eu.darkbot.api.game.other.Location;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.AttackAPI;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.GroupAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.managers.PetAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.CollectorModule;
import eu.darkbot.shared.modules.MapModule;
import eu.darkbot.shared.utils.SafetyFinder;

import java.util.Arrays;
import java.util.Collection;

@Feature(name = "Sentinel", description = "Follow the main ship or the group leader and do the same")
public class SentinelModule implements Module, Configurable<SentinelConfig>, InstructionProvider {
    protected PluginAPI api;
    protected HeroAPI heroapi;
    protected BotAPI bot;
    protected MovementAPI movement;
    protected AttackAPI attacker;
    protected StarSystemAPI starSystem;
    protected PetAPI pet;
    protected GroupAPI group;
    protected ConfigSetting<Integer> workingMap;
    protected ConfigSetting<Integer> maxCircleIterations;
    protected ConfigSetting<ShipMode> configOffensive;
    protected ConfigSetting<ShipMode> configRun;
    protected ConfigSetting<ShipMode> configRoam;
    protected ConfigSetting<Boolean> rsbEnabled;
    protected ConfigSetting<Config.Loot.Sab> sabSettings;
    protected ConfigSetting<Boolean> runConfigInCircle;

    protected Collection<? extends Portal> portals;
    protected Collection<? extends Ship> ships;
    protected Collection<? extends Player> players;
    protected Collection<? extends Npc> npcs;

    private SentinelConfig sConfig;
    private Player sentinel;
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

    protected Location lastSentinelLocation = null;

    private enum State {
        INIT("Init"),
        WAIT("Waiting for group invitation"),
        WAIT_GROUP_LOADING("Waiting while loading the group"),
        TRAVELLING_TO_MASTER("Travelling to the master's map"),
        FOLLOWING_MASTER("Following the master"),
        HELPING_MASTER("Helping the master"),
        TRAVELING_TO_WORKING_MAP("Travelling to the working map to wait");

        private final String message;

        State(String message) {
            this.message = message;
        }
    }

    @Override
    public void setConfig(ConfigSetting<SentinelConfig> arg0) {
        this.sConfig = arg0.getValue();
    }

    public SentinelModule(Main main, PluginAPI api) {
        this(main, api, api.requireAPI(AuthAPI.class), api.requireInstance(SafetyFinder.class));
    }

    @Inject
    public SentinelModule(Main main, PluginAPI api, AuthAPI auth, SafetyFinder safety) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        if (!Utils.discordCheck(auth.getAuthId())) {
            Utils.showDiscordDialog();
            ExtensionsAPI extensionsAPI = api.getAPI(ExtensionsAPI.class);
            extensionsAPI.getFeatureInfo(this.getClass())
                    .addFailure("To use this option you need to be on my discord", "Log in to my discord and reload");
        }

        this.main = main;
        this.currentStatus = State.INIT;

        this.api = api;
        this.bot = api.getAPI(BotAPI.class);
        this.heroapi = api.getAPI(HeroAPI.class);
        this.movement = api.getAPI(MovementAPI.class);
        this.attacker = api.getAPI(AttackAPI.class);
        this.starSystem = api.getAPI(StarSystemAPI.class);
        this.pet = api.getAPI(PetAPI.class);
        this.group = api.getAPI(GroupAPI.class);
        this.safety = safety;

        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        this.portals = entities.getPortals();
        this.ships = entities.getShips();
        this.players = entities.getPlayers();
        this.npcs = entities.getNpcs();

        ConfigAPI configApi = api.getAPI(ConfigAPI.class);
        this.collectorModule = new CollectorModule(api);

        this.workingMap = configApi.requireConfig("general.working_map");
        this.maxCircleIterations = configApi.requireConfig("loot.max_circle_iterations");
        this.runConfigInCircle = configApi.requireConfig("loot.run_config_in_circle");
        this.configOffensive = configApi.requireConfig("general.offensive");
        this.configRun = configApi.requireConfig("general.run");
        this.configRoam = configApi.requireConfig("general.roam");
        this.rsbEnabled = configApi.requireConfig("loot.rsb.enabled");
        this.sabSettings = configApi.requireConfig("loot.sab");

        this.shipAttacker = new ShipAttacker(api, sabSettings.getValue(), rsbEnabled.getValue());
    }

    @Override
    public boolean canRefresh() {
        if (!sConfig.collectorActive || collectorModule.canRefresh()) {
            return safety.tick();
        }
        return false;
    }

    @Override
    public String getStatus() {
        return safety.state() != SafetyFinder.Escaping.NONE ? safety.status()
                : currentStatus.message + " | " + getAttackStatus();
    }

    private String getAttackStatus() {
        return isNpc ? getNpcStatus() : getPlayerStatus();
    }

    private String getNpcStatus() {
        return "NPC | " + attacker.getStatus();
    }

    private String getPlayerStatus() {
        return "Player | " + shipAttacker.getStatus();
    }

    @Override
    public String instructions() {
        return "Sentinel Module: \n" +
                "It's important that the main ship is in a group \n" +
                "Following priority: Master ID > Tag > Group Leader \n " +
                "If a \"Sentinel Tag\" is not defined, it will follow the group leader \n";
    }

    @Override
    public void onTickModule() {
        if ((sConfig.ignoreSecurity || safety.tick()) && (!sConfig.collectorActive || collectorModule.canRefresh())) {
            pet.setEnabled(true);
            if (shipAround()) {
                lastMap = heroapi.getMap() != null ? heroapi.getMap().getId() : null;
                if (isAttacking()) {
                    currentStatus = State.HELPING_MASTER;
                    lastTimeAttack = System.currentTimeMillis();
                    if (isNpc) {
                        npcMove();
                    } else {
                        shipAttacker.tryAttackOrFix();
                        shipAttacker.vsMove();
                    }

                    if (sConfig.aggressiveFollow
                            && heroapi.distanceTo(sentinel.getLocationInfo().getCurrent()) > sConfig.rangeToLider) {
                        moveToMaster();
                    }
                } else if (sentinel.isValid()) {
                    currentStatus = State.FOLLOWING_MASTER;
                    setMode(configRoam.getValue());
                    if (heroapi.distanceTo(sentinel.getLocationInfo().getCurrent()) > sConfig.rangeToLider) {
                        moveToMaster();
                    } else if (sConfig.collectorActive) {
                        collectorModule.findBox();
                        if (collectorModule.currentBox != null && sentinel.getLocationInfo()
                                .distanceTo(collectorModule.currentBox) < sConfig.rangeToLider) {
                            collectorModule.tryCollectNearestBox();
                        }
                    }

                    autoCloack();
                } else {
                    sentinel = null;
                }
            } else if (sConfig.followByPortals && lastSentinelLocation != null) {
                followByPortals();
            } else {
                groupLeaderID = 0;
                if (group.hasGroup()) {
                    goToGroup();
                } else {
                    if (lastMap != heroapi.getMap().getId()) {
                        maximumWaitingTime = System.currentTimeMillis() + 60000;
                    }
                    acceptGroupSentinel();
                    if (lastMap != heroapi.getMap().getId() && currentStatus != State.WAIT_GROUP_LOADING
                            && currentStatus != State.WAIT) {
                        currentStatus = State.WAIT_GROUP_LOADING;
                        maximumWaitingTime = System.currentTimeMillis() + 60000;
                    } else if (maximumWaitingTime <= System.currentTimeMillis()) {
                        currentStatus = State.WAIT;
                        GameMap map = getWorkingMap();
                        if (!portals.isEmpty() && map != starSystem.getCurrentMap()) {
                            currentStatus = State.TRAVELING_TO_WORKING_MAP;
                            this.bot.setModule(new MapModule(api, true))
                                    .setTarget(map);
                        } else if (sConfig.collectorActive) {
                            collectorModule.onTickModule();
                        }
                    }
                }
            }
        }
    }

    private void followByPortals() {
        Portal portal = getNearestPortal(lastSentinelLocation);
        if (portal != null) {
            portal.getTargetMap().ifPresentOrElse(
                    m -> this.bot.setModule(api.requireInstance(MapModule.class)).setTarget(m),
                    () -> lastSentinelLocation = null);
        } else {
            lastSentinelLocation = null;
        }
    }

    private void autoCloack() {
        if (sConfig.autoCloak.autoCloakShip && !heroapi.isInvisible()
                && lastTimeAttack < (System.currentTimeMillis()
                        - (sConfig.autoCloak.secondsOfWaiting * 1000))) {
            shipAttacker.useSelectableReadyWhenReady(Cpu.CL04K);
        }
    }

    private void moveToMaster() {
        if (sentinel != null) {
            if (sConfig.goToMasterDestination) {
                sentinel.getDestination().ifPresentOrElse(d -> movement.moveTo(d),
                        () -> movement.moveTo(sentinel.getLocationInfo().getCurrent()));
            } else {
                movement.moveTo(sentinel.getLocationInfo().getCurrent());
            }
        }
    }

    protected GameMap getWorkingMap() {
        return starSystem.getOrCreateMapById(workingMap.getValue());
    }

    private boolean isAttacking() {
        Entity target = getSentinelTarget();

        if (target == null) {
            if (shipAttacker.getTarget() != null) {
                target = shipAttacker.getTarget();
            } else if (attacker.getTarget() != null) {
                target = attacker.getTarget();
            }
        }

        if (target != null && (!target.isValid() || target.getId() == heroapi.getId())) {
            target = null;
        }

        if (target == null && sConfig.autoAttack.autoAttackEnemies) {
            target = shipAttacker.getEnemy(sConfig.autoAttack.rangeForEnemies);
        }
        if (target == null && sConfig.autoAttack.defendFromNPCs) {
            target = SharedFunctions.getAttacker(heroapi, npcs, heroapi);
        }
        if (target != null) {
            if (target instanceof Npc) {
                isNpc = true;
                attacker.setTarget((Npc) target);
                setMode(configOffensive.getValue(), (Npc) target);
                attacker.tryLockAndAttack();
            } else {
                isNpc = false;
                shipAttacker.setTarget((Ship) target);
                setMode(configOffensive.getValue());
                shipAttacker.tryLockAndAttack();
            }
        }

        return target != null;
    }

    private Entity getSentinelTarget() {
        Entity target = null;
        if (sentinel.getTarget() != null) {
            if (sConfig.autoAttack.helpAttackPlayers || sConfig.autoAttack.helpAttackEnemyPlayers) {
                target = players.stream()
                        .filter(s -> (sentinel.getTarget().getId() == s.getId())
                                && (sConfig.autoAttack.helpAttackPlayers || s.getEntityInfo().isEnemy()))
                        .findAny().orElse(null);
            }
            if (target == null && sConfig.autoAttack.helpAttackNPCs) {
                target = npcs.stream()
                        .filter(s -> sentinel.getTarget().getId() == s.getId())
                        .findAny().orElse(null);
                if (target != null) {
                    isNpc = true;
                }
            }
        }
        return target;
    }

    private boolean shipAround() {
        if (players.isEmpty()) {
            return false;
        }

        sentinel = players.stream()
                .filter(ship -> ship.isValid() && ship.getId() != heroapi.getId())
                .filter(ship -> (ship.getId() == masterID ||
                        (sConfig.MASTER_ID != 0 && ship.getId() == sConfig.MASTER_ID) ||
                        (sConfig.SENTINEL_TAG != null
                                && sConfig.SENTINEL_TAG.has(main.config.PLAYER_INFOS.get(ship.getId())))
                        ||
                        (sConfig.followGroupLeader && groupLeaderID != 0 && ship.getId() == groupLeaderID)))
                .findAny().orElse(null);

        if (sentinel != null) {
            lastSentinelLocation = sentinel.getLocationInfo().getCurrent();
        }

        return sentinel != null;
    }

    private void goToGroup() {
        for (eu.darkbot.api.game.group.GroupMember m : group.getMembers()) {
            if (!m.isDead() && m.getId() != heroapi.getId()
                    && ((sConfig.MASTER_ID != 0 && m.getId() == sConfig.MASTER_ID) ||
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
                    this.bot.setModule(api.requireInstance(MapModule.class))
                            .setTarget(starSystem.getOrCreateMapById(m.getMapId()));
                    currentStatus = State.TRAVELLING_TO_MASTER;
                }
                return;
            }
        }
    }

    private void acceptGroupSentinel() {
        if (group.getInvites().isEmpty()) {
            return;
        }

        main.guiManager.group.invites.stream()
                .filter(in -> in.isIncoming() && (sConfig.SENTINEL_TAG == null ||
                        sConfig.SENTINEL_TAG.has(main.config.PLAYER_INFOS.get(in.getInviter().getId()))))
                .findFirst()
                .ifPresent(main.guiManager.group::acceptInvite);
    }

    private void setMode(ShipMode config, Npc npc) {
        if (npc != null) {
            heroapi.setAttackMode(npc);
        } else {
            setMode(config);
        }
    }

    private void setMode(ShipMode config) {
        if (sConfig.copyMasterFormation && sentinel != null) {
            Formation sentinelFormation = sentinel.getFormation();

            if (!heroapi.isInFormation(sentinelFormation)) {
                if (sentinelFormation != null) {
                    shipAttacker.setMode(config, sentinelFormation);
                } else {
                    heroapi.setMode(config);
                }
            }
        } else {
            heroapi.setMode(config);
        }
    }

    protected double getRadius(Lockable target) {
        if (!(target instanceof Npc)) {
            return 550;
        }
        return attacker.modifyRadius(((Npc) target).getInfo().getRadius());
    }

    protected void npcMove() {
        if (!attacker.hasTarget()) {
            return;
        }
        Lockable target = attacker.getTarget();

        Location direction = movement.getDestination();
        Location targetLoc = target.getLocationInfo().destinationInTime(400);

        double distance = heroapi.distanceTo(attacker.getTarget());
        double angle = targetLoc.angleTo(heroapi);
        double radius = getRadius(target);
        double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
        boolean noCircle = attacker.hasExtraFlag(NpcFlag.NO_CIRCLE);

        if (radius > 750) {
            noCircle = false;
        }

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
            double maxRadFix = radius / 2;
            double radiusFix = (int) Math.max(Math.min(radius - distance, maxRadFix), -maxRadFix);
            distance = (radius += radiusFix);
            angleDiff = Math.max((heroapi.getSpeed() * 0.625) + (Math.max(200, speed) * 0.625)
                    - heroapi.distanceTo(Location.of(targetLoc, angle, radius)), 0) / radius;
        }
        direction = getBestDir(targetLoc, angle, angleDiff, distance);

        while (!movement.canMove(direction) && distance < 10000)
            direction.toAngle(targetLoc, angle += backwards ? -0.3 : 0.3, distance += 2);
        if (distance >= 10000)
            direction.toAngle(targetLoc, angle, 500);

        setNPCConfig(direction);

        movement.moveTo(direction);
    }

    protected Location getBestDir(Locatable targetLoc, double angle, double angleDiff, double distance) {
        int maxCircleIterationsValue = this.maxCircleIterations.getValue();
        int iteration = 1;
        double forwardScore = 0;
        double backScore = 0;
        do {
            forwardScore += score(Locatable.of(targetLoc, angle + (angleDiff * iteration), distance));
            backScore += score(Locatable.of(targetLoc, angle - (angleDiff * iteration), distance));
            if (forwardScore < 0 != backScore < 0 || Math.abs(forwardScore - backScore) > 300)
                break;
        } while (iteration++ < maxCircleIterationsValue);

        if (iteration <= maxCircleIterationsValue)
            backwards = backScore > forwardScore;
        return Location.of(targetLoc, angle + angleDiff * (backwards ? -1 : 1), distance);
    }

    protected double score(Locatable loc) {
        return (movement.canMove(loc) ? 0 : -1000) - npcs.stream()
                .filter(n -> attacker.getTarget() != n)
                .mapToDouble(n -> Math.max(0, n.getInfo().getRadius() - n.distanceTo(loc)))
                .sum();
    }

    protected Portal getNearestPortal(Location loc) {
        if (loc == null) {
            return null;
        }
        return portals.stream().filter(p -> p.distanceTo(loc) < 500).findFirst().orElse(null);
    }

    protected void setNPCConfig(Location direction) {
        Npc target = attacker.getTargetAs(Npc.class);
        if (target == null || !target.isValid()) {
            setMode(configRoam.getValue());
        } else if (runConfigInCircle.getValue()
                && target.getHealth().hpPercent() < 0.25
                && heroapi.getLocationInfo().getCurrent().distanceTo(direction) > target.getInfo().getRadius() * 2) {
            setMode(configRun.getValue());
        } else if (heroapi.getLocationInfo().getCurrent().distanceTo(direction) > target.getInfo().getRadius() * 3) {
            setMode(configRoam.getValue());
        } else {
            setMode(configOffensive.getValue());
        }
    }
}
