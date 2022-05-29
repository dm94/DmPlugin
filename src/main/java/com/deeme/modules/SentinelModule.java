package com.deeme.modules;

import com.deeme.types.ShipAttacker;
import com.deeme.types.VerifierChecker;
import com.deeme.types.config.AutoAttack;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.config.NpcExtra;
import com.github.manolo8.darkbot.config.PlayerTag;
import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.config.types.Tag;
import com.github.manolo8.darkbot.config.types.TagDefault;
import com.github.manolo8.darkbot.core.entities.Npc;
import com.github.manolo8.darkbot.core.entities.Ship;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.core.itf.InstructionProvider;
import com.github.manolo8.darkbot.core.itf.Module;
import com.github.manolo8.darkbot.core.objects.group.GroupMember;
import com.github.manolo8.darkbot.core.utils.Drive;
import com.github.manolo8.darkbot.core.utils.Location;
import com.github.manolo8.darkbot.extensions.features.Feature;
import com.github.manolo8.darkbot.modules.MapModule;
import com.github.manolo8.darkbot.modules.utils.NpcAttacker;
import com.github.manolo8.darkbot.modules.utils.SafetyFinder;

import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem.Formation;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.shared.modules.CollectorModule;

import java.util.Arrays;

import static java.lang.Double.min;
import static java.lang.Math.random;

