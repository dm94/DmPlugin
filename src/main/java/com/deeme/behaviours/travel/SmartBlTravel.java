package com.deeme.behaviours.travel;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "Fix BL Travel", description = "Routes via 4-4 when traveling to enemy X-6/7/8 maps and ship level is too low for BL-to-BL portals", enabledByDefault = true)
public class SmartBlTravel extends FourFourRouter {
  private static final int BL_PORTAL_MIN_LEVEL = 25;

  private final StatsAPI stats;

  @Inject
  public SmartBlTravel(PluginAPI api, HeroAPI hero, AuthAPI auth, ConfigAPI configApi,
      StarSystemAPI star, StatsAPI stats) {
    super(api, hero, auth, configApi, star);
    this.stats = stats;
  }

  @Override
  protected boolean shouldRouteViaFourFour(GameMap current, GameMap target) {
    return stats.getLevel() < BL_PORTAL_MIN_LEVEL
        && isEnemyHighMap(target)
        && isOwnHighOrBl(current);
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
}
