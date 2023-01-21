package com.deeme.behaviours.ambulance;

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
    private State currentStatus;
    private long maxModuleTime = 0;

    private boolean abilityUsed = false;
    private boolean returnToTarget = true;

    private enum State {
        INIT("Init"),
        TRAVEL_TO_MEMBER("Travelling to the group member"),
        TARGET_MEMBER("Targeting the group member"),
        TARGET_OLD_TARGET("Targeting the old target"),
        USING_ABILITY("Using the ship's ability"),
        HELPING_MASTER("Helping the master"),
        SEARCHING_MEMBER("Searching for the member");

        private final String message;

        State(String message) {
            this.message = message;
        }
    }

    public AmbulanceModule(PluginAPI api, int idMember, Ability abilityToUse, boolean returnToTarget) {
        this(api, api.requireAPI(BotAPI.class), idMember, abilityToUse, returnToTarget);
    }

    @Inject
    public AmbulanceModule(PluginAPI api, BotAPI bot, int idMember, Ability abilityToUse, boolean returnToTarget) {
        super(bot);
        this.api = api;
        this.movement = api.getAPI(MovementAPI.class);
        this.groupAPI = api.getAPI(GroupAPI.class);
        this.heroapi = api.getAPI(HeroAPI.class);
        this.items = api.getAPI(HeroItemsAPI.class);

        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        this.idMember = idMember;
        this.abilityToUse = abilityToUse;
        this.players = entities.getPlayers();
        this.currentStatus = State.INIT;
        this.maxModuleTime = System.currentTimeMillis() + 60000;
        this.returnToTarget = returnToTarget;
        this.abilityUsed = false;
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
        try {
            if (idMember == 0 || abilityToUse == null) {
                super.goBack();
                return;
            }
            if (maxModuleTime != 0 && maxModuleTime < System.currentTimeMillis()) {
                super.goBack();
                return;
            }
            if (abilityUsed) {
                if (returnToTarget) {
                    selectOldTarget();
                } else {
                    super.goBack();
                }
                return;
            }
            this.currentStatus = State.SEARCHING_MEMBER;
            Player player = getPlayerIfIsClosed();
            if (player != null && player.isValid()) {
                if (abilityToUse == Ability.AEGIS_REPAIR_POD) {
                    useAbilityReadyWhenReady();
                } else {
                    if (heroapi.getTarget() != null && heroapi.getTarget().getId() != player.getId()) {
                        oldTarget = heroapi.getLocalTarget();
                    }
                    this.currentStatus = State.TARGET_MEMBER;
                    movement.moveTo(player);
                    if (heroapi.getLocationInfo().distanceTo(player) <= 700) {
                        if (heroapi.getTarget() != null && heroapi.getTarget().getId() == player.getId()) {
                            useAbilityReadyWhenReady();
                        } else if (System.currentTimeMillis() - clickDelay > 500) {
                            heroapi.setLocalTarget(player);
                            player.trySelect(false);
                            clickDelay = System.currentTimeMillis();
                        }
                    }
                }
            } else {
                goToMemberAttacked();
            }
        } catch (Exception e) {
            System.err.println(e);
            super.goBack();
        }
    }

    private void selectOldTarget() {
        if (oldTarget != null && oldTarget.isValid() && heroapi.getTarget() != null
                && heroapi.getTarget() != oldTarget) {
            this.currentStatus = State.TARGET_OLD_TARGET;
            movement.moveTo(oldTarget);
            if (heroapi.getLocationInfo().distanceTo(oldTarget) <= 700) {
                if (System.currentTimeMillis() - clickDelay > 500) {
                    heroapi.setLocalTarget(oldTarget);
                    oldTarget.trySelect(false);
                    clickDelay = System.currentTimeMillis();
                }
            } else {
                super.goBack();
            }
        } else {
            super.goBack();
        }
    }

    private Player getPlayerIfIsClosed() {
        return players.stream().filter(player -> player.getId() == idMember && heroapi.distanceTo(player) <= 2000)
                .findFirst().orElse(null);
    }

    public void goToMemberAttacked() {
        GroupMember member = getMember();
        if (member != null && !member.isDead()) {
            this.currentStatus = State.TRAVEL_TO_MEMBER;
            movement.moveTo(member.getLocation());
        } else {
            idMember = 0;
        }
    }

    private void useAbilityReadyWhenReady() {
        this.currentStatus = State.USING_ABILITY;
        if (abilityToUse == null) {
            abilityUsed = true;
            return;
        }

        if (items.getItem(abilityToUse, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
            if (items.useItem(abilityToUse).isSuccessful()) {
                abilityUsed = true;
            }
        } else {
            abilityUsed = true;
        }
    }

    private GroupMember getMember() {
        if (groupAPI.hasGroup()) {
            for (GroupMember member : groupAPI.getMembers()) {
                if (!member.isDead() && member.getMapId() == heroapi.getMap().getId() && member.getId() == idMember) {
                    return member;
                }
            }
        }
        return null;
    }

}
