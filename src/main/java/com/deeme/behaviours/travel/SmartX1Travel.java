package com.deeme.behaviours.travel;

import java.util.Arrays;
import java.util.Collection;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.MapModule;

@Feature(name = "Fix X-1 Travel", description = "Route via 4-4 when traveling to X-1 from high maps")
public class SmartX1Travel implements Behavior {
  private final PluginAPI api;
  private final BotAPI bot;
  private final StarSystemAPI star;
  private final Collection<? extends Portal> portals;
  private final ConfigSetting<Integer> workingMap;

  private boolean overrideActive = false;
  private long nextCheck = 0;

  @Inject
  public SmartX1Travel(PluginAPI api, HeroAPI hero, AuthAPI auth, ConfigAPI configApi, StarSystemAPI star) {
    if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
      throw new SecurityException();
    }
    VerifierChecker.requireAuthenticity(auth);

    ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
    FeatureInfo<?> feature = extensionsAPI.getFeatureInfo(this.getClass());
    Utils.discordCheck(feature, auth.getAuthId());
    Utils.showDonateDialog(feature, auth.getAuthId());

    this.api = api;
    this.bot = api.requireAPI(BotAPI.class);
    this.star = star;
    this.workingMap = configApi.requireConfig("general.working_map");

    EntitiesAPI entities = api.requireAPI(EntitiesAPI.class);
    this.portals = entities.getPortals();
  }

  @Override
  public void onTickBehavior() {
    if (nextCheck > System.currentTimeMillis()) {
      return;
    }
    nextCheck = System.currentTimeMillis() + 500;

    GameMap current = star.getCurrentMap();
    if (current == null) {
      return;
    }

    GameMap target = star.findMap(workingMap.getValue()).orElse(null);
    if (target == null) {
      return;
    }

    if (overrideActive) {
      if (isFourFour(current)) {
        overrideActive = false;
        return;
      }

      GameMap fourFour = findFourFour();
      if (fourFour != null && current != fourFour && !portals.isEmpty()) {
        bot.setModule(api.requireInstance(MapModule.class)).setTarget(fourFour);
      }
      return;
    }

    if (isX1(target) && isHighMap(current)) {
      GameMap fourFour = findFourFour();
      if (fourFour != null && current != fourFour && !portals.isEmpty()) {
        overrideActive = true;
        bot.setModule(api.requireInstance(MapModule.class)).setTarget(fourFour);
      }
    }
  }

  private boolean isX1(GameMap map) {
    String s = map.getShortName();
    return s.matches("^[123]-[12]$");
  }

  private boolean isFourFour(GameMap map) {
    return "4-4".equals(map.getShortName());
  }

  private boolean isHighMap(GameMap map) {
    String s = map.getShortName();
    if (s == null) {
      return false;
    }
    if (s.matches("^[123]-[678]$")) {
      return true;
    }
    String l = s.toLowerCase();
    return l.contains("bl");
  }

  private GameMap findFourFour() {
    for (GameMap m : star.getMaps()) {
      if (isFourFour(m)) {
        return m;
      }
    }
    return null;
  }
}
