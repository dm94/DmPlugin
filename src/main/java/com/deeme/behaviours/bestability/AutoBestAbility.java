package com.deeme.behaviours.bestability;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.deeme.behaviours.ambulance.AmbulanceModule;
import com.deeme.types.ConditionsManagement;
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
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.GroupAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.utils.SafetyFinder;
import eu.darkbot.shared.utils.SafetyFinder.Escaping;

@Feature(name = "Auto Best Ability", description = "Auto use the best ability. Can use almost all abilities")
public class AutoBestAbility implements Behavior, Configurable<BestAbilityConfig> {

    private final BotAPI bot;
    private final HeroAPI heroapi;
    private final GroupAPI group;
    private final HeroItemsAPI items;
    private final SafetyFinder safety;
    private BestAbilityConfig config;
    private Collection<? extends Ship> allPlayers;
    private Collection<? extends Ship> allNPCs;
    private final ConditionsManagement conditionsManagement;

    private final List<Ability> DAMAGE_ABILITIES = Arrays.asList(Ability.SPEARHEAD_TARGET_MARKER, Ability.DIMINISHER,
            Ability.GOLIATH_X_FROZEN_CLAW, Ability.VENOM, Ability.TARTARUS_RAPID_FIRE,
            Ability.DISRUPTOR_SHIELD_DISARRAY, Ability.HECATE_PARTICLE_BEAM, Ability.KERES_SPR, Ability.ZEPHYR_TBR,
            Ability.HOLO_ENEMY_REVERSAL, Ability.TARTARUS_PLUS_RAPID_FIRE,
            Ability.BASILISK_HEIGHTENED_VALOUR, Ability.KERES_SPR, Ability.RETIARUS_CHS,
            Ability.TEMPEST_VOLT_DISCHARGE, Ability.HECATE_PLUS_PARTICLE_BEAM_PLUS, Ability.SPEARHEAD_PLUS_JAMX_CREED,
            Ability.SPEARHEAD_PLUS_NEUTRALIZING_MARKER);

    private final List<Ability> DAMAGE_RANGE_ABILITIES = Arrays.asList(Ability.SOLARIS_INC,
            Ability.SOLARIS_PLUS_INCINERATE_PLUS, Ability.BASILISK_NOXIOUS_NEBULA, Ability.TEMPEST_VOLTAGE_LINK);

    private final List<Ability> SPEED_ABILITIES = Arrays.asList(Ability.CITADEL_TRAVEL, Ability.LIGHTNING,
            Ability.KERES_SLE, Ability.MIMESIS_PHASE_OUT,
            Ability.ZEPHYR_MMT, Ability.PUSAT_PLUS_SPEED_SAP, Ability.RETIARUS_SPC);

    private final List<Ability> HEALTH_ABILITIES_WITHOUT_LOCK = Arrays.asList(Ability.AEGIS_REPAIR_POD,
            Ability.LIBERATOR_PLUS_SELF_REPAIR, Ability.SOLACE, Ability.SOLACE_PLUS_NANO_CLUSTER_REPAIRER_PLUS);

    private final List<Ability> HEALTH_ABILITIES_WITH_LOCK = Arrays.asList(Ability.AEGIS_HP_REPAIR,
            Ability.HAMMERCLAW_PLUS_REALLOCATE);

    private final List<Ability> EVADE_ABILITIES = Arrays.asList(Ability.SPEARHEAD_JAM_X,
            Ability.SPEARHEAD_ULTIMATE_CLOAK, Ability.BERSERKER_RVG, Ability.MIMESIS_SCRAMBLE,
            Ability.DISRUPTOR_DDOL, Ability.SPEARHEAD_PLUS_NEUTRALIZING_MARKER);

    private final List<Ability> EVADE_LAST_INSTANCE_ABILITIES = Arrays.asList(Ability.CITADEL_PLUS_PRISMATIC_ENDURANCE,
            Ability.CITADEL_FORTIFY, Ability.DISRUPTOR_REDIRECT, Ability.SPECTRUM,
            Ability.SPECTRUM_PLUS_PRISMATIC_REFLECTING,
            Ability.SENTINEL, Ability.BERSERKER_BSK);

    private final List<Ability> ALL_TIME_ABILITIES = Arrays.asList(Ability.SPEARHEAD_DOUBLE_MINIMAP);

    private final List<Ability> TARTARUS_SPEED_ABILITIES = Arrays.asList(Ability.TARTARUS_SPEED_BOOST,
            Ability.TARTARUS_PLUS_SPEED_BOOST);

    private long nextCheck = 0;

    private final int ABILITY_DISTANCE = 650;

