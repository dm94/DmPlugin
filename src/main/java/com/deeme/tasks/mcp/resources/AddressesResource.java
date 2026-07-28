package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Catalog of live memory addresses for DarkBot core objects (managers,
 * GUIs, facades). Every address returned here can be fed straight into
 * {@code bot://inspect?address=...} to read the object's slots.
 *
 * <p>
 * Access to DarkBot internals is done through
 * {@link java.lang.invoke.MethodHandle} exclusively, because
 * PluginClassLoader blocks {@code java.lang.reflect.*} references in
 * compiled bytecode. Objects that do not expose a {@code getAddress()}
 * method are silently skipped.
 * </p>
 */
public class AddressesResource implements McpResource {

  private static final String MAIN_CLASS = "com.github.manolo8.darkbot.Main";
  private static final String MGR_PKG = "com.github.manolo8.darkbot.core.manager.";
  private static final String FACADE_PKG = "com.github.manolo8.darkbot.core.objects.facades.";
  private static final String GUI_BASE = "com.github.manolo8.darkbot.core.objects.Gui";
  private static final String GUI_PKG = "com.github.manolo8.darkbot.core.objects.gui.";

  /** Field specs on {@code Main}: {fieldName, declaredTypeFqcn}. */
  private static final String[][] MANAGERS = {
      { "guiManager", MGR_PKG + "GuiManager" },
      { "hero", MGR_PKG + "HeroManager" },
      { "statsManager", MGR_PKG + "StatsManager" },
      { "settingsManager", MGR_PKG + "SettingsManager" },
      { "mapManager", MGR_PKG + "MapManager" },
      { "facadeManager", MGR_PKG + "FacadeManager" },
      { "repairManager", MGR_PKG + "RepairManager" },
      { "effectManager", MGR_PKG + "EffectManager" },
      { "pingManager", MGR_PKG + "PingManager" },
      { "starManager", MGR_PKG + "StarManager" },
  };

  /** Field specs on {@code GuiManager}. */
  private static final String[][] GUIS = {
      { "lostConnection", GUI_BASE },
      { "connecting", GUI_BASE },
      { "quests", GUI_BASE },
      { "monthlyDeluxe", GUI_BASE },
      { "returnLogin", GUI_BASE },
      { "minimap", GUI_BASE },
      { "targetedOffers", GUI_BASE },
      { "logout", GUI_PKG + "LogoutGui" },
      { "eventProgress", GUI_BASE },
      { "eternalGate", GUI_BASE },
      { "blacklightGate", GUI_BASE },
      { "astralGate", GUI_BASE },
      { "astralSelection", GUI_BASE },
      { "seasonPass", GUI_BASE },
      { "refinement", GUI_PKG + "RefinementGui" },
      { "oreTrade", GUI_PKG + "OreTradeGui" },
      { "settingsGui", GUI_PKG + "SettingsGui" },
      { "chat", GUI_PKG + "ChatGui" },
      { "shipWarpGui", GUI_PKG + "ShipWarpGui" },
      { "assembly", GUI_PKG + "AssemblyManager" },
  };

  /** Field specs on {@code FacadeManager}. */
  private static final String[][] FACADES = {
      { "log", FACADE_PKG + "LogMediator" },
      { "chat", FACADE_PKG + "ChatProxy" },
      { "stats", FACADE_PKG + "StatsProxy" },
      { "escort", FACADE_PKG + "EscortProxy" },
      { "booster", FACADE_PKG + "BoosterProxy" },
      { "settings", FACADE_PKG + "SettingsProxy" },
      { "slotBars", FACADE_PKG + "SlotBarsProxy" },
      { "labyrinth", FACADE_PKG + "FrozenLabyrinthProxy" },
      { "eternalGate", FACADE_PKG + "EternalGateProxy" },
      { "blacklightGate", FACADE_PKG + "EternalBlacklightProxy" },
      { "chrominEvent", FACADE_PKG + "ChrominProxy" },
      { "astralGate", FACADE_PKG + "AstralGateProxy" },
      { "highlight", FACADE_PKG + "HighlightProxy" },
      { "spaceMapWindowProxy", FACADE_PKG + "SpaceMapWindowProxy" },
      { "plutus", FACADE_PKG + "GauntletPlutusProxy" },
      { "npcEventProxy", FACADE_PKG + "NpcEventProxy" },
      { "worldBossOverview", FACADE_PKG + "WorldBossOverviewProxy" },
      { "shipWarpProxy", FACADE_PKG + "ShipWarpProxy" },
  };

  private static final MethodType GET_ADDRESS_TYPE = MethodType.methodType(long.class);

  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
  private final MethodHandles.Lookup lookup = MethodHandles.lookup();

  private volatile Object mainInstance;

  @Override
  public String getUri() {
    return "bot://addresses";
  }

  @Override
  public String getName() {
    return "Memory Addresses";
  }

  @Override
  public String getDescription() {
    return "Live memory addresses for DarkBot core objects (managers, "
        + "GUIs, facades). Use any returned address with "
        + "bot://inspect?address=... to read the object's slots.";
  }

  @Override
  public String read(String uri) {
    JsonObject root = new JsonObject();
    Object main = getMainInstance();
    if (main == null) {
      root.addProperty("error", "DarkBot Main instance not available yet");
      return gson.toJson(root);
    }

    Object guiManager = readField(main, "guiManager", MGR_PKG + "GuiManager");
    Object facadeManager = readField(main, "facadeManager", MGR_PKG + "FacadeManager");

    root.add("managers", collect(main, MANAGERS));
    root.add("guis", collect(guiManager, GUIS));
    root.add("facades", collect(facadeManager, FACADES));

    return gson.toJson(root);
  }

  private JsonObject collect(Object container, String[][] specs) {
    JsonObject group = new JsonObject();
    if (container == null) {
      return group;
    }
    for (String[] spec : specs) {
      Object target = readField(container, spec[0], spec[1]);
      if (target == null) {
        continue;
      }
      Long address = addressOf(target);
      if (address == null) {
        // Object exposes no getAddress() — nothing to inspect.
        continue;
      }
      group.addProperty(spec[0], String.format("0x%x", address));
    }
    return group;
  }

  private Object getMainInstance() {
    Object cached = mainInstance;
    if (cached != null) {
      return cached;
    }
    try {
      Class<?> mainClass = Class.forName(MAIN_CLASS);
      MethodHandle getter = lookup.findStaticGetter(mainClass, "INSTANCE", mainClass);
      Object instance = getter.invoke();
      if (instance != null) {
        mainInstance = instance;
      }
      return instance;
    } catch (Throwable t) {
      return null;
    }
  }

  private Object readField(Object container, String name, String typeFqcn) {
    try {
      Class<?> type = Class.forName(typeFqcn);
      MethodHandle getter = findGetter(container.getClass(), name, type);
      return getter.invoke(container);
    } catch (Throwable t) {
      return null;
    }
  }

  /**
   * @return the address, or {@code null} if the object has no readable
   *         {@code getAddress()}.
   */
  private Long addressOf(Object obj) {
    try {
      MethodHandle mh = findVirtual(obj.getClass(), "getAddress", GET_ADDRESS_TYPE);
      return (long) mh.invoke(obj);
    } catch (Throwable t) {
      return null;
    }
  }

  private MethodHandle findGetter(Class<?> refc, String name, Class<?> type)
      throws IllegalAccessException, NoSuchFieldException {
    try {
      return lookup.findGetter(refc, name, type);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      return MethodHandles.privateLookupIn(refc, lookup).findGetter(refc, name, type);
    }
  }

  private MethodHandle findVirtual(Class<?> refc, String name, MethodType type)
      throws IllegalAccessException, NoSuchMethodException {
    try {
      return lookup.findVirtual(refc, name, type);
    } catch (IllegalAccessException | NoSuchMethodException e) {
      return MethodHandles.privateLookupIn(refc, lookup).findVirtual(refc, name, type);
    }
  }
}
