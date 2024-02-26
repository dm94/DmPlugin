
package com.deeme.behaviours;

import java.util.Arrays;

import com.deeme.behaviours.ambulance.AmbulanceConfig;
import com.deeme.behaviours.ambulance.AmbulanceModule;
import com.deeme.types.ConditionsManagement;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;

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

@Feature(name = "Ambulance Mode", description = "Turn your ship into an ambulance for the members of your group.")
public class AmbulanceMode implements Behavior, Configurable<AmbulanceConfig> {

    private final PluginAPI api;
    private final HeroAPI heroapi;
    private final GroupAPI group;
    private final BotAPI botApi;
    private final HeroItemsAPI items;
    private final ConditionsManagement conditionsManagement;

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
        VerifierChecker.requireAuthenticity(auth);

        Utils.showDonateDialog(api.requireAPI(ExtensionsAPI.class).getFeatureInfo(this.getClass()), auth.getAuthId());

        this.api = api;
        this.heroapi = hero;
        this.group = groupAPI;
        this.botApi = api.requireAPI(BotAPI.class);
        this.items = api.requireAPI(HeroItemsAPI.class);
        this.conditionsManagement = new ConditionsManagement(api, items);
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
                setTemporalModule(memberToHelp, getHealthAbility());
            } else if (config.repairShield) {
                memberToHelp = getMemberLowShield();
                if (memberToHelp != 0) {
                    setTemporalModule(memberToHelp, getShieldAbility());
                }
            }
        }
    }

    private void setTemporalModule(int memberToHelp, Ability ability) {
        if (ability != null && botApi.getModule().getClass() != AmbulanceModule.class) {
            botApi.setModule(
                    new AmbulanceModule(api, memberToHelp, ability,
                            config.returnToTarget, config.healthToRepair));
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

    public Ability getShieldAbility() {
        if (items.getItem(Ability.AEGIS_SHIELD_REPAIR, ItemFlag.USABLE, ItemFlag.READY,
                ItemFlag.AVAILABLE).isPresent()) {
            return Ability.AEGIS_SHIELD_REPAIR;
        }
        return null;
    }

    public Ability getHealthAbility() {
        if (items.getItem(Ability.AEGIS_HP_REPAIR, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.AVAILABLE)
                .isPresent()) {
            return Ability.AEGIS_HP_REPAIR;
        }
        if (items.getItem(Ability.AEGIS_REPAIR_POD, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.AVAILABLE)
                .isPresent()) {
            return Ability.AEGIS_REPAIR_POD;
        }

        if (this.conditionsManagement.useSelectableReadyWhenReady(Ability.SOLACE)) {
            return null;
        }
        if (this.conditionsManagement.useSelectableReadyWhenReady(Ability.SOLACE_PLUS_NANO_CLUSTER_REPAIRER_PLUS)) {
            return null;
        }
        return null;
    }

}
