package com.deeme.behaviours.bestrocket;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deeme.types.suppliers.BestRocketSupplier;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "Auto Best Rocket", description = "Automatically switches rockets. Will use all available rockets")
public class AutoBestRocket implements Behavior, Configurable<BestRocketConfig> {

    protected final PluginAPI api;
    protected final HeroAPI heroapi;
    private final HeroItemsAPI items;
    private BestRocketConfig config;
    private BestRocketSupplier bestRocketSupplier;

    public AutoBestRocket(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class), api.requireAPI(HeroItemsAPI.class));
    }

    @Inject
    public AutoBestRocket(PluginAPI api, AuthAPI auth, HeroItemsAPI items) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog(auth.getAuthId());

        this.api = api;
        this.items = api.requireAPI(HeroItemsAPI.class);
        this.heroapi = api.getAPI(HeroAPI.class);

        this.bestRocketSupplier = new BestRocketSupplier(api);
    }

    @Override
    public void setConfig(ConfigSetting<BestRocketConfig> arg0) {
        this.bestRocketSupplier.setConfig(arg0);
        this.config = arg0.getValue();
    }

    @Override
    public void onStoppedBehavior() {
        if (config != null && config.tickStopped) {
            onTickBehavior();
        }
    }

    @Override
    public void onTickBehavior() {
        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid() && heroapi.isAttacking(target)) {
            changeRocket(this.bestRocketSupplier.getBestRocket(target, target instanceof Npc));
        }
    }

    private void changeRocket(SelectableItem rocket) {
        if (rocket == null) {
            return;
        }
        items.useItem(rocket, 500, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.NOT_SELECTED);
    }
}
