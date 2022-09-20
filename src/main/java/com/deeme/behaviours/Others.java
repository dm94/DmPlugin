package com.deeme.behaviours;

import com.deeme.types.backpage.Utils;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.core.entities.Portal;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "Others", description = "Many options")
public class Others implements Behavior, Configurable<Others.LCConfig> {

    private LCConfig lcConfig;
    private final Main main;
    private long nextRefresh = 0;
    protected final PluginAPI api;
    protected final StatsAPI stats;
    protected final BotAPI bot;
    protected final HeroItemsAPI items;

    public Others(Main main, PluginAPI api) throws UnsupportedOperationException, Exception {
        this(main, api,
                api.requireAPI(BotAPI.class),
                api.requireAPI(StatsAPI.class),
                api.requireAPI(HeroItemsAPI.class));
    }

    @Inject
    public Others(Main main, PluginAPI api, BotAPI bot, StatsAPI stats, HeroItemsAPI heroItems) throws Exception {
        Utils.showDonateDialog();
        this.main = main;
        this.api = api;
        this.bot = bot;
        this.stats = stats;
        this.items = heroItems;
    }

    @Override
    public void setConfig(ConfigSetting<LCConfig> arg0) {
        this.lcConfig = arg0.getValue();
    }

    @Override
    public void onTickBehavior() {
        if (lcConfig.maxDeathsKO > 0 && main.backpage.sidStatus().contains("KO")) {
            main.config.GENERAL.SAFETY.MAX_DEATHS = lcConfig.maxDeathsKO;
        }
        if (lcConfig.reloadIfCrash && stats.getPing() > 10000 && inPortal()) {
            if (nextRefresh <= System.currentTimeMillis()) {
                nextRefresh = System.currentTimeMillis() + 120000;
                Main.API.handleRefresh();
            }
        }

        if (lcConfig.maxMemory > 0 && Main.API.getMemoryUsage() >= lcConfig.maxMemory && bot.getModule().canRefresh()) {
            if (nextRefresh <= System.currentTimeMillis()) {
                nextRefresh = System.currentTimeMillis() + 120000;
                Main.API.handleRefresh();
            }
        }

        if (lcConfig.autoBuyLcb10 && this.stats.getTotalCredits() >= 100000
                && this.items.getItem(SelectableItem.Laser.LCB_10, ItemFlag.USABLE).get()
                        .getQuantity() <= 1000
                && this.items.getItem(SelectableItem.AutoBuy.LCB_10, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
            items.useItem(SelectableItem.AutoBuy.LCB_10);
        }
        if (lcConfig.autoBuyPlt2026 && this.stats.getTotalCredits() >= 50000
                && this.items.getItem(SelectableItem.Rocket.PLT_2026, ItemFlag.USABLE).get()
                        .getQuantity() <= 100
                && this.items.getItem(SelectableItem.AutoBuy.PLT_2026, ItemFlag.USABLE, ItemFlag.READY).isPresent()) {
            items.useItem(SelectableItem.AutoBuy.PLT_2026);
        }
    }

    public static class LCConfig {
        @Option(value = "Max deaths if KO", description = "Max deaths if status SID is KO. 0 = Disabled")
        @Num(max = 99, step = 1)
        public int maxDeathsKO = 0;

        @Option(value = "Reload if stuck jumping", description = "As the game goes wrong and sometimes gets stuck jumping this makes a reload if it happens")
        public boolean reloadIfCrash = false;

        @Option(value = "Reload if memory is exceeded ", description = "0 = Disabled. Reload if memory is exceeded")
        @Num(max = 6000, step = 100)
        public int maxMemory = 0;

        @Option(value = "Auto Buy LCB-10", description = "Automatically buys LCB-10 if it has less than 1000")
        public boolean autoBuyLcb10 = false;

        @Option(value = "Auto Buy PLT-2026", description = "Automatically buys LCB-10 if it has less than 100")
        public boolean autoBuyPlt2026 = false;
    }

    private boolean inPortal() {

        for (Portal p : main.mapManager.entities.portals) {
            if (main.hero.locationInfo.distance(p) < 200) {
                return true;
            }
        }

        return false;
    }
}
