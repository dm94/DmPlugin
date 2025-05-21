package com.deeme.behaviours.bestrocketlauncher;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import com.deeme.types.SharedFunctions;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.github.manolo8.darkbot.config.NpcExtraFlag;
import com.github.manolo8.darkbot.core.itf.NpcExtraProvider;
import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.items.Item;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.RocketLauncher;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.utils.Inject;
import com.deeme.shared.AmmoConditions;

@Feature(name = "Auto Best Rocket Launcher", description = "Auto use the best Rocket Launcher")
public class AutoBestRocketLauncherDummy
        implements Behavior, Configurable<BestRocketLauncherConfig>, NpcExtraProvider {
    private final HeroAPI heroapi;
    private final HeroItemsAPI items;
    private BestRocketLauncherConfig config;

    private AmmoConditions ammoConditions;

    private static final int URB_100_DAMAGE_X2 = 144000;
    private static final int MIN_ROCKET_AMMO = 5;

    public AutoBestRocketLauncherDummy(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class), api.requireAPI(HeroItemsAPI.class));
    }

    @Inject
    public AutoBestRocketLauncherDummy(PluginAPI api, AuthAPI auth, HeroItemsAPI items) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        VerifierChecker.requireAuthenticity(auth);
        ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
        Utils.discordCheck(extensionsAPI.getFeatureInfo(this.getClass()), auth.getAuthId());

        this.items = items;
        this.heroapi = api.requireAPI(HeroAPI.class);
        this.ammoConditions = new AmmoConditions(api, heroapi);
    }

    @Override
    public NpcExtraFlag[] values() {
        return ExtraNpcFlagsEnum.values();
    }


    @Override
    public void setConfig(ConfigSetting<BestRocketLauncherConfig> arg0) {
        this.config = arg0.getValue();
    }


    @Override
    public void onTickBehavior() {
        tick();
    }

    @Override
    public void onStoppedBehavior() {
        if (hasOption(BehaviourOptionsEnum.TICK_STOPPED)) {
            tick();
        }
    }

    private void tick() {
        if (!config.enable) {
            return;
        }
        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid() && heroapi.isAttacking()) {
            boolean isNpc = target instanceof Npc;
            if (hasTag(target, ExtraNpcFlagsEnum.BEST_AMMO)
                    || (!isNpc && hasOption(BehaviourOptionsEnum.VS_PLAYERS))
                    || (isNpc && hasOption(BehaviourOptionsEnum.ALWAYS_FOR_NPC))
                    || heroapi.getHealth().hpPercent() <= config.alwaysUseBellowHp) {
                changeRocketLauncher(getBestRocketLauncher(target, isNpc));
                return;
            }
        }
        changeRocketLauncher(SharedFunctions.getItemById(config.defaultRocket));
    }

    private boolean changeRocketLauncher(SelectableItem rocket) {
        if (rocket == null) {
            return false;
        }

        return items.useItem(rocket, 0, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.NOT_SELECTED)
                .isSuccessful();
    }

    private SelectableItem getBestRocketLauncher(Lockable target, boolean isNpc) {
        if (ableToUseInfectionAmmo(isNpc)) {
            return RocketLauncher.PIR_100;
        }

        SelectableItem sabRocket = getBestSabRocket(isNpc);
        if (sabRocket != null) {
            return sabRocket;
        }

        SelectableItem damageRocket = getDamageRocket(target, isNpc);
        if (damageRocket != null) {
            return damageRocket;
        }

        return SharedFunctions.getItemById(config.defaultRocket);
    }

    private SelectableItem getBestSabRocket(boolean isNpc) {
        if (!this.ammoConditions.ableToUseSAB())
            return null;
        if (ableToUse(RocketLauncher.CBR, isNpc)) {
            return RocketLauncher.CBR;
        }
        if (ableToUse(RocketLauncher.SAR_02, isNpc)) {
            return RocketLauncher.SAR_02;
        }
        if (ableToUse(RocketLauncher.SAR_01, isNpc)) {
            return RocketLauncher.SAR_01;
        }
        return null;
    }

    private SelectableItem getDamageRocket(Lockable target, boolean isNpc) {
        if (target != null && target.isValid()) {
            if (target.getHealth().getHp() > URB_100_DAMAGE_X2
                    && ableToUse(RocketLauncher.UBR_100, isNpc)) {
                return RocketLauncher.UBR_100;
            }
            if (ableToUse(RocketLauncher.HSTRM_01, isNpc)) {
                return RocketLauncher.HSTRM_01;
            }
            if (ableToUse(RocketLauncher.BDR1212, isNpc)) {
                return RocketLauncher.BDR1212;
            }
            if (ableToUse(RocketLauncher.ECO_10, isNpc)) {
                return RocketLauncher.ECO_10;
            }
        }
        return null;
    }

    private boolean ableToUseInfectionAmmo(boolean isNpc) {
        if (!ableToUse(RocketLauncher.PIR_100, isNpc)) {
            return false;
        }

        return this.ammoConditions.ableToUseInfectionAmmo();
    }

    private boolean hasOption(BehaviourOptionsEnum option) {
        return config.options.stream().anyMatch(s -> s.name().equals(option.name()));
    }

    private boolean ableToUse(SelectableItem rocket, boolean isNpc) {
        Set<RocketLauncher> ammoToUse =
                isNpc ? config.rocketsToUseNPCs : config.rocketsToUsePlayers;

        if (ammoToUse.stream()
                .noneMatch(s -> s.getId() != null && s.getId().equals(rocket.getId()))) {
            return false;
        }

        Optional<Item> item = items.getItem(rocket, ItemFlag.USABLE, ItemFlag.POSITIVE_QUANTITY);
        return item.isPresent() && item.get().getQuantity() > MIN_ROCKET_AMMO;
    }

    private boolean hasTag(Lockable target, Enum<?> tag) {
        return (target != null && target.isValid() && target instanceof Npc
                && ((Npc) target).getInfo().hasExtraFlag(tag));
    }
}
