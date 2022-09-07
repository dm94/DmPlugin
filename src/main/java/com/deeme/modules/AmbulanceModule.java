package com.deeme.modules;

import java.util.Collection;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.game.group.GroupMember;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem.Ability;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.GroupAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.TemporalModule;

public class AmbulanceModule extends TemporalModule {
    protected final PluginAPI api;
    protected final HeroAPI heroapi;
    protected final MovementAPI movement;
    protected final GroupAPI groupAPI;
    protected final HeroItemsAPI items;
    protected final Collection<? extends Player> players;

    private int idMember = 0;
    private Ability abilityToUse = null;
    private Lockable oldTarget = null;

    private long clickDelay;
    private long keyDelay;
    private State currentStatus;

    private boolean abilityUsed = false;

    private enum State {
        INIT("Init"),
        TRAVEL_TO_MEMBER("Travelling to the group member"),
        TARGET_MEMBER("Targeting the group member"),
        TARGET_OLD_TARGET("Targeting the old target"),
        USING_ABILITY("Using the ship's ability"),
        HELPING_MASTER("Helping the master");

        private final String message;

        State(String message) {
            this.message = message;
        }
    }

    public AmbulanceModule(PluginAPI api, int idMember, Ability abilityToUse) {
        this(api, api.requireAPI(BotAPI.class),
                api.requireAPI(HeroAPI.class),
                api.requireAPI(MovementAPI.class),
                api.requireAPI(EntitiesAPI.class), api.requireAPI(GroupAPI.class), idMember, abilityToUse);

    }

    @Inject
    public AmbulanceModule(PluginAPI api, BotAPI bot, HeroAPI hero, MovementAPI movement,
            EntitiesAPI entities, GroupAPI groupAPI, int idMember, Ability abilityToUse) {
        super(bot);
        this.api = api;
        this.heroapi = hero;
        this.movement = movement;
        this.groupAPI = groupAPI;
        this.items = api.getAPI(HeroItemsAPI.class);
        this.idMember = idMember;
        this.abilityToUse = abilityToUse;
        this.players = entities.getPlayers();
        this.currentStatus = State.INIT;
    }

    @Override
    public boolean canRefresh() {
        return false;
    }

    @Override
    public String getStatus() {
        return "Ambulance Mode | " + this.currentStatus.message;
    }

    @Override
    public void onTickModule() {
        if (idMember != 0 && abilityToUse != null) {
            if (!abilityUsed) {
                Player player = getPlayerIfIsClosed();
                if (player != null) {
                    if (abilityToUse == Ability.AEGIS_REPAIR_POD) {
                        useAbilityReadyWhenReady();
                    } else {
                        if (heroapi.getTarget() != null && heroapi.getTarget().getId() != player.getId()) {
                            oldTarget = heroapi.getLocalTarget();
                        } else if (heroapi.getTarget() != null && heroapi.getTarget().getId() == player.getId()) {
                            useAbilityReadyWhenReady();
                        } else {
                            this.currentStatus = State.TARGET_MEMBER;
                            if (heroapi.getLocationInfo().distanceTo(player) < 800) {
                                if (System.currentTimeMillis() - clickDelay > 500) {
                                    heroapi.setLocalTarget(player);
                                    player.trySelect(false);
                                    clickDelay = System.currentTimeMillis();
                                }
                            } else {
                                movement.moveTo(player);
                            }
                        }
                    }
                } else {
                    goToMemberAttacked();
                }
            } else {
                if (oldTarget != null && oldTarget.isValid() && heroapi.getTarget() != null
                        && heroapi.getTarget() != oldTarget) {
                    this.currentStatus = State.TARGET_OLD_TARGET;
                    if (heroapi.getLocationInfo().distanceTo(oldTarget) < 800) {
                        if (System.currentTimeMillis() - clickDelay > 500) {
                            heroapi.setLocalTarget(oldTarget);
                            oldTarget.trySelect(false);
                            clickDelay = System.currentTimeMillis();
                        }
                    } else {
                        movement.moveTo(oldTarget);
                    }
                } else {
                    super.goBack();
                }
            }
        } else {
            super.goBack();
        }
    }

    private Player getPlayerIfIsClosed() {
        return players.stream().filter(player -> player.getId() == idMember && heroapi.distanceTo(player) <= 1000)
                .findFirst().orElse(null);
    }

    public void goToMemberAttacked() {
        GroupMember member = getMember();
        if (member != null) {
            this.currentStatus = State.TRAVEL_TO_MEMBER;
            movement.moveTo(member.getLocation());
        } else {
            idMember = 0;
        }
    }

    private void useAbilityReadyWhenReady() {
        this.currentStatus = State.USING_ABILITY;
        if (System.currentTimeMillis() - keyDelay < 1000)
            abilityUsed = false;
        if (abilityToUse == null) {
            abilityUsed = false;
        }

        boolean isReady = items.getItem(abilityToUse, ItemFlag.USABLE, ItemFlag.READY).isPresent();

        if (isReady && items.useItem(abilityToUse).isSuccessful()) {
            keyDelay = System.currentTimeMillis();
            abilityUsed = true;
        }
    }

    private GroupMember getMember() {
        if (groupAPI.hasGroup()) {
            for (GroupMember member : groupAPI.getMembers()) {
                if (member.getMapId() == heroapi.getMap().getId() && member.getId() == idMember) {
                    return member;
                }
            }
        }
        return null;
    }

}
