package com.deeme.behaviours.travel;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.utils.Inject;

@Feature(name = "Fix X-1 Travel", description = "Route via 4-4 when traveling to X-1 from high maps")
public class SmartX1Travel extends FourFourRouter {

  @Inject
  public SmartX1Travel(PluginAPI api, HeroAPI hero, AuthAPI auth, ConfigAPI configApi, StarSystemAPI star) {
    super(api, hero, auth, configApi, star);
  }

  @Override
  protected boolean shouldRouteViaFourFour(GameMap current, GameMap target) {
    return isX1(target) && isHighMap(current);
  }

  private boolean isX1(GameMap map) {
    String s = map.getShortName();
    return s.matches("^[123]-[12]$");
  }

  private boolean isHighMap(GameMap map) {
    String s = map.getShortName();
    if (s == null) {
      return false;
    }
    if (s.matches("^[123]-[678]$")) {
      return true;
    }
    return s.toLowerCase().contains("bl");
  }
}
