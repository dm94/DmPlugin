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
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.MapModule;

@Feature(name = "Fix BL Travel", description = "Routes via 4-4 when traveling to enemy X-6/7/8 maps and ship level is too low for BL-to-BL portals")
public class SmartBlTravel implements Behavior {
  private static final int BL_PORTAL_MIN_LEVEL = 25;

  private final PluginAPI api;
  private final BotAPI bot;
  private final HeroAPI hero;
  private final StarSystemAPI star;
  private final StatsAPI stats;
  private final Collection<? extends Portal> portals;
  private final ConfigSetting<Integer> workingMap;

  private boolean overrideActive = false;
  private long nextCheck = 0;

  @Inject
  public SmartBlTravel(PluginAPI api, HeroAPI hero, AuthAPI auth, ConfigAPI configApi,
      StarSystemAPI star, StatsAPI stats) {
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
    this.hero = hero;
    this.star = star;
    this.stats = stats;
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

    if (hasConflictiveModuleInUse()) {
      return;
    }

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

    if (stats.getLevel() < BL_PORTAL_MIN_LEVEL
        && isEnemyHighMap(target)
        && isOwnHighOrBl(current)) {
      GameMap fourFour = findFourFour();
      if (fourFour != null && current != fourFour && !portals.isEmpty()) {
        overrideActive = true;
        bot.setModule(api.requireInstance(MapModule.class)).setTarget(fourFour);
      }
    }
  }

  private boolean hasConflictiveModuleInUse() {
    return bot.getModule().getClass().getName().contains("CaptchaPicker");
  }

  private boolean isFourFour(GameMap map) {
    return "4-4".equals(map.getShortName());
  }

  private boolean isEnemyHighMap(GameMap map) {
    String s = map.getShortName();
    if (s == null || !s.matches("^[123]-[678]$")) {
      return false;
    }
    return s.charAt(0) - '0' != hero.getEntityInfo().getFaction().ordinal();
  }

  private boolean isOwnHighOrBl(GameMap map) {
    String s = map.getShortName();
    if (s == null) {
      return false;
    }
    if (s.toLowerCase().contains("bl")) {
      return true;
    }
    String prefix = String.valueOf(hero.getEntityInfo().getFaction().ordinal());
    return s.matches("^" + prefix + "-[678]$");
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
