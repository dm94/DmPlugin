package com.deeme.behaviours.bestrocket;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.deeme.types.SharedFunctions;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.PercentRange;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.enums.EntityEffect;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Rocket;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "Auto Best Rocket", description = "Automatically switches rockets. Will use all available rockets")
public class AutoBestRocket implements Behavior, Configurable<BestRocketConfig> {

    protected final PluginAPI api;
    protected final BotAPI bot;
    protected final HeroAPI heroapi;
    protected final MovementAPI movement;
    private final HeroItemsAPI items;
    protected final ConfigSetting<PercentRange> repairHpRange;
    private BestRocketConfig config;

    List<SelectableItem> damageOrder = Arrays.asList(Rocket.SP_100X, Rocket.PLT_3030, Rocket.PLT_2021,
            Rocket.BDR_1211,
            Rocket.PLT_2026,
            Rocket.R_310);
    List<SelectableItem> damageOrderNPCs = Arrays.asList(Rocket.BDR_1211, Rocket.SP_100X,
            Rocket.PLT_3030,
            Rocket.PLT_2021,
            Rocket.PLT_2026,
            Rocket.R_310);

    public AutoBestRocket(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class),
                api.requireAPI(BotAPI.class), api.requireAPI(HeroItemsAPI.class));
    }

    @Inject
    public AutoBestRocket(PluginAPI api, AuthAPI auth, BotAPI bot, HeroItemsAPI items) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog(auth.getAuthId());

        this.api = api;
        this.bot = bot;
        this.items = items;
        this.heroapi = api.getAPI(HeroAPI.class);
        this.movement = api.getAPI(MovementAPI.class);

        ConfigAPI configAPI = api.getAPI(ConfigAPI.class);
        this.repairHpRange = configAPI.requireConfig("general.safety.repair_hp_range");
    }

    @Override
    public void setConfig(ConfigSetting<BestRocketConfig> arg0) {
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
        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid() && heroapi.isAttacking(target)) {
            changeRocket(getBestRocket(target, target instanceof Npc));
        }
    }

    private void changeRocket(SelectableItem rocket) {
        if (rocket == null) {
            return;
        }
        items.useItem(rocket, 500, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.NOT_SELECTED);
    }

    private SelectableItem getBestRocket(Lockable target, boolean isNpc) {
        if (!hasISH()) {
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
            if (ableToUse(Rocket.PLD_8, isNpc) && shouldUsePLD(target)) {
                return Rocket.PLD_8;
            }

            if (ableToUse(Rocket.AGT_500, isNpc) && shouldUseAGT()) {
                return Rocket.AGT_500;
            }
        }

        return getBestRocketByDamage(isNpc);
    }

    private SelectableItem getBestRocketByDamage(boolean isNpc) {
        return (isNpc ? damageOrderNPCs : damageOrder).stream()
                .filter(rocket -> ableToUse(rocket, isNpc))
                .min(Comparator.comparing(i -> damageOrder.indexOf(i)))
                .orElse(SharedFunctions.getItemById(config.npcRocket));
    }

    private boolean shouldUseAGT() {
        Lockable target = heroapi.getLocalTarget();
        return target != null && target.isValid() && heroapi.distanceTo(target) <= 500;
    }

    private boolean hasISH() {
        Lockable target = heroapi.getLocalTarget();
        return target != null && target.isValid() && (target.hasEffect(EntityEffect.ISH)
                || target.hasEffect(EntityEffect.NPC_ISH) || target.hasEffect(EntityEffect.PET_SPAWN));
    }

    private boolean shouldFocusSpeed(Lockable target) {
        double distance = heroapi.getLocationInfo().getCurrent().distanceTo(target.getLocationInfo());
        double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;

        return (distance > 400 && speed > heroapi.getSpeed())
                || (distance < 600 && speed > heroapi.getSpeed()
                        && heroapi.getHealth().hpPercent() < repairHpRange.getValue().getMin());
    }

    private boolean shouldUsePLD(Lockable target) {
        return target instanceof Movable && ((Movable) target).isAiming(heroapi)
                && heroapi.getHealth().hpPercent() < 0.5;
    }

    private boolean ableToUse(SelectableItem rocket, boolean isNpc) {
        if (isNpc) {
            return ableToUse(rocket, config.rocketsToUseNPCs);
        }

        return ableToUse(rocket, config.rocketsToUsePlayers);
    }

    private boolean ableToUse(SelectableItem rocket, Set<SupportedRockets> rockets) {
        return rocket != null && rockets.stream().anyMatch(s -> s.getId() != null && s.getId().equals(rocket.getId()))
                && items.getItem(rocket, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.AVAILABLE,
                        ItemFlag.POSITIVE_QUANTITY).isPresent();
    }
}
