package com.deeme.behaviours.bootykeymanager;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.api.game.stats.Stats;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.api.config.types.BoxInfo;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Feature(name = "BootyKey Collector Manager",
    description = "Automatically enables or disables resource collection based on BootyKey availability")
public class BootyKeyCollectorManager implements Behavior, Configurable<BootyKeyCollectorConfig> {
  private final StatsAPI stats;
  private final ConfigAPI configApi;

  private BootyKeyCollectorConfig config;
  private ConfigSetting<Map<String, BoxInfo>> boxInfos;
  private long nextCheck = 0;

  @Inject
  public BootyKeyCollectorManager(PluginAPI api, AuthAPI auth) {
    if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
      throw new SecurityException();
    }

    VerifierChecker.requireAuthenticity(auth);

    ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
    FeatureInfo<?> feature = extensionsAPI.getFeatureInfo(this.getClass());
    Utils.discordCheck(feature, auth.getAuthId());
    Utils.showDonateDialog(feature, auth.getAuthId());

    this.stats = api.requireAPI(StatsAPI.class);
    this.configApi = api.requireAPI(ConfigAPI.class);

    this.boxInfos = configApi.requireConfig("collect.box_infos");
  }

  @Override
  public void setConfig(ConfigSetting<BootyKeyCollectorConfig> config) {
    this.config = config.getValue();
  }

  @Override
  public void onTickBehavior() {
    if (System.currentTimeMillis() < nextCheck) {
      return;
    }

    nextCheck = System.currentTimeMillis() + (config.checkInterval * 1000L);

    boolean shouldCollect = shouldEnableCollection();
    updateResourceCollection(shouldCollect);
  }

  private boolean shouldEnableCollection() {
    for (String bootyKeyName : config.bootyKeysToMonitor) {
      try {
        Stats.BootyKey bootyKey = Stats.BootyKey.valueOf(bootyKeyName);
        if (stats.getStatValue(bootyKey) > 0) {
          return true;
        }
      } catch (IllegalArgumentException e) {
        // Skip invalid booty key names
      }
    }
    return false;
  }

  private void updateResourceCollection(boolean enable) {
    Map<String, BoxInfo> allBoxes = boxInfos.getValue();

    for (String bootyKeyName : config.bootyKeysToMonitor) {
      try {
        Stats.BootyKey bootyKey = Stats.BootyKey.valueOf(bootyKeyName);
        Optional<String> resourceName = getResourceNameForBootyKey(bootyKey);
        if (resourceName.isPresent()) {
          BoxInfo boxInfo = allBoxes.get(resourceName.get());
          if (boxInfo != null) {
            boxInfo.setShouldCollect(enable);
          }
        }
      } catch (IllegalArgumentException e) {
        // Skip invalid booty key names
      }
    }
  }

  private Optional<String> getResourceNameForBootyKey(Stats.BootyKey bootyKey) {
    switch (bootyKey) {
      case GREEN:
        return Optional.of("PIRATE_BOOTY_GOLD");
      case BLUE:
        return Optional.of("PIRATE_BOOTY_BLUE");
      case RED:
        return Optional.of("PIRATE_BOOTY_RED");
      case SILVER:
        return Optional.of("PIRATE_BOOTY_SILVER");
      case APOCALYPSE:
        return Optional.of("MASQUE_BOOTY_BOX");
      case PROMETHEUS:
        return Optional.of("PROMETHEUS_BOOTY_BOX");
      case OBSIDIAN_MICROCHIP:
        return Optional.of("BLACK_BOOTY_BOX");
      case PROSPEROUS_FRAGMENT:
        return Optional.of("PROSPEROUS_BOOTY_BOX");
      case ASTRAL:
        return Optional.of("ASTRAL_BOOTY_BOX");
      case ASTRAL_SUPREME:
        return Optional.of("ASTRAL_PRIME_BOOTY_BOX");
      case EMPYRIAN:
        return Optional.of("EMPYRIAN_BOOTY_BOX");
      case PERSEUS:
        return Optional.of("PERSEUS_BLESSING_BOOTY_BOX");
      case LUCENT:
      case BLACK_LIGHT_CODE:
      case BLACK_LIGHT_DECODER:
      default:
        return Optional.empty();
    }
  }
}
