package com.deeme.behaviours.bestability;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.deeme.behaviours.ambulance.AmbulanceModule;
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
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.enums.EntityEffect;
import eu.darkbot.api.game.group.GroupMember;
import eu.darkbot.api.game.items.Item;
import eu.darkbot.api.game.items.ItemCategory;
import eu.darkbot.api.game.items.ItemFlag;
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
    private Collection<? extends Ship> allPlayers;
    private Collection<? extends Ship> allNPCs;

    private final List<Ability> damageAbilities = Arrays.asList(Ability.SPEARHEAD_TARGET_MARKER, Ability.DIMINISHER,
            Ability.GOLIATH_X_FROZEN_CLAW, Ability.VENOM, Ability.TARTARUS_RAPID_FIRE,
            Ability.DISRUPTOR_SHIELD_DISARRAY, Ability.HECATE_PARTICLE_BEAM, Ability.KERES_SPR, Ability.ZEPHYR_TBR,
            Ability.HOLO_ENEMY_REVERSAL);

    private final List<Ability> speedAbilities = Arrays.asList(Ability.CITADEL_TRAVEL, Ability.LIGHTNING,
            Ability.TARTARUS_SPEED_BOOST, Ability.KERES_SPR, Ability.MIMESIS_PHASE_OUT,
            Ability.ZEPHYR_MMT, Ability.PUSAT_PLUS_SPEED_SAP, Ability.RETIARUS_SPC);

    private final List<Ability> healthAbilitiesWithoutLock = Arrays.asList(Ability.AEGIS_REPAIR_POD,
            Ability.LIBERATOR_PLUS_SELF_REPAIR, Ability.SOLACE, Ability.SOLACE_PLUS_NANO_CLUSTER_REPAIRER_PLUS);

    private final List<Ability> evadeAbilities = Arrays.asList(Ability.SPEARHEAD_JAM_X,
            Ability.SPEARHEAD_ULTIMATE_CLOAK, Ability.BERSERKER_RVG, Ability.MIMESIS_SCRAMBLE,
            Ability.DISRUPTOR_DDOL);

    private final List<Ability> evadeLastInstanceAbilities = Arrays.asList(Ability.CITADEL_PLUS_PRISMATIC_ENDURANCE,
            Ability.CITADEL_FORTIFY, Ability.DISRUPTOR_REDIRECT, Ability.SPECTRUM,
            Ability.SENTINEL, Ability.BERSERKER_BSK);

    private long nextCheck = 0;

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
        this.allPlayers = entities.getPlayers();
        this.allNPCs = entities.getNpcs();
    }

    @Override
    public void setConfig(ConfigSetting<BestAbilityConfig> arg0) {
        this.config = arg0.getValue();
    }

    @Override
    public void onStoppedBehavior() {
        if (config.tickStopped) {
            onTickBehavior();
        }
    }

    @Override
    public void onTickBehavior() {
        if (nextCheck < System.currentTimeMillis()) {
            nextCheck = System.currentTimeMillis() + (config.timeToCheck * 1000);
            useSelectableReadyWhenReady(getBestAbility());
            useSelectableReadyWhenReady(getAbilityAlwaysToUse());
        }
    }

    private Ability getAbilityAlwaysToUse() {
        if (config.abilitiesToUseEverytime == null) {
            return null;
        }

        try {
            return items.getItems(ItemCategory.SHIP_ABILITIES).stream()
                    .filter(Item::isReadyToUse)
                    .map(s -> s.getAs(Ability.class))
                    .filter(s -> s != null
                            && config.abilitiesToUseEverytime.stream()
                                    .anyMatch(a -> a != null && a.name().equals(s.name())))
                    .findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Ability getBestAbility() {
        Ability healthAbility = getHealthAbility();
        if (healthAbility != null) {
            return healthAbility;
        }

        if (isAvailable(Ability.AEGIS_HP_REPAIR) && shouldFocusHealth(true)) {
            return Ability.AEGIS_HP_REPAIR;
        }

        if (shouldFocusShield()) {
            if (isAvailable(Ability.AEGIS_SHIELD_REPAIR)) {
                return Ability.AEGIS_SHIELD_REPAIR;
            } else if (isAvailable(Ability.SENTINEL)) {
                return Ability.SENTINEL;
            }
        }

        Ability speedAbility = getSpeedAbility();
        if (speedAbility != null) {
            return speedAbility;
        }

        Ability evadeAbility = getEvadeAbility();
        if (evadeAbility != null) {
            return evadeAbility;
        }

        if (shouldFocusHelpTank()) {
            if (isAvailable(Ability.CITADEL_DRAW_FIRE)) {
                return Ability.CITADEL_DRAW_FIRE;
            } else if (isAvailable(Ability.CITADEL_PROTECTION)) {
                return Ability.CITADEL_PROTECTION;
            }
        }

        Ability evadeAbilityLastInstance = getEvadeAbilityLastInstance();
        if (evadeAbilityLastInstance != null) {
            return evadeAbilityLastInstance;
        }

        return getDamageAbility();
    }

    private Ability getHealthAbility() {
        if (!shouldFocusHealth(false)) {
            return null;
        }

        return getAbilityAvailableFromList(healthAbilitiesWithoutLock);
    }

    private Ability getSpeedAbility() {
        if (!shouldFocusSpeed()) {
            return null;
        }

        return getAbilityAvailableFromList(speedAbilities);
    }

    private Ability getEvadeAbility() {
        if (!shouldFocusEvade()) {
            return null;
        }

        return getAbilityAvailableFromList(evadeAbilities);
    }

    private Ability getEvadeAbilityLastInstance() {
        if (!shouldFocusEvade()) {
            return null;
        }

        if (shouldUseOrcusAssimilate()) {
            return Ability.ORCUS_ASSIMILATE;
        }

        return getAbilityAvailableFromList(evadeLastInstanceAbilities);
    }

    private Ability getDamageAbility() {
        if (!shouldFocusDamage() || hasISH()) {
            return null;
        }

        if (isInRange()) {
            if (isAvailable(Ability.SOLARIS_INC)) {
                return Ability.SOLARIS_INC;
            } else if (isAvailable(Ability.SOLARIS_PLUS_INCINERATE_PLUS)) {
                return Ability.SOLARIS_PLUS_INCINERATE_PLUS;
            }
        }

        return getAbilityAvailableFromList(damageAbilities);
    }

    private boolean hasISH() {
        Lockable target = heroapi.getLocalTarget();
        return target != null && target.isValid() && (target.hasEffect(EntityEffect.ISH)
                || target.hasEffect(EntityEffect.NPC_ISH) || target.hasEffect(EntityEffect.PET_SPAWN));
    }

    private boolean shouldFocusDamage() {
        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            if (!config.npcEnabled && heroapi.getLocalTarget() instanceof Npc) {
                return false;
            }
            return (heroapi.isAttacking() && target.getHealth() != null
                    && target.getHealth().getHp() >= this.config.minHealthToUseDamage);
        }

        return false;
    }

    private Ability getAbilityAvailableFromList(List<Ability> abilities) {
        return abilities.stream().filter(this::isAvailable).findFirst().orElse(null);
    }

    private boolean isInRange() {
        Lockable target = heroapi.getLocalTarget();
        return (target != null && target.isValid() && heroapi.distanceTo(target) < 650);
    }

    private boolean shouldFocusSpeed() {
        if (safety.state() == Escaping.ENEMY) {
            return true;
        }

        Lockable target = heroapi.getLocalTarget();

        if (target != null && target.isValid()) {
            if (!config.npcEnabled && heroapi.getLocalTarget() instanceof Npc) {
                return false;
            }

            double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
            return heroapi.distanceTo(target) >= 650 || speed > heroapi.getSpeed();
        }
        return false;
    }

    private boolean shouldFocusHealth(boolean needLock) {
        if (bot.getModule() != null && bot.getModule().getClass() == AmbulanceModule.class) {
            return false;
        } else if (heroapi.getEffects() != null
                && heroapi.getEffects().toString().contains("76")) {
            return false;
        } else if (heroapi.getHealth().hpPercent() <= this.config.minHealthToUseHealth) {
            return true;
        } else if (group.hasGroup()) {
            for (GroupMember member : group.getMembers()) {
                if (!member.isDead() && member.isAttacked() && (!needLock || member.isLocked())
                        && member.getMemberInfo().hpPercent() <= this.config.minHealthToUseHealth) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shouldFocusHelpTank() {
        if (heroapi.getHealth().shieldPercent() <= this.config.minHealthToUseHealth) {
            return false;
        } else if (group.hasGroup()) {
            for (GroupMember member : group.getMembers()) {
                if (!member.isDead() && member.isAttacked() && member.getLocation().distanceTo(heroapi) < 1000) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shouldFocusShield() {
        if (heroapi.getHealth().shieldPercent() <= this.config.minHealthToUseHealth) {
            return true;
        } else if (group.hasGroup()) {
            for (GroupMember member : group.getMembers()) {
                if (!member.isDead() && member.isAttacked() && member.isLocked()
                        && member.getMemberInfo().shieldPercent() <= this.config.minHealthToUseHealth) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shouldUseOrcusAssimilate() {
        if (!isAvailable(Ability.ORCUS_ASSIMILATE)) {
            return false;
        }
        if (allNPCs == null || allNPCs.isEmpty()) {
            return false;
        }

        if (heroapi.getHealth().hpPercent() > this.config.minHealthToUseHealth) {
            return false;
        }

        Entity target = SharedFunctions.getAttacker(heroapi, allNPCs, heroapi);
        return target != null;
    }

    private boolean shouldFocusEvade() {
        if (allPlayers == null || allPlayers.isEmpty()) {
            return false;
        }
        Entity target = SharedFunctions.getAttacker(heroapi, allPlayers, heroapi);

        if (config.npcEnabled && target == null) {
            if (heroapi.getHealth().hpPercent() > this.config.minHealthToUseHealth) {
                return false;
            }

            target = SharedFunctions.getAttacker(heroapi, allNPCs, heroapi);
        }
        return target != null;
    }

    private boolean isAvailable(Ability ability) {
        return ability != null && config.supportedAbilities != null
                && config.supportedAbilities.stream().anyMatch(s -> s.name().equals(ability.name()))
                && items.getItem(ability, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.AVAILABLE).isPresent();
    }

    private boolean useSelectableReadyWhenReady(Ability selectableItem) {
        return selectableItem != null
                && items.useItem(selectableItem, 500, ItemFlag.USABLE, ItemFlag.READY).isSuccessful();
    }
}