    public AutoBestAbility(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class),
                api.requireAPI(BotAPI.class), api.requireAPI(HeroItemsAPI.class));
    }

    @Inject
    public AutoBestAbility(PluginAPI api, AuthAPI auth, BotAPI bot, HeroItemsAPI items) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog(api.requireAPI(ExtensionsAPI.class).getFeatureInfo(this.getClass()), auth.getAuthId());

        this.bot = bot;
        this.items = items;
        this.heroapi = api.requireAPI(HeroAPI.class);
        this.group = api.requireAPI(GroupAPI.class);
        this.safety = api.requireInstance(SafetyFinder.class);

        this.conditionsManagement = new ConditionsManagement(api, items);

        EntitiesAPI entities = api.requireAPI(EntitiesAPI.class);
        this.allPlayers = entities.getPlayers();
        this.allNPCs = entities.getNpcs();
        this.nextCheck = 0;
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
        if (this.nextCheck < System.currentTimeMillis()) {
            this.nextCheck = System.currentTimeMillis() + (this.config.timeToCheck * 1000);
            disableTartarusSpeedIfAttacking();
            this.conditionsManagement.useSelectableReadyWhenReady(getAbilityAlwaysToUse());
            this.conditionsManagement.useSelectableReadyWhenReady(getBestAbility());
        }
    }

    private Ability getAbilityAlwaysToUse() {
        if (this.config.abilitiesToUseEverytime == null) {
            return null;
        }

        try {
            return items.getItems(ItemCategory.SHIP_ABILITIES).stream()
                    .filter(Item::isReadyToUse)
                    .map(s -> s.getAs(Ability.class))
                    .filter(s -> s != null
                            && this.config.abilitiesToUseEverytime.stream()
                                    .anyMatch(a -> a != null && a.name().equals(s.name())))
                    .findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Ability getBestAbility() {
        return getHealthAbility()
                .orElse(getShieldAbility()
                        .orElse(getSpeedAbility()
                                .orElse(getSpeedAbility()
                                        .orElse(getEvadeAbility()
                                                .orElse(getTankAbility()
                                                        .orElse(getEvadeAbilityLastInstance()
                                                                .orElse(getDamageAbility()
                                                                        .orElse(getAllTimeAbility()
                                                                                .orElseGet(() -> {
                                                                                    if (shouldUseVoltBackup()) {
                                                                                        return Ability.TEMPEST_VOLT_BACKUP;
                                                                                    }
                                                                                    return null;
                                                                                })))))))));
    }

    private void disableTartarusSpeedIfAttacking() {
        if (heroapi.isAttacking() && heroapi.hasEffect(92)) {
            TARTARUS_SPEED_ABILITIES
                    .forEach(ability -> items.useItem(ability, 250, ItemFlag.USABLE));
        }
    }

    private Optional<Ability> getShieldAbility() {
        if (shouldFocusShield()) {
            if (isAvailable(Ability.AEGIS_SHIELD_REPAIR)) {
                return Optional.of(Ability.AEGIS_SHIELD_REPAIR);
            } else if (isAvailable(Ability.SENTINEL)) {
                return Optional.of(Ability.SENTINEL);
            }
        }

        return Optional.empty();
    }

    private Optional<Ability> getTankAbility() {
        if (shouldFocusHelpTank()) {
            if (isAvailable(Ability.CITADEL_DRAW_FIRE)) {
                return Optional.of(Ability.CITADEL_DRAW_FIRE);
            } else if (isAvailable(Ability.CITADEL_PROTECTION)) {
                return Optional.of(Ability.CITADEL_PROTECTION);
            }
        }

        return Optional.empty();
    }

    private Optional<Ability> getHealthAbility() {
        if (!shouldFocusHealth(false)) {
            return Optional.empty();
        }

        if (shouldFocusHealth(true)) {
            Optional<Ability> ability = getAbilityAvailableFromList(HEALTH_ABILITIES_WITH_LOCK);
            if (ability.isPresent()) {
                return ability;
            }
        }

        if (shouldFocusHealth(false)) {
            return getAbilityAvailableFromList(HEALTH_ABILITIES_WITHOUT_LOCK);
        }

        return Optional.empty();
    }

    private Optional<Ability> getSpeedAbility() {
        if (!shouldFocusSpeed()) {
            return Optional.empty();
        }

        if (!heroapi.hasEffect(92)) {
            Optional<Ability> tartarusAbility = getAbilityAvailableFromList(TARTARUS_SPEED_ABILITIES);
            if (tartarusAbility.isPresent()) {
                return tartarusAbility;
            }
        }

        return getAbilityAvailableFromList(SPEED_ABILITIES);
    }

    private Optional<Ability> getEvadeAbility() {
        if (!shouldFocusEvade()) {
            return Optional.empty();
        }

        return getAbilityAvailableFromList(EVADE_ABILITIES);
    }

    private Optional<Ability> getEvadeAbilityLastInstance() {
        if (!shouldFocusEvade()) {
            return Optional.empty();
        }

        if (shouldUseOrcusAssimilate()) {
            return Optional.of(Ability.ORCUS_ASSIMILATE);
        }

        return getAbilityAvailableFromList(EVADE_LAST_INSTANCE_ABILITIES);
    }

    private Optional<Ability> getAllTimeAbility() {
        if (!heroapi.isAttacking() && !heroapi.hasEffect(92)) {
            Optional<Ability> tartarusAbility = getAbilityAvailableFromList(TARTARUS_SPEED_ABILITIES);
            if (tartarusAbility.isPresent()) {
                return tartarusAbility;
            }
        }

        return getAbilityAvailableFromList(ALL_TIME_ABILITIES);
    }

    private Optional<Ability> getDamageAbility() {
        if (!shouldFocusDamage() || hasISH()) {
            return Optional.empty();
        }

        if (isInRange()) {
            Optional<Ability> rangeAbility = getAbilityAvailableFromList(DAMAGE_RANGE_ABILITIES);
            if (rangeAbility.isPresent()) {
                return rangeAbility;
            }
        }

        return getAbilityAvailableFromList(DAMAGE_ABILITIES);
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
            return (this.heroapi.isAttacking() && target.getHealth() != null
                    && target.getHealth().getHp() >= this.config.minHealthToUseDamage);
        }

        return false;
    }

    private Optional<Ability> getAbilityAvailableFromList(List<Ability> abilities) {
        return abilities.stream().filter(this::isAvailable).findFirst();
    }

    private boolean isInRange() {
        Lockable target = this.heroapi.getLocalTarget();
        return (target != null && target.isValid() && this.heroapi.distanceTo(target) < ABILITY_DISTANCE);
    }

    private boolean shouldFocusSpeed() {
        if (this.safety.state() == Escaping.ENEMY) {
            return true;
        }

        Lockable target = this.heroapi.getLocalTarget();

        if (target != null && target.isValid()) {
            if (!config.npcEnabled && this.heroapi.getLocalTarget() instanceof Npc) {
                return false;
            }

            double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
            return this.heroapi.distanceTo(target) >= ABILITY_DISTANCE || speed > this.heroapi.getSpeed();
        }
        return false;
    }

    private boolean shouldFocusHealth(boolean needLock) {
        if (this.bot.getModule() != null && this.bot.getModule().getClass() == AmbulanceModule.class) {
            return false;
        } else if (heroapi.hasEffect(EntityEffect.REPAIR_BOT)) {
            return false;
        } else if (this.heroapi.getHealth().hpPercent() <= this.config.minHealthToUseHealth) {
            return true;
        } else if (this.group.hasGroup()) {
            return this.group.getMembers().stream().anyMatch((member) -> !member.isDead() && member.isAttacked()
                    && member.getMemberInfo().hpPercent() <= this.config.minHealthToUseHealth
                    && (!needLock || (member.isLocked()
                            && this.heroapi.distanceTo(member.getLocation()) < ABILITY_DISTANCE)));
        }

        return false;
    }

    private boolean shouldUseVoltBackup() {
        return this.heroapi.getHealth().hpPercent() < 0.1;
    }

    private boolean shouldFocusHelpTank() {
        if (this.heroapi.getHealth().shieldPercent() <= this.config.minHealthToUseHealth) {
            return false;
        } else if (this.group.hasGroup()) {
            for (GroupMember member : this.group.getMembers()) {
                if (!member.isDead() && member.isAttacked() && member.getLocation().distanceTo(this.heroapi) < 1000) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shouldFocusShield() {
        if (this.heroapi.getHealth().shieldPercent() <= this.config.minHealthToUseHealth) {
            return true;
        } else if (this.group.hasGroup()) {
            for (GroupMember member : this.group.getMembers()) {
                if (!member.isDead() && member.isAttacked() && member.isLocked()
                        && member.getMemberInfo().shieldPercent() <= this.config.minHealthToUseHealth) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shouldUseOrcusAssimilate() {
        if (!isAvailable(Ability.ORCUS_ASSIMILATE) || this.allNPCs == null || this.allNPCs.isEmpty()) {
            return false;
        }

        if (this.heroapi.getHealth().hpPercent() > this.config.minHealthToUseHealth) {
            return false;
        }

        return SharedFunctions.getAttacker(this.heroapi, this.allNPCs, this.heroapi) != null;
    }

    private boolean shouldFocusEvade() {
        if (this.allPlayers == null || this.allPlayers.isEmpty()) {
            return false;
        }
        Entity target = SharedFunctions.getAttacker(this.heroapi, this.allPlayers, this.heroapi);

        if (this.config.npcEnabled && target == null) {
            if (heroapi.getHealth().hpPercent() > this.config.minHealthToUseHealth) {
                return false;
            }

            target = SharedFunctions.getAttacker(this.heroapi, this.allNPCs, this.heroapi);
        }
        return target != null;
    }

    private boolean isAvailable(Ability ability) {
        return ability != null && this.config.supportedAbilities != null
                && this.config.supportedAbilities.stream().anyMatch(s -> s.name().equals(ability.name()))
                && this.items
                        .getItem(ability, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.AVAILABLE, ItemFlag.NOT_SELECTED)
                        .isPresent();
    }
}
