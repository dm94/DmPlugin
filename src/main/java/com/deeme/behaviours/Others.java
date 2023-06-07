package com.deeme.behaviours;

import java.util.Collection;

import com.deeme.types.backpage.Utils;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.modules.DisconnectModule;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.other.Gui;
import eu.darkbot.api.managers.BackpageAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.GameScreenAPI;
import eu.darkbot.api.managers.HeroAPI;
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
    protected final HeroAPI heroapi;
    protected final BackpageAPI backpage;
    private final Gui lostConnectionGUI;
    private long lastLoggedIn = 0;

    private Collection<? extends Portal> portals;

    public Others(Main main, PluginAPI api) {
        this(main, api,
                api.requireAPI(BotAPI.class),
                api.requireAPI(StatsAPI.class),
                api.requireAPI(HeroItemsAPI.class));
    }

    @Inject
    public Others(Main main, PluginAPI api, BotAPI bot, StatsAPI stats, HeroItemsAPI heroItems) {
        Utils.showDonateDialog();
        this.main = main;
        this.api = api;
        this.bot = bot;
        this.stats = stats;
        this.items = heroItems;
        this.backpage = api.getAPI(BackpageAPI.class);
        this.heroapi = api.getAPI(HeroAPI.class);
        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        this.portals = entities.getPortals();

        GameScreenAPI gameScreenAPI = api.getAPI(GameScreenAPI.class);
        lostConnectionGUI = gameScreenAPI.getGui("lost_connection");

        this.lastLoggedIn = 0;
    }

    @Override
    public void setConfig(ConfigSetting<LCConfig> arg0) {
        this.lcConfig = arg0.getValue();
    }

    @Override
    public void onTickBehavior() {
        if (lcConfig.maxDeathsKO > 0 && backpage.getSidStatus().contains("KO")) {
            main.config.GENERAL.SAFETY.MAX_DEATHS = lcConfig.maxDeathsKO;
        }

        if (lcConfig.reloadIfCrash && stats.getPing() > 10000 && inPortal()) {
            refresh();
        }

        if (lcConfig.maxMemory > 0 && Main.API.getMemoryUsage() >= lcConfig.maxMemory && bot.getModule().canRefresh()) {
            refresh();
        }

        autoBuyLogic();
        closeBotLogic();
    }

    @Override
    public void onStoppedBehavior() {
        closeBotLogic();
    }

    private void closeBotLogic() {
        if (lcConfig.closeBotMinutes > 0 && this.lastLoggedIn > 0 && isDisconnect()
                && !(bot.getModule() instanceof DisconnectModule)) {
            if (this.lastLoggedIn < System.currentTimeMillis() - (lcConfig.closeBotMinutes * 60000)) {
                System.exit(0);
            }
        } else {
            this.lastLoggedIn = System.currentTimeMillis();
        }
    }

    private boolean isDisconnect() {
        return heroapi.getMap() == null || heroapi.getMap().getId() == -1
                || (lostConnectionGUI != null && lostConnectionGUI.isVisible());
    }

    private void autoBuyLogic() {
        if (lcConfig.autoBuyLcb10 && this.stats.getTotalCredits() >= 100000) {
            this.items.getItem(SelectableItem.Laser.LCB_10, ItemFlag.USABLE).ifPresent(i -> {
                if (i.getQuantity() <= 1000) {
                    items.useItem(SelectableItem.AutoBuy.LCB_10, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.AVAILABLE);
                }
            });
        }

        if (lcConfig.autoBuyPlt2026 && this.stats.getTotalCredits() >= 50000) {
            this.items.getItem(SelectableItem.Rocket.PLT_2026, ItemFlag.USABLE).ifPresent(i -> {
                if (i.getQuantity() <= 100) {
                    items.useItem(SelectableItem.AutoBuy.PLT_2026, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.AVAILABLE);
                }
            });
        }
    }

    private boolean inPortal() {
        return portals.stream().anyMatch(p -> p.distanceTo(heroapi) < 200);
    }

    private void refresh() {
        if (nextRefresh <= System.currentTimeMillis()) {
            nextRefresh = System.currentTimeMillis() + 120000;
            Main.API.handleRefresh();
        }
    }

    @Configuration("others")
    public static class LCConfig {
        @Option("others.max_deaths")
        @Number(max = 99, step = 1)
        public int maxDeathsKO = 0;

        @Option("others.reload")
        public boolean reloadIfCrash = false;

        @Option("others.close_bot")
        @Number(max = 120, step = 1)
        public int closeBotMinutes = 0;

        @Option("others.max_memory")
        @Number(max = 6000, step = 100)
        public int maxMemory = 0;

        @Option("others.auto_buy_lcb10")
        public boolean autoBuyLcb10 = false;

        @Option("others.auto_buy_plt_2026")
        public boolean autoBuyPlt2026 = false;
    }
}
