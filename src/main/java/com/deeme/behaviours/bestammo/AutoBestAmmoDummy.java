package com.deeme.behaviours.bestammo;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.github.manolo8.darkbot.config.NpcExtraFlag;
import com.github.manolo8.darkbot.core.itf.NpcExtraProvider;
import com.github.manolo8.darkbot.core.objects.facades.SettingsProxy;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.NpcFlag;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.utils.Inject;

import java.util.Set;
import java.util.Map;

import com.deeme.shared.AmmoConditions;
import com.deeme.types.SharedFunctions;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;

import eu.darkbot.api.config.types.NpcInfo;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.items.Item;

@Feature(name = "Auto Best Ammo", description = "Auto use the best ammo")
public class AutoBestAmmoDummy implements Behavior, Configurable<BestAmmoConfig>, NpcExtraProvider {
    private final HeroAPI heroapi;
    private final ConfigAPI configApi;
    private final HeroItemsAPI items;
    private BestAmmoConfig config;
    private AmmoConditions ammoConditions;
    private final ConfigSetting<Map<String, NpcInfo>> npcInfos;
    private Character attackLaserKey;

    private static final int MAX_RANGE = 730;
    private static final int MIN_AMMO = 200;
    private static final int PROMETHEUS_EFFECT_ID = 98;
    private static final double LOW_HP_THRESHOLD = 0.5; // 50% HP threshold

    List<Laser> damageOrder = Arrays.asList(Laser.RCB_140, Laser.RSB_75, Laser.IDB_125, Laser.CC_A, Laser.CC_B,
            Laser.CC_C, Laser.CC_D,
            Laser.CC_E, Laser.CC_F, Laser.CC_G, Laser.CC_H,
            Laser.UCB_100, Laser.SBL_100, Laser.A_BL, Laser.EMAA_20,
            Laser.VB_142,
            Laser.MCB_50, Laser.MCB_25, Laser.LCB_10);

    List<Laser> damageOrderNPC = Arrays.asList(Laser.RCB_140, Laser.RSB_75, Laser.UCB_100,
            Laser.JOB_100, Laser.MCB_50, Laser.MCB_25, Laser.LCB_10);

    public AutoBestAmmoDummy(PluginAPI api) throws SecurityException {
        this(api, api.requireAPI(AuthAPI.class), api.requireAPI(HeroItemsAPI.class));
    }

    @Inject
    public AutoBestAmmoDummy(PluginAPI api, AuthAPI auth, HeroItemsAPI items) throws SecurityException {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        VerifierChecker.requireAuthenticity(auth);
        ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
        Utils.discordCheck(extensionsAPI.getFeatureInfo(this.getClass()), auth.getAuthId());

        this.items = items;
        this.heroapi = api.requireAPI(HeroAPI.class);
        this.ammoConditions = new AmmoConditions(api, heroapi);

        this.configApi = api.requireAPI(ConfigAPI.class);
        this.npcInfos = configApi.requireConfig("loot.npc_infos");
        SettingsProxy settingsProxy = api.requireInstance(SettingsProxy.class);
        this.attackLaserKey = settingsProxy.getCharacterOf(SettingsProxy.KeyBind.ATTACK_LASER).orElse(null);
    }

    @Override
    public NpcExtraFlag[] values() {
        return ExtraNpcFlagsEnum.values();
    }

    @Override
    public void setConfig(ConfigSetting<BestAmmoConfig> arg0) {
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
        if (target != null && target.isValid() && heroapi.isAttacking(target)
                && heroapi.distanceTo(target) < MAX_RANGE) {
            boolean isNpc = target instanceof Npc;
            if (hasTag(ExtraNpcFlagsEnum.BEST_AMMO) || (!isNpc && hasOption(BehaviourOptionsEnum.VS_PLAYERS))
                    || (isNpc && hasOption(BehaviourOptionsEnum.ALWAYS_FOR_NPC))
                    || heroapi.getHealth().hpPercent() <= LOW_HP_THRESHOLD) {
                changeLaser(getBestLaserAmmo(isNpc));
            } else {
                changeLaser(getDefaultLaser());
            }
        }
    }

    private void changeLaser(SelectableItem laser) {
        if (laser == null) {
            return;
        }

        if (items.getItem(laser, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.NOT_SELECTED, ItemFlag.POSITIVE_QUANTITY)
                .isPresent()) {
            if (hasOption(BehaviourOptionsEnum.CHANGE_AMMO_DIRECTLY)) {
                items.useItem(laser, ItemFlag.NOT_SELECTED);
            }

            if (hasOption(BehaviourOptionsEnum.REPLACE_AMMO_KEY)) {
                changeAmmoKey(laser);
            }
        }
    }

    private void changeAmmoKey(SelectableItem laser) {
        ConfigSetting<Character> ammoKey = configApi.requireConfig("loot.ammo_key");
        Character key = items.getKeyBind(laser);
        if (key == null) {
            ammoKey.setValue(attackLaserKey);
        } else if (ammoKey.getValue() == null || !ammoKey.getValue().equals(key)) {
            ammoKey.setValue(key);
        }
    }

    private boolean noPassPrometheusCheck() {
        return hasOption(BehaviourOptionsEnum.ONLY_PROMETHEUS) && !heroapi.hasEffect(PROMETHEUS_EFFECT_ID);
    }

