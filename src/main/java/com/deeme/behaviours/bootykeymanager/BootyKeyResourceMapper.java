package com.deeme.behaviours.bootykeymanager;

import eu.darkbot.api.game.stats.Stats;
import java.util.Map;
import java.util.Optional;

/**
 * Helper class that maps BootyKey types to their corresponding resource names.
 */
public class BootyKeyResourceMapper {
    private static final Map<Stats.BootyKey, String> BOOTY_RESOURCE_MAP =
            Map.ofEntries(Map.entry(Stats.BootyKey.GREEN, "PIRATE_BOOTY_GOLD"),
                    Map.entry(Stats.BootyKey.BLUE, "PIRATE_BOOTY_BLUE"),
                    Map.entry(Stats.BootyKey.RED, "PIRATE_BOOTY_RED"),
                    Map.entry(Stats.BootyKey.SILVER, "PIRATE_BOOTY_SILVER"),
                    Map.entry(Stats.BootyKey.APOCALYPSE, "MASQUE_BOOTY_BOX"),
                    Map.entry(Stats.BootyKey.PROMETHEUS, "PROMETHEUS_BOOTY_BOX"),
                    Map.entry(Stats.BootyKey.OBSIDIAN_MICROCHIP, "BLACK_BOOTY_BOX"),
                    Map.entry(Stats.BootyKey.PROSPEROUS_FRAGMENT, "PROSPEROUS_BOOTY_BOX"),
                    Map.entry(Stats.BootyKey.ASTRAL, "ASTRAL_BOOTY_BOX"),
                    Map.entry(Stats.BootyKey.ASTRAL_SUPREME, "ASTRAL_PRIME_BOOTY_BOX"),
                    Map.entry(Stats.BootyKey.EMPYRIAN, "EMPYRIAN_BOOTY_BOX"),
                    Map.entry(Stats.BootyKey.PERSEUS, "PERSEUS_BLESSING_BOOTY_BOX"),
                    Map.entry(Stats.BootyKey.LUCENT, "LUCENT_ALIEN_EGG_BOOTY_BOX"),
                    Map.entry(Stats.BootyKey.BLACK_LIGHT_CODE, "UNSTABLE_BLACKLIGHT_CACHE"),
                    Map.entry(Stats.BootyKey.BLACK_LIGHT_DECODER, "BLACKLIGHT-CRYPT"));

    private BootyKeyResourceMapper() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Gets the resource name for a given BootyKey.
     * 
     * @param bootyKey the BootyKey to get the resource name for
     * @return Optional containing the resource name if mapping exists, empty otherwise
     */
    public static Optional<String> getResourceNameForBootyKey(Stats.BootyKey bootyKey) {
        return Optional.ofNullable(BOOTY_RESOURCE_MAP.get(bootyKey));
    }
}
