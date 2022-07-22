package com.deeme.behaviours;

import com.deeme.types.VerifierChecker;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.core.entities.Portal;
import com.github.manolo8.darkbot.core.itf.Behaviour;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.extensions.features.Feature;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.GameScreenAPI;
import eu.darkbot.api.managers.StatsAPI;

import java.util.Arrays;

@Feature(name = "Others", description = "Many options")
public class Others implements Behaviour, Configurable<Others.LCConfig> {

    private LCConfig lcConfig;
    private Main main;
    private long nextRefresh = 0;
    protected PluginAPI api;
    protected GameScreenAPI gameScreen;
    protected StatsAPI stats;
    protected BotAPI bot;

    @Override
    public void setConfig(Others.LCConfig conf) {
        this.lcConfig = conf;
    }

    @Override
    public void install(Main main) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            return;
        VerifierChecker.checkAuthenticity();

        this.main = main;

        this.api = main.pluginAPI.getAPI(PluginAPI.class);
        this.gameScreen = api.getAPI(GameScreenAPI.class);
        this.stats = api.getAPI(StatsAPI.class);
        this.bot = api.getAPI(BotAPI.class);
    }

    @Override
    public void tick() {
        if (lcConfig.maxDeathsKO > 0 && main.backpage.sidStatus().contains("KO")) {
            main.config.GENERAL.SAFETY.MAX_DEATHS = lcConfig.maxDeathsKO;
        }
        if (lcConfig.reloadIfCrash && stats.getPing() > 10000 && inPortal()) {
            if (nextRefresh <= System.currentTimeMillis()) {
                nextRefresh = System.currentTimeMillis() + 120000;
                Main.API.handleRefresh();
            }
        }
        if (lcConfig.maxMemory > 0 && gameScreen.getMemory() > lcConfig.maxMemory && bot.getModule().canRefresh()) {
            if (nextRefresh <= System.currentTimeMillis()) {
                nextRefresh = System.currentTimeMillis() + 120000;
                Main.API.handleRefresh();
            }
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
