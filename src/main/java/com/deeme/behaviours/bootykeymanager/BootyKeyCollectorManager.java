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
import eu.darkbot.api.managers.*;
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
    for (Stats.BootyKey bootyKey : config.bootyKeysToMonitor) {
      if (stats.getStatValue(bootyKey) > 0) {
        return true;
      }
    }
    return false;
  }

  private void updateResourceCollection(boolean enable) {
    Map<String, BoxInfo> allBoxes = boxInfos.getValue();

    for (Stats.BootyKey bootyKey : config.bootyKeysToMonitor) {
      Optional<String> resourceName = getResourceNameForBootyKey(bootyKey);
      if (resourceName.isPresent()) {
        BoxInfo boxInfo = allBoxes.get(resourceName.get());
        if (boxInfo != null) {
          boxInfo.setShouldCollect(enable);
        }
      }
    }
  }

  private Optional<String> getResourceNameForBootyKey(Stats.BootyKey bootyKey) {
    switch (bootyKey) {
      case GREEN:
        return Optional.of("PIRATE_BOOTY");
      case BLUE:
        return Optional.of("BLUE_BOOTY");
      case RED:
        return Optional.of("RED_BOOTY");
      case SILVER:
        return Optional.of("SILVER_BOOTY");
      case APOCALYPSE:
        return Optional.of("APOCALYPSE_BOOTY");
      case PROMETHEUS:
        return Optional.of("PROMETHEUS_BOOTY");
      case OBSIDIAN_MICROCHIP:
        return Optional.of("OBSIDIAN_BOOTY");
      case BLACK_LIGHT_CODE:
        return Optional.of("BLACK_LIGHT_BOOTY");
      case BLACK_LIGHT_DECODER:
        return Optional.of("BLACK_LIGHT_DECODER_BOOTY");
      case PROSPEROUS_FRAGMENT:
        return Optional.of("PROSPEROUS_BOOTY");
      case ASTRAL:
        return Optional.of("ASTRAL_BOOTY");
      case ASTRAL_SUPREME:
        return Optional.of("ASTRAL_SUPREME_BOOTY");
      case EMPYRIAN:
        return Optional.of("EMPYRIAN_BOOTY");
      case LUCENT:
        return Optional.of("LUCENT_BOOTY");
      case PERSEUS:
        return Optional.of("PERSEUS_BOOTY");
      default:
        return Optional.empty();
    }
  }
}