@Feature(name = "Sentinel Module", description = "Follow the main ship or the group leader and do the same")
public class SentinelModule implements Module, Configurable<SentinelModule.SentinelConfig>, InstructionProvider {
    private SentinelConfig sConfig;
    private Ship sentinel;
    private Main main;
    private NpcAttacker attacker;
    private Drive drive;
    private SafetyFinder safety;
    private State currentStatus;
    private ShipAttacker shipAttacker;
    private HeroItemsAPI items;
    protected CollectorModule collectorModule;
    protected boolean isNpc = false;
    protected boolean backwards = false;
    protected int masterID = 0;
    protected long maximumWaitingTime = 0;
    protected int lastMap = 0;

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
        this.attacker = new NpcAttacker(main);
        this.drive = main.hero.drive;
        this.safety = new SafetyFinder(main);
        currentStatus = State.INIT;
        this.shipAttacker = new ShipAttacker(main);
        collectorModule = new CollectorModule(main.pluginAPI);
        this.items = main.pluginAPI.getAPI(HeroItemsAPI.class);
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
                lastMap = main.hero.getMap().getId();
                if (isAttacking()) {
                    currentStatus = State.TRAVELING_TO_ENEMY;
                    if (isNpc) {
                        npcMove();
                    } else {
                        shipAttacker.vsMove();
                    }
                } else {
                    currentStatus = State.FOLLOWING_MASTER;
                    main.hero.roamMode();
                    if (main.hero.distanceTo(sentinel.getLocationInfo().getCurrent()) > sConfig.rangeToLider) {
                        drive.move(sentinel.getLocationInfo().now);
                    } else if (sConfig.collectorActive) {
                        collectorModule.findBox();
                        if (collectorModule.currentBox != null && sentinel.getLocationInfo().distanceTo(collectorModule.currentBox) < sConfig.rangeToLider) {
                            collectorModule.tryCollectNearestBox();
                        }
                    }
                }
            } else {
                if (main.guiManager.group.hasGroup()) {
                    goToLeader();
                } else {
                    if (lastMap != main.hero.getMap().getId()) {
                        maximumWaitingTime = System.currentTimeMillis() + 120000;
                    }
                    acceptGroupSentinel();
                    if (lastMap != main.hero.getMap().getId() && currentStatus != State.WAIT_GROUP_LOADING  && currentStatus != State.WAIT) {
                        currentStatus = State.WAIT_GROUP_LOADING;
                        maximumWaitingTime = System.currentTimeMillis() + 120000;
                    } else if (maximumWaitingTime <= System.currentTimeMillis()) {
                        currentStatus =  State.WAIT;
                        if (main.config.GENERAL.WORKING_MAP != main.hero.getMap().getId() && !main.mapManager.entities.portals.isEmpty()) {
                            currentStatus = State.TRAVELING_TO_WORKING_MAP;
                            this.main.setModule(new MapModule())
                                    .setTarget(this.main.starManager.byId(this.main.config.GENERAL.WORKING_MAP));
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

        if (target == null) {
            target = main.mapManager.entities.ships.stream()
                .filter(s -> (sentinel.getTarget() != null && sentinel.getTarget().getId() == s.getId()) && s.playerInfo.isEnemy())
                .findAny().orElse(null);
        }

        if (target == null && sConfig.autoAttack.autoAttackEnemies) {
            target = shipAttacker.getEnemy(sConfig.autoAttack.rangeForEnemies);
        }

        if (target == null) { return false; }

        if ((target instanceof Npc)) {
            isNpc = true;
            attacker.setTarget((Npc) target);
            setMode(main.config.GENERAL.OFFENSIVE, (Npc) target);
            attacker.doKillTargetTick();
        } else {
            isNpc = false;
            shipAttacker.setTarget((Ship) target);
            setMode(main.config.GENERAL.OFFENSIVE);
            shipAttacker.doKillTargetTick();
        }
        currentStatus = State.HELPING_MASTER;

        return attacker.target != null;
    }

    private boolean shipAround() {
        if (main.mapManager.entities.ships == null) { return false; } 

        sentinel = main.mapManager.entities.ships.stream()
                .filter(ship ->  (ship.getId() == masterID || ship.getId() == sConfig.MASTER_ID || sConfig.SENTINEL_TAG.has(main.config.PLAYER_INFOS.get(ship.getId()))) && ship.getId() != main.hero.getId())
                .findAny().orElse(null);

        return sentinel != null;
    }

    private void goToLeader() {
        for (GroupMember m : main.guiManager.group.group.members) {
            if ((m.getId() == sConfig.MASTER_ID || sConfig.SENTINEL_TAG.has(main.config.PLAYER_INFOS.get(m.getId())) || m.isLeader()) && m.getId() != main.hero.getId()) {
                masterID = m.getId();
                if (m.getMapId() == main.hero.getMap().getId()) {
                    drive.move(m.location);
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

    private void setMode(Config.ShipConfig config, Npc npc) {
        if (npc != null && npc.npcInfo.attackFormation != null) {
            main.hero.setMode(config.CONFIG, npc.npcInfo.attackFormation);
        } else {
            setMode(config);
        }
    }

    private void setMode(Config.ShipConfig config) {
        if (sConfig.copyMasterFormation && sentinel != null) {
            int sentinelFormation = sentinel.formationId;

            if (!main.hero.isInFormation(sentinelFormation)) {
                Formation formation = Formation.of(sentinelFormation);

                if (formation != null) {
                    Boolean canChange = items.getItem(formation, ItemFlag.USABLE, ItemFlag.READY).isPresent();

                    if (canChange) {
                        main.hero.setMode(config.CONFIG, main.facadeManager.slotBars.getKeyBind(formation));
                    } else {
                        main.hero.setMode(config);
                    }
                } else {
                    main.hero.setMode(config);
                }
            }
        } else {
            main.hero.setMode(config);
        }
    }

    public void npcMove() {
        Npc target = attacker.target;
        Location direction = drive.movingTo();
        Location heroLoc = main.hero.getLocationInfo().now;
        Location targetLoc = target.getLocationInfo().destinationInTime(400);

        double distance = heroLoc.distance(target.getLocationInfo().now);
        double angle = targetLoc.angle(heroLoc);

        double radius = target.getInfo().getRadius();
        boolean noCircle = target.getInfo().hasExtraFlag(NpcExtra.NO_CIRCLE);

        radius = attacker.modifyRadius(radius);
        if (radius > 750) noCircle = false;

        double angleDiff;
        if (noCircle) {
            double dist = targetLoc.distance(direction);
            double minRad = Math.max(0, Math.min(radius - 200, radius * 0.5));
            if (dist <= radius && dist >= minRad) {
                setNPCConfig(direction);
                return;
            }
            distance = minRad + random() * (radius - minRad - 10);
            angleDiff = (random() * 0.1) - 0.05;
        } else {
            double maxRadFix = target.getInfo().getRadius() / 2,
                    radiusFix = (int) Math.max(Math.min(radius - distance, maxRadFix), -maxRadFix);
            distance = (radius += radiusFix);
            angleDiff = Math.max((main.hero.getSpeed() * 0.625) + (min(200, target.getSpeed()) * 0.625)
                    - heroLoc.distance(Location.of(targetLoc, angle, radius)), 0) / radius;
        }
        direction = getBestDir(targetLoc, angle, angleDiff, distance);

        while (!drive.canMove(direction) && distance < 10000)
            direction.toAngle(targetLoc, angle += backwards ? -0.3 : 0.3, distance += 2);
        if (distance >= 10000) direction.toAngle(targetLoc, angle, 500);

        setNPCConfig(direction);

        drive.move(direction);
    }

    protected Location getBestDir(Location targetLoc, double angle, double angleDiff, double distance) {
        int iteration = 1;
        double forwardScore = 0, backScore = 0;
        do {
            forwardScore += score(Location.of(targetLoc, angle + (angleDiff * iteration), distance));
            backScore += score(Location.of(targetLoc, angle - (angleDiff * iteration), distance));
            if (forwardScore < 0 != backScore < 0 || Math.abs(forwardScore - backScore) > 300) break;
        } while (iteration++ < main.config.LOOT.MAX_CIRCLE_ITERATIONS);

        if (iteration <= main.config.LOOT.MAX_CIRCLE_ITERATIONS) backwards = backScore > forwardScore;
        return Location.of(targetLoc, angle + angleDiff * (backwards ? -1 : 1), distance);
    }

    protected double score(Location loc) {
        return (drive.canMove(loc) ? 0 : -1000) - main.mapManager.entities.ships.stream()
                .filter(n -> attacker.target != n)
                .mapToDouble(n -> Math.max(0, 560 - n.getLocationInfo().getCurrent().distanceTo(loc)))
                .sum();
    }

    protected void setNPCConfig(Location direction) {
        if (!attacker.hasTarget()) main.hero.roamMode();
        else if (main.config.LOOT.RUN_CONFIG_IN_CIRCLE
                && attacker.target.getHealth().hpPercent() < 0.25
                && main.hero.getLocationInfo().getCurrent().distanceTo(direction) > attacker.target.getInfo().getRadius() * 2) setMode(main.config.GENERAL.RUN);
        else if (main.hero.getLocationInfo().getCurrent().distanceTo(direction) > attacker.target.getInfo().getRadius() * 3) setMode(main.config.GENERAL.ROAM);
        else setMode(main.config.GENERAL.OFFENSIVE);
    }

    public static class SentinelConfig  {

        @Option(value = "Master ID", description = "Can be empty, will use tag or group leader")
        @Num(max = 2000000000, step = 1)
        public int MASTER_ID = 0;

        @Option (value = "Sentinel Tag", description = "He'll follow every ship with that tag")
        @Tag(TagDefault.ALL)
        public PlayerTag SENTINEL_TAG = null;

        @Option(value = "Ignore security", description = "Ignore the config, when enabled, the ship will follow you to death.")
        public boolean ignoreSecurity = false;

        @Option(value = "Range towards the leader", description = "Distance it will be from the leader")
        @Num(min = 10, max = 1000, step = 100)
        public int rangeToLider = 300;

        @Option(value = "Enable collector", description = "It will use the collector module when it is not doing anything.")
        public boolean collectorActive = false;

        @Option(value = "Copy master formation", description = "It will try to use the master's formation")
        public boolean copyMasterFormation = false;

        public @Option(value = "Auto Attack", description = "Will attack even when the master is not attacking")
        AutoAttack autoAttack = new AutoAttack();

    }
}
