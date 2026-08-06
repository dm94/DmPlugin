package com.deeme.behaviours.travel;

import java.util.Arrays;
import java.util.Collection;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Behavior;
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
import eu.darkbot.shared.modules.MapModule;

/**
 * Base behavior that, once activated, reroutes the ship to 4-4 before letting
 * the normal travel resume. Subclasses decide when the reroute should trigger.
 */
public abstract class FourFourRouter implements Behavior {
  private static final long CHECK_INTERVAL_MS = 500;

  protected final PluginAPI api;
  protected final HeroAPI hero;
  protected final BotAPI bot;
  protected final StarSystemAPI star;
  protected final Collection<? extends Portal> portals;
  protected final ConfigSetting<Integer> workingMap;

  private boolean overrideActive = false;
  private long nextCheck = 0;

  protected FourFourRouter(PluginAPI api, HeroAPI hero, AuthAPI auth, ConfigAPI configApi, StarSystemAPI star) {
    if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
      throw new SecurityException();
    }
    VerifierChecker.requireAuthenticity(auth);

    ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
    FeatureInfo<?> feature = extensionsAPI.getFeatureInfo(getClass());
    Utils.discordCheck(feature, auth.getAuthId());
    Utils.showDonateDialog(feature, auth.getAuthId());

    this.api = api;
    this.hero = hero;
    this.bot = api.requireAPI(BotAPI.class);
    this.star = star;
    this.workingMap = configApi.requireConfig("general.working_map");

    EntitiesAPI entities = api.requireAPI(EntitiesAPI.class);
    this.portals = entities.getPortals();
  }

  @Override
  public final void onTickBehavior() {
    if (nextCheck > System.currentTimeMillis()) {
      return;
    }
    nextCheck = System.currentTimeMillis() + CHECK_INTERVAL_MS;

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
      routeToFourFour(current);
      return;
    }

    if (shouldRouteViaFourFour(current, target)) {
      overrideActive = true;
      routeToFourFour(current);
    }
  }

  /**
   * @return true when the current/target combination should trigger a reroute via
   *         4-4.
   */
  protected abstract boolean shouldRouteViaFourFour(GameMap current, GameMap target);

  private void routeToFourFour(GameMap current) {
    GameMap fourFour = findFourFour();
    if (fourFour != null && current != fourFour && !portals.isEmpty()) {
      bot.setModule(api.requireInstance(MapModule.class)).setTarget(fourFour);
    }
  }

  protected boolean isFourFour(GameMap map) {
    return "4-4".equals(map.getShortName());
  }

  protected GameMap findFourFour() {
    for (GameMap m : star.getMaps()) {
      if (isFourFour(m)) {
        return m;
      }
    }
    return null;
  }

  private boolean hasConflictiveModuleInUse() {
    return bot.getModule() != null && bot.getModule().getClass() != null
        && bot.getModule().getClass().getName().contains("CaptchaPicker");
  }
}
