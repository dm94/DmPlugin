package com.deeme.behaviours;

import com.deeme.types.VerifierChecker;
import com.deeme.types.gui.AdvertisingMessage;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.core.entities.Portal;
import com.github.manolo8.darkbot.core.itf.Behaviour;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.extensions.features.Feature;

@Feature(name = "Others", description = "Many options")
public class Others implements Behaviour, Configurable<Others.LCConfig> {

    private LCConfig lcConfig;
    private Main main;
    private long nextRefresh = 0;

    @Override
    public void setConfig(Others.LCConfig conf) {
        this.lcConfig = conf;
    }

    @Override
    public void install(Main main) {
        this.main = main;

        AdvertisingMessage.showAdverMessage();

        if (!main.hero.map.gg) {
            AdvertisingMessage.newUpdateMessage(main.featureRegistry.getFeatureDefinition(this));
        }
    }

    @Override
    public void tick() {
        if (!AdvertisingMessage.hasAccepted) {
            main.featureRegistry.getFeatureDefinition(this).setStatus(false);
            main.pluginHandler.updatePluginsSync();
            return;
        }
        if (lcConfig.maxDeathsKO > 0 && main.backpage.sidStatus().contains("KO")) {
            main.config.GENERAL.SAFETY.MAX_DEATHS = lcConfig.maxDeathsKO;
        }
        if (lcConfig.reloadIfCrash && main.pingManager.ping > 10000 && inPortal()) {
            if (nextRefresh <= System.currentTimeMillis()) {
                nextRefresh = System.currentTimeMillis() + 120000;
                Main.API.handleRefresh();
            }
        }
    }

    public static class LCConfig {
        @Option(value = "Max deaths if KO", description = "Max deaths if status SID is KO")
        @Num(max = 99, step = 1)
        public int maxDeathsKO = 0;

        @Option(value = "Reload if stuck jumping", description = "As the game goes wrong and sometimes gets stuck jumping this makes a reload if it happens")
        public boolean reloadIfCrash = false;

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
