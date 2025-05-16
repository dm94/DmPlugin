package com.deeme.modules.botsteps;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.game.other.Gui;
import eu.darkbot.api.managers.GameScreenAPI;
import eu.darkbot.api.utils.Inject;

import java.util.List;
import java.util.ArrayList;

@Feature(name = "Bot Steps Module",
    description = "Executes a sequence of GUI steps and changes profile")
public class BotStepsModule implements Module, Configurable<BotStepsConfig> {
  private final PluginAPI api;
  private final GameScreenAPI gameScreen;
  private BotStepsConfig config;
  private Gui selectedGui;
  private int currentStep = 0;
  private long waitUntil = 0;
  private boolean finished = false;

  @Inject
  public BotStepsModule(PluginAPI api) {
    this.api = api;
    this.gameScreen = api.requireAPI(GameScreenAPI.class);
  }

  @Override
  public void setConfig(ConfigSetting<BotStepsConfig> config) {
    this.config = config.getValue();
    this.selectedGui = gameScreen.getGui(this.config.guiId);
    this.currentStep = 0;
    this.finished = false;
  }

  @Override
  public void onTickModule() {
    if (finished || config == null || config.steps == null || config.steps.isEmpty()) {
      return;
    }

    if (selectedGui == null) {
      return;
    }

    if (!selectedGui.isVisible()) {
      selectedGui.setVisible(true);
      return;
    }

    if (currentStep < config.steps.size()) {
      Step step = config.steps.get(currentStep);
      if (System.currentTimeMillis() < waitUntil) {
        return;
      }

      selectedGui.click(step.x, step.y);
      waitUntil = System.currentTimeMillis() + step.waitMs;
      currentStep++;
    } else {
      // All steps done, change profile
      if (config.profileToChange != null && !config.profileToChange.isEmpty()) {
        // Use ConfigAPI to change profile
        api.requireAPI(eu.darkbot.api.managers.ConfigAPI.class)
            .setConfigProfile(config.profileToChange);
      }
      finished = true;
    }
  }

  @Override
  public String getStatus() {
    return finished ? "Finished steps" : "Running step " + currentStep;
  }

  @Override
  public String getStoppedStatus() {
    return getStatus();
  }

  // Dropdown for available GUIs from DarkBot's GuiManager
  public static class GuiDropdown implements Dropdown.Options<String> {
    private PluginAPI api;

    // Empty constructor required by framework
    public GuiDropdown() { /* Required by framework */ }

    @Inject
    public void setApi(PluginAPI api) {
      this.api = api;
    }

    @Override
    public List<String> options() {
      if (api == null) {
        return new ArrayList<>();
      }
      GameScreenAPI gameScreen = api.requireAPI(GameScreenAPI.class);
      List<String> ids = new ArrayList<>();
      for (Gui gui : gameScreen.getGuis()) {
        ids.add(gui.toString());
      }
      return ids;
    }

    @Override
    public String getText(String id) {
      return id;
    }
  }
}
