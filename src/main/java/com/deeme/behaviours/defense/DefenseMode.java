package com.deeme.behaviours.defense;

import com.deeme.modules.PVPModule;
import com.deeme.modules.SentinelModule;
import com.deeme.types.SharedFunctions;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.game.group.GroupMember;
import eu.darkbot.api.game.other.EntityInfo.Diplomacy;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.GroupAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.utils.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Feature(name = "Defense Mode", description = "Add enemy defense options")
public class DefenseMode implements Behavior, Configurable<DefenseConfig> {
    protected final PluginAPI api;
    protected final HeroAPI heroapi;
    protected final MovementAPI movement;
    protected final GroupAPI group;
    protected final ConfigAPI configApi;
    protected final BotAPI botApi;
    protected final Collection<? extends Player> players;
    private DefenseConfig defenseConfig;
    private Entity target = null;

    public DefenseMode(PluginAPI api) {
        this(api, api.requireAPI(HeroAPI.class),
                api.requireAPI(MovementAPI.class),
                api.requireAPI(AuthAPI.class),
                api.requireAPI(ConfigAPI.class),
                api.requireAPI(EntitiesAPI.class));
    }

    @Inject
    public DefenseMode(PluginAPI api, HeroAPI hero, MovementAPI movement, AuthAPI auth, ConfigAPI configApi,
            EntitiesAPI entities) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog();

        this.api = api;
        this.heroapi = hero;
        this.movement = movement;
        this.configApi = configApi;
        this.botApi = api.getAPI(BotAPI.class);
        this.group = api.getAPI(GroupAPI.class);
        this.players = entities.getPlayers();
    }

    @Override
    public void setConfig(ConfigSetting<DefenseConfig> arg0) {
        this.defenseConfig = arg0.getValue();
    }

    @Override
    public void onTickBehavior() {
        if (heroapi.getMap() != null && heroapi.getMap().isGG()) {
            return;
        }
        if (botApi.getModule() != null && botApi.getModule().getClass() != DefenseModule.class
                && !(botApi.getModule().getClass() == PVPModule.class && heroapi.isAttacking())
                && !(botApi.getModule().getClass() == SentinelModule.class && heroapi.isAttacking())
                && isUnderAttack()) {
            botApi.setModule(new DefenseModule(api, defenseConfig, target));
        }
    }

    public boolean inGroupAttacked(int id) {
        if (group.hasGroup()) {
            for (GroupMember member : group.getMembers()) {
                if (!member.isDead() && member.getId() == id && member.isAttacked()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isUnderAttack() {
        if (target != null && target.isValid() && (!defenseConfig.ignoreEnemies
                || target.getLocationInfo().distanceTo(heroapi) < 1500)) {
            return true;
        }

        if (defenseConfig.respondAttacks) {
            target = SharedFunctions.getAttacker(heroapi, players, heroapi);
            if (target != null) {
                return true;
            }
        }

        List<Player> ships = players.stream()
                .filter(s -> (defenseConfig.helpAllies && s.getEntityInfo().getClanDiplomacy() == Diplomacy.ALLIED)
                        ||
                        (defenseConfig.helpEveryone && !s.getEntityInfo().isEnemy())
                        || (defenseConfig.helpGroup && inGroupAttacked(s.getId())))
                .collect(Collectors.toList());

        if (!ships.isEmpty()) {
            for (Player ship : ships) {
                if (defenseConfig.helpAttack && ship.isAttacking() && ship.getTarget() != null) {
                    Entity tar = ship.getTarget();
                    if (!(tar instanceof Npc)) {
                        target = tar;
                        return true;
                    }
                }

                target = SharedFunctions.getAttacker(ship, players, heroapi);
                if (target != null) {
                    return true;
                }
            }
        }

        if (defenseConfig.goToGroup) {
            goToMemberAttacked();
        }

        return target != null && target.isValid();
    }

    public void goToMemberAttacked() {
        GroupMember member = getMemberGroupAttacked();
        if (member != null) {
            movement.moveTo(member.getLocation());
        }
    }

    private GroupMember getMemberGroupAttacked() {
        if (group.hasGroup()) {
            for (GroupMember member : group.getMembers()) {
                if (!member.isDead() && member.getMapId() == heroapi.getMap().getId() && member.isAttacked()
                        && member.getTargetInfo() != null
                        && member.getTargetInfo().getShipType() != 0 && !member.getTargetInfo().getUsername().isEmpty()
                        && !SharedFunctions.isNpc(configApi, member.getTargetInfo().getUsername())) {
                    return member;

                }
            }
        }
        return null;
    }
}
