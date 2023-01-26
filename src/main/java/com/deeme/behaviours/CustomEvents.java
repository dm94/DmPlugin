package com.deeme.behaviours;

import java.util.Arrays;

import com.deeme.types.SharedFunctions;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deeme.types.config.CustomEventsConfig;
import com.deeme.types.config.ExtraKeyConditionsKey;
import com.deeme.types.config.ExtraKeyConditionsSelectable;
import com.github.manolo8.darkbot.config.Config;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.Condition;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "CustomEvents", description = "Another custom events")
public class CustomEvents implements Behavior, Configurable<CustomEventsConfig> {

    protected final PluginAPI api;
    protected final ConfigSetting<Boolean> rsbEnabled;
    protected final ConfigSetting<Config.Loot.Sab> sabSettings;
    private CustomEventsConfig config;
    protected long clickDelay;
    protected final HeroItemsAPI items;

    public CustomEvents(PluginAPI api) {
        this(api,
                api.requireAPI(AuthAPI.class),
                api.requireAPI(ConfigAPI.class),
                api.requireAPI(HeroItemsAPI.class));
    }

    @Inject
    public CustomEvents(PluginAPI api, AuthAPI auth, ConfigAPI configApi, HeroItemsAPI heroItems) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog();

        this.api = api;
        this.rsbEnabled = configApi.requireConfig("loot.rsb.enabled");
        this.sabSettings = configApi.requireConfig("loot.sab");
        this.items = heroItems;
    }

    @Override
    public void setConfig(ConfigSetting<CustomEventsConfig> arg0) {
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
        useKeyWithConditions(config.otherKey);
        useKeyWithConditions(config.otherKey2);
        useKeyWithConditions(config.otherKey3);
        useKeyWithConditions(config.otherKey4);
        useKeyWithConditions(config.otherKey5);
        useKeyWithConditions(config.selectable1);
        useKeyWithConditions(config.selectable2);
        useKeyWithConditions(config.selectable3);
        useKeyWithConditions(config.selectable4);
        useKeyWithConditions(config.selectable5);
        useKeyWithConditions(config.selectable6);
        useKeyWithConditions(config.selectable7);
        useKeyWithConditions(config.selectable8);
        useKeyWithConditions(config.selectable9);
        useKeyWithConditions(config.selectable10);
    }

    public boolean useKeyWithConditions(Condition condition, SelectableItem selectableItem) {
        if (selectableItem != null && condition != null && condition.get(api).allows()) {
            return useSelectableReadyWhenReady(selectableItem);
        }
        return false;
    }

    public boolean useKeyWithConditions(ExtraKeyConditionsSelectable extra) {
        if (extra.enable) {
            return useKeyWithConditions(extra.condition, SharedFunctions.getItemById(extra.item));
        }
        return false;
    }

    public boolean useKeyWithConditions(ExtraKeyConditionsKey extra) {
        if (extra.enable) {
            SelectableItem selectableItem = items.getItem(extra.Key);
            if (selectableItem != null && extra.condition != null && extra.condition.get(api).allows()) {
                return useSelectableReadyWhenReady(selectableItem);
            }
        }
        return false;
    }

    public boolean useSelectableReadyWhenReady(SelectableItem selectableItem) {
        if (System.currentTimeMillis() - clickDelay < 1000) {
            return false;
        }
        if (selectableItem == null) {
            return false;
        }

        boolean isReady = items.getItem(selectableItem, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.AVAILABLE)
                .isPresent();

        if (isReady && items.useItem(selectableItem).isSuccessful()) {
            clickDelay = System.currentTimeMillis();
            return true;
        }

        return false;
    }

}