    private SelectableItem getBestLaserAmmo(boolean isNpc) {
        if (noPassPrometheusCheck()) {
            return getDefaultLaser();
        }

        boolean useRsb = ableToUseRSB(isNpc);
        if (useRsb) {
            if (isNpc ? ableToUseNPCs(Laser.RSB_75) : ableToUsePlayers(Laser.RSB_75)) {
                return Laser.RSB_75;
            }
            if (isNpc ? ableToUseNPCs(Laser.RCB_140) : ableToUsePlayers(Laser.RCB_140)) {
                return Laser.RCB_140;
            }
        }

        if (ableToUseInfectionAmmo(isNpc)) {
            return Laser.PIB_100;
        }

        if (this.ammoConditions.ableToUseSAB()) {
            if (isNpc ? ableToUseNPCs(Laser.CBO_100) : ableToUsePlayers(Laser.CBO_100)) {
                return Laser.CBO_100;
            }
            if (isNpc ? ableToUseNPCs(Laser.SAB_50) : ableToUsePlayers(Laser.SAB_50)) {
                return Laser.SAB_50;
            }
        }

        SelectableItem bestLaser = isNpc ? getBestLaserForNpc() : getBestLaserAmmoByDamage(isNpc);
        if (bestLaser != null) {
            return bestLaser;
        }

        return getDefaultLaser();
    }

    private Optional<SelectableItem.Laser> getConfiguredAmmoForNpc(String npcName) {
        if (!hasOption(BehaviourOptionsEnum.RESPECT_NPC_AMMO)) {
            return Optional.empty();
        }

        Map<String, NpcInfo> npcs = npcInfos.getValue();

        NpcInfo info = npcs.getOrDefault(npcName, null);

        return info == null ? Optional.empty() : info.getAmmo();
    }

    private SelectableItem getDefaultLaser() {
        return SharedFunctions.getItemById(config.defaultLaser);
    }

    private SelectableItem getBestLaserForNpc() {
        Lockable target = heroapi.getLocalTarget();
        String name = target.getEntityInfo().getUsername();

        if (noPassPrometheusCheck()) {
            if (hasOption(BehaviourOptionsEnum.RESPECT_NPC_AMMO)) {
                return getConfiguredAmmoForNpc(name).orElse((Laser) getDefaultLaser());
            } else {
                return getDefaultLaser();
            }
        }

        if ((name.contains("Lanatum") || name.contains("Styxus") || name.contains("Charopos"))
                && ableToUseNPCs(Laser.VB_142)) {
            return Laser.VB_142;
        } else if ((name.contains("Invoke XVI") || name.contains("Mindfire Behemoth"))
                && ableToUseNPCs(Laser.A_BL)) {
            return Laser.A_BL;
        } else if (isMimesisNPC(name)
                && ableToUseNPCs(Laser.EMAA_20)) {
            return Laser.EMAA_20;
        } else if (name.toLowerCase().contains("demaner") && ableToUseNPCs(Laser.RB_214)) {
            return Laser.RB_214;
        } else if (name.toLowerCase().contains("sibelon") && ableToUseNPCs(Laser.SBL_100)) {
            return Laser.SBL_100;
        }

        return getBestLaserAmmoByDamage(true);
    }

    private boolean isMimesisNPC(String name) {
        return name.contains("Mimesis") || name.contains("Mimes1s") || name.contains("Mimesi5")
                || name.contains("Mime5is") || name.contains("Mim3sis") || name.contains("M1mesis")
                || name.contains("M1mesi5") || name.contains("Mim3si5")
                || name.matches("(M|m)(i|1)(m|M)(e|3)(s|5)(i|1)(s|5)");
    }

    private boolean ableToUseInfectionAmmo(boolean isNpc) {
        if (isNpc ? !ableToUseNPCs(Laser.PIB_100) : !ableToUsePlayers(Laser.PIB_100)) {
            return false;
        }

        return this.ammoConditions.ableToUseInfectionAmmo();
    }

    private boolean ableToUseRSB(boolean isNpc) {
        if (this.ammoConditions.hasISH()) {
            return false;
        }

        boolean useRsb = true;
        if ((hasOption(BehaviourOptionsEnum.RESPECT_RSB_TAG) && isNpc)) {
            useRsb = hasTag(NpcFlag.USE_RSB);
        }

        return useRsb;
    }

    private SelectableItem getBestLaserAmmoByDamage(boolean isNpc) {
        boolean useRsb = ableToUseRSB(isNpc);

        try {
            if (isNpc) {
                return damageOrderNPC.stream()
                        .filter(p -> useRsb || (p != Laser.RCB_140 && p != Laser.RSB_75))
                        .filter(this::ableToUseNPCs)
                        .findFirst().orElse(null);
            } else {
                return damageOrder.stream()
                        .filter(p -> useRsb || (p != Laser.RCB_140 && p != Laser.RSB_75))
                        .filter(this::ableToUsePlayers)
                        .findFirst().orElse(null);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasOption(BehaviourOptionsEnum option) {
        return config.optionsToUse.stream().anyMatch(s -> s.name().equals(option.name()));
    }

    private boolean ableToUseNPCs(SelectableItem laser) {
        return ableToUse(laser, config.ammoToUseNpcs);
    }

    private boolean ableToUsePlayers(SelectableItem laser) {
        return ableToUse(laser, config.ammoToUsePlayers);
    }

    private boolean ableToUse(SelectableItem laser, Set<Laser> ammoToUse) {
        if (ammoToUse.stream().noneMatch(s -> s.getId() != null && s.getId().equals(laser.getId()))) {
            return false;
        }

        Optional<Item> item = items.getItem(laser, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY);
        return item.isPresent() && item.get().getQuantity() >= MIN_AMMO;
    }

    private boolean hasTag(Enum<?> tag) {
        Lockable target = heroapi.getLocalTarget();
        return (target != null && target.isValid() && target instanceof Npc
                && ((Npc) target).getInfo().hasExtraFlag(tag));
    }

}