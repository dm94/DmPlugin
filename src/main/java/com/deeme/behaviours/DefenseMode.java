package com.deeme.behaviours;

import com.deeme.types.ShipAttacker;
import com.deeme.types.config.Defense;
import com.deeme.types.gui.AdvertisingMessage;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.itf.Behaviour;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.extensions.features.Feature;

@Feature(name = "Defense Mode", description = "Add enemy defense options")
public class DefenseMode implements Behaviour, Configurable<Defense> {
    private ShipAttacker shipAttacker;
    private Main main;
    private Defense defenseConfig;

    @Override
    public void setConfig(Defense defense) {
        this.defenseConfig = defense;
        setup();
    }

    @Override
    public void install(Main main) {
        this.main = main;
        setup();
        AdvertisingMessage.showAdverMessage();
        if (!main.hero.map.gg) {
            AdvertisingMessage.newUpdateMessage(main.featureRegistry.getFeatureDefinition(this),main.VERSION);
        }
    }

    @Override
    public void uninstall() {
        shipAttacker.uninstall();
    }

    private void setup() {
        if (main == null || defenseConfig == null) return;

        this.shipAttacker = new ShipAttacker(main,defenseConfig);
    }
    @Override
    public void tick() {
        if (shipAttacker != null) {
            shipAttacker.tick();
        }
    }
}
