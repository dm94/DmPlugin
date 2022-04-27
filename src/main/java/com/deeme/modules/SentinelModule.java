package com.deeme.modules;

import com.deeme.types.VerifierChecker;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.PlayerTag;
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

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Feature(name = "Sentinel Module", description = "Follow the main ship or the group leader and do the same")
public class SentinelModule implements Module, Configurable<SentinelModule.SentinelConfig>, InstructionProvider {
    private SentinelConfig sConfig;
    private Ship sentinel;
    private Main main;
    private List<Ship> ships;
    private NpcAttacker attacker;
    private List<Npc> npcs;
    private Drive drive;
    private Random rnd;
    private SafetyFinder safety;
    private State currentStatus;

    private enum State {
        WAIT ("Waiting for group invitation"),
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
        this.ships = main.mapManager.entities.ships;
        this.npcs = main.mapManager.entities.npcs;
        this.attacker = new NpcAttacker(main);
        this.drive = main.hero.drive;
        this.rnd = new Random();
        this.safety = new SafetyFinder(main);
        currentStatus = State.WAIT;
    }

    @Override
    public void uninstall() {
        safety.uninstall();
    }

    @Override
    public boolean canRefresh() {
        return safety.tick();
    }

    @Override
    public String status() {
        return safety.state() != SafetyFinder.Escaping.NONE ? safety.status() : currentStatus.message;
    }

    @Override
    public void setConfig(SentinelConfig sentinelConfig) {
        this.sConfig = sentinelConfig;
    }

    @Override
    public String instructions() {
        return "Sentinel Module: \n" +
                "It's important that the main ship is in a group \n" +
                "If a \"Sentinel Tag\" is not defined, it will follow the group leader \n" +
                "It is recommended to activate and configure the Defence Mode";
    }

    public static class SentinelConfig  {
        @Option (value = "Sentinel Tag", description = "He'll follow every ship with that tag")
        @Tag(TagDefault.ALL)
        public PlayerTag SENTINEL_TAG = null;

        @Option(value = "Ignore security", description = "Ignore the config, when enabled, the ship will follow you to death.")
        public boolean ignoreSecurity = false;
    }

    @Override
    public void tick() {
        if (sConfig.ignoreSecurity || safety.tick()) {
            main.guiManager.pet.setEnabled(true);
            if (main.guiManager.group.group != null && main.guiManager.group.group.isValid()) {
                if (shipAround()) {
                    if (!isAttacking() && main.hero.getTarget() != sentinel) {
                        currentStatus = State.FOLLOWING_MASTER;
                        main.hero.roamMode();
                        if (drive.getDistanceBetween(main.hero.locationInfo, sentinel.locationInfo) > 300) {
                            drive.move(sentinel);
                        }
                    } else {
                        currentStatus = State.TRAVELING_TO_ENEMY;
                        drive.move(Location.of(attacker.target.locationInfo.now, rnd.nextInt(360), attacker.target.npcInfo.radius));
                    }
                } else {
                    goToLeader();
                }
            } else {
                currentStatus = State.WAIT;
                acceptGroupSentinel();
                if (main.config.GENERAL.WORKING_MAP != main.hero.map.id && !main.mapManager.entities.portals.isEmpty()) {
                    currentStatus = State.TRAVELING_TO_WORKING_MAP;
                    this.main.setModule(new MapModule())
                            .setTarget(this.main.starManager.byId(this.main.config.GENERAL.WORKING_MAP));
                }
            }
        }
    }

    private boolean isAttacking() {
        if (this.npcs == null) { return false; }
        if ((attacker.target = this.npcs.stream()
                .filter(s -> sentinel.isAttacking(s))
                .findAny().orElse(null)) == null) {
            return false;
        }

        main.hero.attackMode(attacker.target);
        attacker.doKillTargetTick();
        currentStatus = State.HELPING_MASTER;

        return (attacker.target != null);
    }

    private boolean shipAround() {
        if (this.ships == null) { return false; } 
        sentinel = this.ships.stream()
                .filter(ship -> (sConfig.SENTINEL_TAG.has(main.config.PLAYER_INFOS.get(ship.id))))
                .findAny().orElse(null);
        return sentinel != null;
    }

    private void goToLeader() {
        for (GroupMember m : main.guiManager.group.group.members) {
            if ((m.isLeader || sConfig.SENTINEL_TAG.has(main.config.PLAYER_INFOS.get(m.id))) && m.id != main.hero.id) {
                if (m.mapId == main.hero.map.id) {
                    drive.move(m.location);
                    currentStatus = State.FOLLOWING_MASTER;
                } else {
                    main.setModule(new MapModule()).setTarget(main.starManager.byId(m.mapId));
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
                .filter(in -> in.incomming && (sConfig.SENTINEL_TAG == null ||
                        sConfig.SENTINEL_TAG.has(main.config.PLAYER_INFOS.get(in.inviter.id))))
                .findFirst()
                .ifPresent(inv -> main.guiManager.group.acceptInvite(inv));
    }

}
