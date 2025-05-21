package com.deeme.behaviours.bestammo;

import java.util.EnumSet;
import java.util.Set;
import com.deeme.types.suppliers.LaserSupplier;
import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Percentage;
import eu.darkbot.api.game.items.SelectableItem.Laser;

@Configuration("best_ammo")
public class BestAmmoConfig {
    @Option("general.enabled")
    public boolean enable = false;

    @Option("general.default_laser")
    @Dropdown(options = LaserSupplier.class)
    public String defaultLaser = "";

    @Option("best_ammo.ammo_to_use_npcs")
    @Dropdown(multi = true)
    public Set<Laser> ammoToUseNpcs = EnumSet.noneOf(Laser.class);

    @Option("best_ammo.ammo_to_use_players")
    @Dropdown(multi = true)
    public Set<Laser> ammoToUsePlayers = EnumSet.allOf(Laser.class);

    @Option("general.options")
    @Dropdown(multi = true)
    public Set<BehaviourOptionsEnum> optionsToUse = EnumSet.of(BehaviourOptionsEnum.VS_PLAYERS,
            BehaviourOptionsEnum.RESPECT_RSB_TAG, BehaviourOptionsEnum.RESPECT_NPC_AMMO,
            BehaviourOptionsEnum.REPLACE_AMMO_KEY, BehaviourOptionsEnum.CHANGE_AMMO_DIRECTLY);

    @Option("general.always_use_below_hp")
    @Percentage
    public double alwaysUseBellowHp = 0;
}
