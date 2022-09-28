package com.deeme.behaviours.bestability;

import java.util.Arrays;
import java.util.Collection;

import com.deeme.modules.temporal.AmbulanceModule;
import com.deeme.types.SharedFunctions;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.github.manolo8.darkbot.extensions.util.Version;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.group.GroupMember;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Ability;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.GroupAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.utils.SafetyFinder;
import eu.darkbot.shared.utils.SafetyFinder.Escaping;

@Feature(name = "Auto Best Ability", description = "Auto use the best ability. Can use almost all abilities")
public class AutoBestAbility implements Behavior, Configurable<BestAbilityConfig> {

    protected final PluginAPI api;
    protected final BotAPI bot;
    protected final HeroAPI heroapi;
    protected final GroupAPI group;
    protected final HeroItemsAPI items;
    protected final SafetyFinder safety;
    private BestAbilityConfig config;

    private Collection<? extends Ship> allShips;

    public AutoBestAbility(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class),
                api.requireAPI(BotAPI.class), api.requireAPI(HeroItemsAPI.class));
    }

    @Inject
    public AutoBestAbility(PluginAPI api, AuthAPI auth, BotAPI bot, HeroItemsAPI items) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog();

        this.api = api;
        this.bot = bot;
        this.items = items;
        this.heroapi = api.getAPI(HeroAPI.class);
        this.group = api.getAPI(GroupAPI.class);
        this.safety = api.requireInstance(SafetyFinder.class);

        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        this.allShips = entities.getShips();
    }

    @Override
    public void setConfig(ConfigSetting<BestAbilityConfig> arg0) {
        this.config = arg0.getValue();
    }

    @Override
    public void onTickBehavior() {
        Entity target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            if (config.npcEnabled || !(target instanceof Npc)) {
                useSelectableReadyWhenReady(getBestAbility());
            }
        } else if (safety.state() == Escaping.ENEMY) {
            useSelectableReadyWhenReady(getBestAbility());
        }
    }

    private Ability getBestAbility() {
        if (shoulFocusHealth()) {
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
            if (bot.getVersion().compareTo(new Version("1.13.17 beta 109 alpha 15")) > 1) {
                if (items.getItem(Ability.SOLACE_PLUS_NANO_CLUSTER_REPAIRER_PLUS, ItemFlag.USABLE, ItemFlag.READY)
                        .isPresent()) {
                    return Ability.SOLACE_PLUS_NANO_CLUSTER_REPAIRER_PLUS;
                }
            }
        }

        if (shoulFocusShield()
                && items.getItem(Ability.AEGIS_SHIELD_REPAIR, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
            return Ability.AEGIS_SHIELD_REPAIR;
        }

        if (shoulFocusSpeed()) {
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

        if (shoulFocusEvade()) {
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

        if (shouldFocusHelpTank()) {
            if (items.getItem(Ability.CITADEL_DRAW_FIRE, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.CITADEL_DRAW_FIRE;
            }
            if (items.getItem(Ability.CITADEL_PROTECTION, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
                return Ability.CITADEL_PROTECTION;
            }
        }

        if (shoulFocusEvade()) {
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

        if (shoulFocusDamage()) {
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

    private boolean shoulFocusDamage() {
        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            if (target.getEntityInfo() != null && target.getEntityInfo().isEnemy()
                    && target.getHealth().hpPercent() > 0.3) {
                return true;
            }
        }

        return false;
    }

    private boolean shoulFocusSpeed() {
        if (safety.state() == Escaping.ENEMY) {
            return true;
        }
        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            double distance = heroapi.getLocationInfo().getCurrent().distanceTo(target.getLocationInfo());
            double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
            return distance > 800 && speed > heroapi.getSpeed();
        }
        return false;
    }

    private boolean shoulFocusHealth() {
        if (bot.getModule() != null && bot.getModule().getClass() == AmbulanceModule.class) {
            return false;
        }
        if (heroapi.getHealth().hpPercent() < 0.5 && heroapi.getEffects() != null
                && !heroapi.getEffects().toString().contains("76")) {
            return true;
        }
        if (group.hasGroup()) {
            for (GroupMember member : group.getMembers()) {
                if (!member.isDead() && member.isAttacked() && member.isLocked()
                        && member.getMemberInfo().hpPercent() < 0.5) {
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
                if (!member.isDead() && member.isAttacked() && member.getLocation().distanceTo(heroapi) < 1000) {
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
                if (!member.isDead() && member.isAttacked() && member.isLocked()
                        && member.getMemberInfo().shieldPercent() < 0.5) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shoulFocusEvade() {
        if (allShips == null || allShips.isEmpty()) {
            return false;
        }
        Entity target = SharedFunctions.getAttacker(heroapi, allShips, heroapi);
        return target != null;
    }

    private boolean useSelectableReadyWhenReady(SelectableItem selectableItem) {
        if (selectableItem == null) {
            return false;
        }

        if (items.useItem(selectableItem, 500, ItemFlag.USABLE, ItemFlag.READY).isSuccessful()) {
            return true;
        }

        return false;
    }
}
