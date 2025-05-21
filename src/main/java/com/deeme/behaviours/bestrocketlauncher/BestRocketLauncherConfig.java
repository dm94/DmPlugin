package com.deeme.behaviours.bestrocketlauncher;

import java.util.EnumSet;
import java.util.Set;
import com.deeme.types.suppliers.RocketLauncherSupplier;
import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Percentage;
import eu.darkbot.api.game.items.SelectableItem.RocketLauncher;

@Configuration("best_rocket_launcher")
public class BestRocketLauncherConfig {
    @Option("general.enabled")
    public boolean enable = false;

    @Option("general.default_rocket_launcher")
    @Dropdown(options = RocketLauncherSupplier.class)
    public String defaultRocket = "";

    @Option("best_rocket_launcher.rockets_to_use_npcs")
    @Dropdown(multi = true)
    public Set<RocketLauncher> rocketsToUseNPCs = EnumSet.allOf(RocketLauncher.class);

    @Option("best_rocket_launcher.rockets_to_use_pvp")
    @Dropdown(multi = true)
    public Set<RocketLauncher> rocketsToUsePlayers = EnumSet.allOf(RocketLauncher.class);

    @Option("general.options")
    @Dropdown(multi = true)
    public Set<BehaviourOptionsEnum> options = EnumSet.of(BehaviourOptionsEnum.VS_PLAYERS);

    @Option("general.always_use_below_hp")
    @Percentage
    public double alwaysUseBellowHp = 0;
}
