
package com.deeme.behaviours;

import java.util.Arrays;

import com.deeme.modules.temporal.AmbulanceModule;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deeme.types.config.AmbulanceConfig;
import com.github.manolo8.darkbot.extensions.util.Version;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.group.GroupMember;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem.Ability;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.GroupAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "Ambulance Mode", description = "Turn your ship into an ambulance")
public class AmbulanceMode implements Behavior, Configurable<AmbulanceConfig> {

    protected final PluginAPI api;
    protected final HeroAPI heroapi;
    protected final GroupAPI group;
    protected final BotAPI botApi;
    protected final HeroItemsAPI items;

    private AmbulanceConfig config;
    private long nextCheck = 0;

    public AmbulanceMode(PluginAPI api) {
        this(api, api.requireAPI(HeroAPI.class),
                api.requireAPI(AuthAPI.class),
                api.requireAPI(GroupAPI.class));
    }

    @Inject
    public AmbulanceMode(PluginAPI api, HeroAPI hero, AuthAPI auth, GroupAPI groupAPI) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        if (!Utils.discordCheck(auth.getAuthId())) {
            Utils.showDiscordDialog();
            ExtensionsAPI extensionsAPI = api.getAPI(ExtensionsAPI.class);
            extensionsAPI.getFeatureInfo(this.getClass())
                    .addFailure("To use this option you need to be on my discord", "Log in to my discord and reload");
        }

        this.api = api;
        this.heroapi = hero;
        this.group = groupAPI;
        this.botApi = api.getAPI(BotAPI.class);
        this.items = api.getAPI(HeroItemsAPI.class);
    }

    @Override
    public void setConfig(ConfigSetting<AmbulanceConfig> arg0) {
        this.config = arg0.getValue();
    }

    @Override
    public void onTickBehavior() {
        if (config.enable && nextCheck <= System.currentTimeMillis()) {
            nextCheck = System.currentTimeMillis() + (config.timeToCheck * 1000);
            int memberToHelp = getMemberLowLife();
            if (memberToHelp != 0) {
                Ability ability = getAbility();
                if (ability != null) {
                    if (config.shipType == 2) {
                        items.useItem(ability);
                    } else if (botApi.getModule().getClass() != AmbulanceModule.class) {
                        botApi.setModule(new AmbulanceModule(api, memberToHelp, ability));
                    }
                }
            } else if (config.repairShield) {
                memberToHelp = getMemberLowShield();
                if (memberToHelp != 0
                        && items.getItem(Ability.AEGIS_SHIELD_REPAIR, ItemFlag.USABLE, ItemFlag.READY,
                                ItemFlag.AVAILABLE).isPresent()) {
                    Ability ability = Ability.AEGIS_SHIELD_REPAIR;
                    if (botApi.getModule().getClass() != AmbulanceModule.class) {
                        botApi.setModule(new AmbulanceModule(api, memberToHelp, ability));
                    }
                }
            }
        }
    }

    private int getMemberLowLife() {
        if (group.hasGroup()) {
            for (GroupMember member : group.getMembers()) {
                if (!member.isDead() && member.isAttacked() && member.getMapId() == heroapi.getMap().getId()
                        && member.getMemberInfo().getHp() > 1
                        && member.getMemberInfo().hpPercent() < config.healthToRepair) {
                    return member.getId();
                }
            }
        }
        return 0;
    }

    private int getMemberLowShield() {
        if (group.hasGroup()) {
            for (GroupMember member : group.getMembers()) {
                if (!member.isDead() && member.isAttacked() && member.getMapId() == heroapi.getMap().getId()
                        && member.getMemberInfo().getMaxShield() > 1000
                        && member.getMemberInfo().shieldPercent() < config.healthToRepair) {
                    return member.getId();
                }
            }
        }
        return 0;
    }

    public Ability getAbility() {
        if (config.shipType == 0 || config.shipType == 1) {
            if (items.getItem(Ability.AEGIS_HP_REPAIR, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.AVAILABLE)
                    .isPresent()) {
                return Ability.AEGIS_HP_REPAIR;
            }
            if (items.getItem(Ability.AEGIS_REPAIR_POD, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.AVAILABLE)
                    .isPresent()) {
                return Ability.AEGIS_REPAIR_POD;
            }
        } else if (config.shipType == 2) {
            if (items.getItem(Ability.SOLACE, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.AVAILABLE).isPresent()) {
                return Ability.SOLACE;
            }
            if (botApi.getVersion().compareTo(new Version("1.13.17 beta 109 alpha 14")) > 1
                    && items.getItem(Ability.SOLACE_PLUS_NANO_CLUSTER_REPAIRER_PLUS, ItemFlag.USABLE, ItemFlag.READY,
                            ItemFlag.AVAILABLE)
                            .isPresent()) {
                return Ability.SOLACE_PLUS_NANO_CLUSTER_REPAIRER_PLUS;

            }
        }
        return null;
    }

}
