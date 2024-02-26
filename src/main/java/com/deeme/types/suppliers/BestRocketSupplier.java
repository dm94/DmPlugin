package com.deeme.types.suppliers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.deeme.behaviours.bestrocket.BestRocketConfig;
import com.deeme.behaviours.bestrocket.SupportedRockets;
import com.deeme.types.SharedFunctions;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.PercentRange;
import eu.darkbot.api.game.enums.EntityEffect;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Rocket;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;

public class BestRocketSupplier {
    private final HeroAPI heroapi;
    private final HeroItemsAPI items;
    private final ConfigSetting<PercentRange> repairHpRange;
    private BestRocketConfig config;
    private String defaultRocket = "";

    private static final int SR5_MIN_SHIELD = 500000;

    List<SelectableItem> damageOrder = Arrays.asList(Rocket.SP_100X, Rocket.PLT_3030, Rocket.PLT_2021,
            Rocket.BDR_1211,
            Rocket.PLT_2026,
            Rocket.R_310);

    List<SelectableItem> damageOrderNPCs = Arrays.asList(Rocket.BDR_1211, Rocket.SP_100X,
            Rocket.PLT_3030,
            Rocket.PLT_2021,
            Rocket.PLT_2026,
            Rocket.R_310);

    public BestRocketSupplier(PluginAPI api, String defaultRocket) {
        this(api);
        this.defaultRocket = defaultRocket;
    }

    public BestRocketSupplier(PluginAPI api) {
        this.items = api.requireAPI(HeroItemsAPI.class);
        this.heroapi = api.requireAPI(HeroAPI.class);

        ConfigAPI configAPI = api.requireAPI(ConfigAPI.class);
        this.repairHpRange = configAPI.requireConfig("general.safety.repair_hp_range");
    }

    public void setConfig(ConfigSetting<BestRocketConfig> arg0) {
        this.config = arg0.getValue();
    }

    public SelectableItem getBestRocket(Lockable target, boolean isNpc) {
        if (target == null || !target.isValid() || hasISH(target)) {
            return getDefaultRocket();
        }

        if (shouldFocusSpeed(target)) {
            if (ableToUse(Rocket.R_IC3, isNpc)) {
                return Rocket.R_IC3;
            } else if (ableToUse(Rocket.DCR_250, isNpc)) {
                return Rocket.DCR_250;
            } else if (ableToUse(Rocket.K_300M, isNpc)) {
                return Rocket.K_300M;
            } else if (ableToUse(Rocket.RC_100, isNpc)) {
                return Rocket.RC_100;
            }
        }

        if (shouldUseSR5(target, isNpc)) {
            return Rocket.SR_5;
        }

        if (shouldUsePLD(target, isNpc)) {
            return Rocket.PLD_8;
        }

        if (shouldUseAGT(target, isNpc)) {
            return Rocket.AGT_500;
        }

        return getBestRocketByDamage(isNpc);
    }

    public SelectableItem getWorstRocket(boolean isNpc) {
        return (isNpc ? damageOrderNPCs : damageOrder).stream()
                .filter(rocket -> ableToUse(rocket, isNpc))
                .max(Comparator.comparing(i -> damageOrder.indexOf(i)))
                .orElse(getDefaultRocket());
    }

    private SelectableItem getBestRocketByDamage(boolean isNpc) {
        return (isNpc ? damageOrderNPCs : damageOrder).stream()
                .filter(rocket -> ableToUse(rocket, isNpc))
                .min(Comparator.comparing(i -> damageOrder.indexOf(i)))
                .orElse(getDefaultRocket());
    }

    private boolean shouldUseAGT(Lockable target, boolean isNpc) {
        if (!ableToUse(Rocket.AGT_500, isNpc)) {
            return false;
        }

        return heroapi.distanceTo(target) <= 500;
    }

    private boolean hasISH(Lockable target) {
        return target.hasEffect(EntityEffect.ISH)
                || target.hasEffect(EntityEffect.NPC_ISH) || target.hasEffect(EntityEffect.PET_SPAWN);
    }

    private boolean shouldFocusSpeed(Lockable target) {
        double distance = heroapi.getLocationInfo().getCurrent().distanceTo(target.getLocationInfo());
        double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;

        return (distance > 400 && speed > heroapi.getSpeed())
                || (distance < 600 && speed > heroapi.getSpeed()
                        && heroapi.getHealth().hpPercent() < repairHpRange.getValue().getMin());
    }

    private boolean shouldUsePLD(Lockable target, boolean isNpc) {
        if (!ableToUse(Rocket.PLD_8, isNpc)) {
            return false;
        }

        return target instanceof Movable && ((Movable) target).isAiming(heroapi)
                && heroapi.getHealth().hpPercent() < 0.5;
    }

    private boolean shouldUseSR5(Lockable target, boolean isNpc) {
        if (!ableToUse(Rocket.SR_5, isNpc)) {
            return false;
        }

        return target.getHealth().getShield() > SR5_MIN_SHIELD;
    }

    private boolean ableToUse(SelectableItem rocket, boolean isNpc) {
        if (config == null) {
            return ableToUse(rocket);
        }

        if (isNpc) {
            return ableToUse(rocket, config.rocketsToUseNPCs);
        }

        return ableToUse(rocket, config.rocketsToUsePlayers);
    }

    private boolean ableToUse(SelectableItem rocket, Set<SupportedRockets> rockets) {
        return rocket != null && rockets.stream().anyMatch(s -> s.getId() != null && s.getId().equals(rocket.getId()))
                && ableToUse(rocket);
    }

    private boolean ableToUse(SelectableItem rocket) {
        return rocket != null
                && items.getItem(rocket, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.AVAILABLE,
                        ItemFlag.POSITIVE_QUANTITY).isPresent();
    }

    private SelectableItem getDefaultRocket() {
        return SharedFunctions.getItemById(config != null ? config.npcRocket : defaultRocket);
    }
}
