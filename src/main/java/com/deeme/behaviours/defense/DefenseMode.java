package com.deeme.behaviours.defense;

import com.deeme.modules.PVPModule;
import com.deeme.modules.SentinelModule;
import com.deeme.modules.pvp.AntiPushLogic;
import com.deeme.types.SharedFunctions;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Pet;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.group.GroupMember;
import eu.darkbot.api.game.other.EntityInfo.Diplomacy;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.GroupAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.MapModule;
import eu.darkbot.shared.modules.TemporalModule;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Feature(name = "Defense Mode", description = "Add enemy defense options")
public class DefenseMode implements Behavior, Configurable<DefenseConfig> {
    private final PluginAPI api;
    private final HeroAPI heroapi;
    private final MovementAPI movement;
    private final GroupAPI group;
    private final ConfigAPI configApi;
    private final BotAPI botApi;
    private final StatsAPI stats;
    private final Collection<? extends Player> players;

    private DefenseConfig defenseConfig;
    private AntiPushLogic antiPushLogic;
    private Ship target = null;

    public DefenseMode(PluginAPI api) {
        this(api, api.requireAPI(HeroAPI.class), api.requireAPI(MovementAPI.class),
                api.requireAPI(AuthAPI.class), api.requireAPI(ConfigAPI.class),
                api.requireAPI(EntitiesAPI.class));
    }

    @Inject
    public DefenseMode(PluginAPI api, HeroAPI hero, MovementAPI movement, AuthAPI auth,
            ConfigAPI configApi, EntitiesAPI entities) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog(api.requireAPI(ExtensionsAPI.class).getFeatureInfo(this.getClass()),
                auth.getAuthId());

        this.api = api;
        this.heroapi = hero;
        this.movement = movement;
        this.configApi = configApi;
        this.stats = api.requireAPI(StatsAPI.class);
        this.botApi = api.requireAPI(BotAPI.class);
        this.group = api.requireAPI(GroupAPI.class);
        this.players = entities.getPlayers();
        setup();
    }

    @Override
    public void setConfig(ConfigSetting<DefenseConfig> arg0) {
        this.defenseConfig = arg0.getValue();
        setup();
    }

    private void setup() {
        if (api == null || defenseConfig == null) {
            return;
        }

        this.antiPushLogic = new AntiPushLogic(heroapi, stats, this.defenseConfig.antiPush);
    }

    @Override
    public void onTickBehavior() {
        if (defenseConfig == null) {
            return;
        }

        if (antiPushLogic == null) {
            setup();
            if (antiPushLogic == null) {
                return;
            }
        }

        if (heroapi.getMap() != null && heroapi.getMap().isGG()) {
            return;
        }
        Module currentModule = botApi.getModule();
        if (currentModule != null && !(currentModule instanceof DefenseModule)
                && !((currentModule instanceof PVPModule || currentModule instanceof SentinelModule)
                        && heroapi.isAttacking())
                && !((currentModule instanceof TemporalModule)
                        && !(currentModule instanceof MapModule))
                && isUnderAttack()) {
            botApi.setModule(new DefenseModule(api, defenseConfig, target));
        }
        antiPushLogic.registerTarget(target);
    }

    private boolean inGroupAttacked(int id) {
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
        if (hasPreviousTarget() || hasAttacker()) {
            return true;
        }

        boolean friendNeedsHelp = friendUnderAttack();
        if (friendNeedsHelp) {
            goToMemberAttacked();
        }
        return friendNeedsHelp;
    }

    private boolean hasPreviousTarget() {
        return target != null && target.isValid() && target.getId() != heroapi.getId()
                && !(target instanceof Pet) && target.getLocationInfo()
                        .distanceTo(heroapi) < defenseConfig.rangeForAttackedEnemy;
    }

    private boolean friendUnderAttack() {
        target = getTarget(players.stream()
                .filter(this::shouldHelpPlayer)
                .collect(Collectors.toList()));

        return target != null && target.isValid();
    }

    private boolean hasAttacker() {
        if (!defenseConfig.respondAttacks) {
            return false;
        }

        target = SharedFunctions.getAttacker(heroapi, players, heroapi,
                !defenseConfig.defendEvenAreNotEnemies);
        if (target != null && target.isValid()
                && heroapi.distanceTo(target) <= defenseConfig.rangeForAttackedEnemy
                && !isIgnoredPlayer(target.getId())) {
            return true;
        }

        target = null;
        return false;
    }

    private Ship getTarget(Collection<? extends Player> ships) {
        for (Player ship : ships) {
            if (defenseConfig.helpAttack && ship.isAttacking() && ship.getTarget() != null) {
                Entity playerTarget = ship.getTarget();
                if (!(playerTarget instanceof Npc) && !(playerTarget instanceof Pet)
                        && !isIgnoredPlayer(playerTarget.getId())) {
                    Ship allyTarget = ship.getTargetAs(Ship.class);
                    if (allyTarget != null && allyTarget.isValid()) {
                        return allyTarget;
                    }
                }
            }

            Ship attacker = SharedFunctions.getAttacker(ship, players, heroapi,
                    !defenseConfig.defendEvenAreNotEnemies);
            if (attacker != null && attacker.isValid()
                    && !isIgnoredPlayer(attacker.getId())) {
                return attacker;
            }
        }

        return null;
    }

    private boolean shouldHelpPlayer(Player player) {
        if (!player.isValid() || player instanceof Pet) {
            return false;
        }

        return (defenseConfig.helpList.contains(HelpList.CLAN)
                && player.getEntityInfo().getClanId() == heroapi.getEntityInfo().getClanId())
                || (defenseConfig.helpList.contains(HelpList.ALLY)
                        && player.getEntityInfo().getClanDiplomacy() == Diplomacy.ALLIED)
                || (defenseConfig.helpList.contains(HelpList.GROUP)
                        && inGroupAttacked(player.getId()))
                || (defenseConfig.helpList.contains(HelpList.EVERYONE)
                        && !player.getEntityInfo().isEnemy());
    }

    private boolean isIgnoredPlayer(int playerId) {
        return antiPushLogic != null && antiPushLogic.getIgnoredPlayers().contains(playerId);
    }

    private void goToMemberAttacked() {
        if (!defenseConfig.goToGroup
                || !defenseConfig.respondAttacks
                || !defenseConfig.helpList.contains(HelpList.GROUP)) {
            return;
        }

        GroupMember member = SharedFunctions.getMemberGroupAttacked(group, heroapi, configApi);
        if (member != null) {
            movement.moveTo(member.getLocation());
        }
    }
}
