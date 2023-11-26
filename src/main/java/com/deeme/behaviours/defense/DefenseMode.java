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
import eu.darkbot.api.managers.GroupAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.MapModule;
import eu.darkbot.shared.modules.TemporalModule;

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
    protected final StatsAPI stats;
    protected final Collection<? extends Player> players;

    private DefenseConfig defenseConfig;
    private AntiPushLogic antiPushLogic;
    private Ship target = null;

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
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog(auth.getAuthId());

        this.api = api;
        this.heroapi = hero;
        this.movement = movement;
        this.configApi = configApi;
        this.stats = api.getAPI(StatsAPI.class);
        this.botApi = api.getAPI(BotAPI.class);
        this.group = api.getAPI(GroupAPI.class);
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
        if (heroapi.getMap() != null && heroapi.getMap().isGG()) {
            return;
        }
        if (botApi.getModule() != null && botApi.getModule().getClass() != DefenseModule.class
                && !((botApi.getModule().getClass() == PVPModule.class
                        || botApi.getModule().getClass() == SentinelModule.class) && heroapi.isAttacking())
                && !((botApi.getModule() instanceof TemporalModule)
                        && botApi.getModule().getClass() != MapModule.class)
                && isUnderAttack()) {
            botApi.setModule(new DefenseModule(api, defenseConfig, target));
        }
        this.antiPushLogic.registerTarget(target);
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
        goToMemberAttacked();

        return hasPreviusTarget() || hasAttacker() || friendUnderAttack();
    }

    private boolean hasPreviusTarget() {
        return target != null && target.isValid() && target.getId() != heroapi.getId() && !(target instanceof Pet)
                && target.getLocationInfo().distanceTo(heroapi) < defenseConfig.rangeForAttackedEnemy;
    }

    private boolean friendUnderAttack() {
        List<Player> ships = players.stream()
                .filter(Player::isValid)
                .filter(s -> !(s instanceof Pet))
                .filter(s -> (defenseConfig.helpList.contains(HelpList.CLAN)
                        && s.getEntityInfo().getClanId() == heroapi.getEntityInfo().getClanId())
                        || (defenseConfig.helpList.contains(HelpList.ALLY)
                                && s.getEntityInfo().getClanDiplomacy() == Diplomacy.ALLIED)
                        || (defenseConfig.helpList.contains(HelpList.GROUP) && inGroupAttacked(s.getId())
                                || (defenseConfig.helpList.contains(HelpList.EVERYONE)
                                        && !s.getEntityInfo().isEnemy())))
                .collect(Collectors.toList());

        target = getTarget(ships);

        return target != null && target.isValid();
    }

    private boolean hasAttacker() {
        if (!defenseConfig.respondAttacks) {
            return false;
        }

        target = SharedFunctions.getAttacker(heroapi, players, heroapi, !defenseConfig.defendEvenAreNotEnemies);
        if (target != null && target.isValid() && heroapi.distanceTo(target) <= defenseConfig.rangeForAttackedEnemy
                && !this.antiPushLogic.getIgnoredPlayers().contains(target.getId())) {
            return true;
        }

        target = null;
        return false;
    }

    private Ship getTarget(List<Player> ships) {
        if (!ships.isEmpty()) {
            for (Player ship : ships) {
                if (defenseConfig.helpAttack && ship.isAttacking() && ship.getTarget() != null) {
                    Entity tar = ship.getTarget();
                    if (!(tar instanceof Npc) && !(tar instanceof Pet)) {
                        return ship.getTargetAs(Ship.class);
                    }
                }

                Ship tar = SharedFunctions.getAttacker(ship, players, heroapi, !defenseConfig.defendEvenAreNotEnemies);
                if (tar != null && tar.isValid()) {
                    return tar;
                }
            }
        }
        return null;
    }

    private void goToMemberAttacked() {
        if (!defenseConfig.goToGroup) {
            return;
        }

        GroupMember member = SharedFunctions.getMemberGroupAttacked(group, heroapi, configApi);
        if (member != null) {
            movement.moveTo(member.getLocation());
        }
    }
}
