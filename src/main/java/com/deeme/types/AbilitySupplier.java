package com.deeme.types;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.group.GroupMember;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Ability;
import eu.darkbot.api.game.items.SelectableItem.Formation;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.GroupAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;

public class AbilitySupplier implements PrioritizedSupplier<SelectableItem> {
    protected HeroItemsAPI items;
    protected HeroAPI heroapi;
    protected GroupAPI group;
    protected final PluginAPI api;

    private boolean focusShield, focusHealth, focusSpeed, focusEvade, focusDamage, focusHelpTank = false;

    List<String> damageOrder = Arrays.asList(Formation.DRILL.getId(), Formation.PINCER.getId(), Formation.STAR.getId(),
            Formation.DOUBLE_ARROW.getId());

    public AbilitySupplier(PluginAPI api) {
        this.api = api;
        this.heroapi = api.getAPI(HeroAPI.class);
        this.group = api.getAPI(GroupAPI.class);
        this.items = api.getAPI(HeroItemsAPI.class);
    }

    public SelectableItem get() {
        if (focusHealth) {
            if (items.getItem(Ability.AEGIS_REPAIR_POD, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.AEGIS_REPAIR_POD;
            }
            if (items.getItem(Ability.AEGIS_HP_REPAIR, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.AEGIS_HP_REPAIR;
            }
            if (items.getItem(Ability.LIBERATOR_PLUS_SELF_REPAIR, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.LIBERATOR_PLUS_SELF_REPAIR;
            }
            if (items.getItem(Ability.SOLACE, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.SOLACE;
            }
        }

        if (focusShield && items.getItem(Ability.AEGIS_SHIELD_REPAIR, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
            return Ability.AEGIS_SHIELD_REPAIR;
        }

        if (focusSpeed) {
            if (items.getItem(Ability.CITADEL_TRAVEL, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.CITADEL_TRAVEL;
            }
            if (items.getItem(Ability.LIGHTNING, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.LIGHTNING;
            }
            if (items.getItem(Ability.TARTARUS_SPEED_BOOST, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.TARTARUS_SPEED_BOOST;
            }
            if (items.getItem(Ability.KERES_SPR, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.KERES_SPR;
            }
            if (items.getItem(Ability.RETIARUS_SPC, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.RETIARUS_SPC;
            }
            if (items.getItem(Ability.MIMESIS_PHASE_OUT, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.MIMESIS_PHASE_OUT;
            }
            if (items.getItem(Ability.ZEPHYR_MMT, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.ZEPHYR_MMT;
            }
        }

        if (focusEvade) {
            if (items.getItem(Ability.SPEARHEAD_ULTIMATE_CLOAK, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.SPEARHEAD_ULTIMATE_CLOAK;
            }
            if (items.getItem(Ability.BERSERKER_RVG, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.BERSERKER_RVG;
            }
            if (items.getItem(Ability.MIMESIS_SCRAMBLE, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.MIMESIS_SCRAMBLE;
            }
            if (items.getItem(Ability.DISRUPTOR_DDOL, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.DISRUPTOR_DDOL;
            }
        }

        if (focusHelpTank) {
            if (items.getItem(Ability.CITADEL_DRAW_FIRE, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.CITADEL_DRAW_FIRE;
            }
            if (items.getItem(Ability.CITADEL_PROTECTION, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.CITADEL_PROTECTION;
            }
        }

        if (focusEvade) {
            if (items.getItem(Ability.CITADEL_PLUS_PRISMATIC_ENDURANCE, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.CITADEL_PLUS_PRISMATIC_ENDURANCE;
            }
            if (items.getItem(Ability.CITADEL_FORTIFY, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.CITADEL_FORTIFY;
            }
            if (items.getItem(Ability.DISRUPTOR_REDIRECT, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.DISRUPTOR_REDIRECT;
            }
            if (items.getItem(Ability.SPECTRUM, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.SPECTRUM;
            }
            if (items.getItem(Ability.SENTINEL, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.SENTINEL;
            }
            if (items.getItem(Ability.BERSERKER_BSK, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.BERSERKER_BSK;
            }
            if (items.getItem(Ability.ORCUS_ASSIMILATE, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.ORCUS_ASSIMILATE;
            }
        }

        if (focusDamage) {
            if (items.getItem(Ability.SPEARHEAD_TARGET_MARKER, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.SPEARHEAD_TARGET_MARKER;
            }
            if (items.getItem(Ability.DIMINISHER, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.DIMINISHER;
            }
            if (items.getItem(Ability.GOLIATH_X_FROZEN_CLAW, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.GOLIATH_X_FROZEN_CLAW;
            }
            if (items.getItem(Ability.VENOM, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.VENOM;
            }
            if (items.getItem(Ability.SOLARIS_INC, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.SOLARIS_INC;
            }
            if (items.getItem(Ability.TARTARUS_RAPID_FIRE, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.TARTARUS_RAPID_FIRE;
            }
            if (items.getItem(Ability.DISRUPTOR_SHIELD_DISARRAY, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.DISRUPTOR_SHIELD_DISARRAY;
            }
            if (items.getItem(Ability.HECATE_PARTICLE_BEAM, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.HECATE_PARTICLE_BEAM;
            }
            if (items.getItem(Ability.KERES_SPR, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.KERES_SPR;
            }
            if (items.getItem(Ability.ZEPHYR_TBR, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.ZEPHYR_TBR;
            }
            if (items.getItem(Ability.HOLO_ENEMY_REVERSAL, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.HOLO_ENEMY_REVERSAL;
            }
        }

        return null;
    }

    @Override
    public Priority getPriority() {
        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            focusSpeed = shoulFocusSpeed(target);
            focusHealth = shoulFocusHealth();
            focusShield = shoulFocusShield();
            focusEvade = shoulFocusEvade();
            focusHelpTank = shouldFocusHelpTank();
            focusDamage = shoulFocusDamage(target);
        }
        return focusHealth || focusShield ? Priority.HIGHEST
                : focusSpeed ? Priority.HIGH
                        : focusEvade || focusHelpTank ? Priority.MODERATE : Priority.LOWEST;
    }

    private boolean shoulFocusDamage(Lockable target) {
        if (target.getEntityInfo() != null && target.getEntityInfo().isEnemy()
                && target.getHealth().hpPercent() > 0.3) {
            return true;
        }

        return false;
    }

    private boolean shoulFocusSpeed(Lockable target) {
        double distance = heroapi.getLocationInfo().getCurrent().distanceTo(target.getLocationInfo());
        double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
        return distance > 800 && speed > heroapi.getSpeed();
    }

    private boolean shoulFocusHealth() {
        if (heroapi.getHealth().hpPercent() < 0.5) {
            return true;
        }
        if (group.hasGroup()) {
            for (GroupMember member : group.getMembers()) {
                if (member.isAttacked() && member.isLocked() && member.getMemberInfo().hpPercent() < 0.5) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shouldFocusHelpTank() {
        if (heroapi.getHealth().shieldPercent() < 0.5) {
            return false;
        }

        if (group.hasGroup()) {
            for (GroupMember member : group.getMembers()) {
                if (member.isAttacked() && member.getLocation().distanceTo(heroapi) < 500) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shoulFocusShield() {
        if (heroapi.getHealth().shieldPercent() < 0.5) {
            return true;
        }

        if (group.hasGroup()) {
            for (GroupMember member : group.getMembers()) {
                if (member.isAttacked() && member.isLocked() && member.getMemberInfo().shieldPercent() < 0.5) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shoulFocusEvade() {
        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        Collection<? extends Ship> allShips = entities.getShips();
        Entity target = SharedFunctions.getAttacker(heroapi, allShips, heroapi);
        return target != null;
    }
}