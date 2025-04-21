package com.deeme.shared;

import com.github.manolo8.darkbot.config.Config.Loot.Sab;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.NpcFlag;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.enums.EntityEffect;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.HeroAPI;

public class AmmoConditions {
    private final HeroAPI heroapi;
    private final PluginAPI api;

    private static final long MIN_HEALTH = 100000;
    private static final long MIN_SAB_SHIELD = 150000;
    private static final long MIN_HERO_SHIELD = 150000;
    private static final long MIN_HERO_HP = 60000;
    private static final double MIN_HERO_HP_PERCENT = 0.5;
    private static final double MIN_SHIELD_PERCENT_THRESHOLD = 0.5;

    private final ConfigSetting<Sab> sabSettings;

    public AmmoConditions(PluginAPI api, HeroAPI heroAPI) {
        this.api = api;
        this.heroapi = heroAPI;

        ConfigAPI configApi = api.getAPI(ConfigAPI.class);
        this.sabSettings = configApi.requireConfig("loot.sab");
    }

    public boolean ableToUseInfectionAmmo() {
        if (hasISH()) {
            return false;
        }

        Lockable target = heroapi.getLocalTarget();
        double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
        return target != null && target.isValid() && !target.hasEffect(EntityEffect.INFECTION)
                && ((target.getHealth() != null
                        && target.getHealth().getHp() >= MIN_HEALTH) || heroapi.getSpeed() <= speed);
    }

    public boolean hasISH() {
        Lockable target = heroapi.getLocalTarget();
        return target != null && target.isValid() && (target.hasEffect(EntityEffect.ISH)
                || target.hasEffect(EntityEffect.NPC_ISH) || target.hasEffect(EntityEffect.PET_SPAWN));
    }

    public boolean ableToUseSAB() {
        if (hasTag(NpcFlag.NO_SAB)) {
            return false;
        }

        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            if (target.getHealth().getShield() > MIN_SAB_SHIELD
                    && heroapi.getHealth().shieldPercent() < MIN_SHIELD_PERCENT_THRESHOLD
                    && heroapi.getHealth().getMaxShield() > MIN_HERO_SHIELD && heroapi.getHealth().getHp() < MIN_HERO_HP
                    && heroapi.getHealth().hpPercent() < MIN_HERO_HP_PERCENT) {
                return true;
            }

            if (target instanceof Npc) {
                Sab sab = sabSettings.getValue();
                return !(!sab.ENABLED
                        || heroapi.getHealth().shieldPercent() > sab.PERCENT
                        || target.getHealth().getShield() <= sab.NPC_AMOUNT
                        || (sab.CONDITION != null && !sab.CONDITION.get(api).toBoolean()));
            }
        }

        return false;
    }

    private boolean hasTag(Enum<?> tag) {
        Lockable target = heroapi.getLocalTarget();
        return (target != null && target.isValid() && target instanceof Npc
                && ((Npc) target).getInfo().hasExtraFlag(tag));
    }
}
