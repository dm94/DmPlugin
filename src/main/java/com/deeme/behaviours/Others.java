package com.deeme.behaviours;

import java.util.Collection;

import com.deeme.behaviours.others.OthersConfig;
import com.deeme.types.backpage.Utils;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.PlayerInfo;
import com.github.manolo8.darkbot.config.PlayerTag;
import com.github.manolo8.darkbot.modules.DisconnectModule;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Player;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.other.Gui;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BackpageAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.GameScreenAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "Others", description = "Many options")
public class Others implements Behavior, Configurable<OthersConfig> {

    private OthersConfig lcConfig;
    private final Main main;
    private long nextRefresh = 0;
    private final StatsAPI stats;
    private final BotAPI bot;
    private final HeroItemsAPI items;
    private final HeroAPI heroapi;
    private final BackpageAPI backpage;
    private final Gui lostConnectionGUI;
    private long lastLoggedIn = 0;

    private static final int ROCKETS_CREDITS_COST = 50000;
    private static final int AMMO_CREDITS_COST = 100000;

    private Collection<? extends Portal> portals;
    private final Collection<? extends Player> players;

    public Others(Main main, PluginAPI api) {
        this(main, api,
                api.requireAPI(BotAPI.class),
                api.requireAPI(StatsAPI.class),
                api.requireAPI(HeroItemsAPI.class));
    }

    @Inject
    public Others(Main main, PluginAPI api, BotAPI bot, StatsAPI stats, HeroItemsAPI heroItems) {
        Utils.showDonateDialog(api.requireAPI(ExtensionsAPI.class).getFeatureInfo(this.getClass()),
                api.requireAPI(AuthAPI.class).getAuthId());
        this.main = main;
        this.bot = bot;
        this.stats = stats;
        this.items = heroItems;
        this.backpage = api.requireAPI(BackpageAPI.class);
        this.heroapi = api.requireAPI(HeroAPI.class);
        EntitiesAPI entities = api.requireAPI(EntitiesAPI.class);
        this.portals = entities.getPortals();
        this.players = entities.getPlayers();
        GameScreenAPI gameScreenAPI = api.requireAPI(GameScreenAPI.class);
        lostConnectionGUI = gameScreenAPI.getGui("lost_connection");

        this.lastLoggedIn = 0;
    }

    @Override
    public void setConfig(ConfigSetting<OthersConfig> arg0) {
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
        tagClanMembers();
    }

    @Override
    public void onStoppedBehavior() {
        closeBotLogic();
    }

    private void tagClanMembers() {
        if (!lcConfig.tagClanMembers.active || players == null || players.isEmpty()
                || lcConfig.tagClanMembers.clanTag == null) {
            return;
        }

        int id = heroapi.getEntityInfo().getClanId();

        if (id == 0) {
            return;
        }

        players.stream().filter(player -> player.getEntityInfo().getClanId() == id)
                .forEach(player -> addTag(player, lcConfig.tagClanMembers.clanTag));
    }

    private void addTag(Player player, PlayerTag tag) {
        if (player == null || tag == null) {
            return;
        }

        PlayerInfo pl = null;

        if (main.config.PLAYER_INFOS.containsKey(player.getId())) {
            pl = main.config.PLAYER_INFOS.get(player.getId());
        } else {
            pl = new PlayerInfo(player);
        }

        if (pl == null) {
            return;
        }

        pl.setTag(tag, -1L);

        main.config.PLAYER_INFOS.put(player.getId(), pl);
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
        if (lcConfig.autoBuyLcb10 && this.stats.getTotalCredits() >= AMMO_CREDITS_COST) {
            this.items.getItem(SelectableItem.Laser.LCB_10).ifPresent(i -> {
                if (i.getQuantity() <= 10000) {
                    items.useItem(SelectableItem.AutoBuy.LCB_10, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.AVAILABLE);
                }
            });
        }

        if (lcConfig.autoBuyPlt2026 && this.stats.getTotalCredits() >= ROCKETS_CREDITS_COST) {
            this.items.getItem(SelectableItem.Rocket.PLT_2026).ifPresent(i -> {
                if (i.getQuantity() <= 1000) {
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
}
