package com.deeme.modules.guiexecutor;

import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;

import java.util.Arrays;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.FeatureInfo;
import java.util.ArrayList;
import java.util.List;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.game.other.Gui;
import eu.darkbot.api.managers.GameScreenAPI;

@Feature(name = "GUI Executor Module",
    description = "Clicks on the configured window. Advanced users")
public class GuiExecutor implements Module, Configurable<GuiExecutorConfig> {
  /** API instance used to access game functionality */
  private final PluginAPI api;
  /** Game screen API used to access GUI elements */
  private final GameScreenAPI gameScreen;
  /** Configuration for this module */
  private GuiExecutorConfig config;
  /** Currently selected GUI for interaction */
  private Gui selectedGui;
  /** Index of the current step being executed */
  private int currentStep = 0;
  /** Timestamp in milliseconds until which to wait before executing the next step */
  private long waitUntil = 0;
  /** Cached list of steps from the configuration for efficient access */
  private List<Step> cachedSteps;

  public GuiExecutor(PluginAPI api) throws SecurityException {

    AuthAPI auth = api.requireAPI(AuthAPI.class);
    if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
      throw new SecurityException();
    }

    VerifierChecker.requireAuthenticity(auth);

    ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
    FeatureInfo<?> feature = extensionsAPI.getFeatureInfo(this.getClass());
    Utils.discordCheck(feature, auth.getAuthId());
    Utils.showDonateDialog(feature, auth.getAuthId());

    this.api = api;
    this.gameScreen = api.requireAPI(GameScreenAPI.class);
  }

  @Override
  public void setConfig(ConfigSetting<GuiExecutorConfig> config) {
    this.config = config.getValue();
    this.currentStep = 0;
    this.cachedSteps = null;
  }

  @Override
  public void onTickModule() {
    if (config == null || this.config.guiId.isEmpty() || config.steps == null
        || config.steps.isEmpty()) {
      return;
    }

    if (cachedSteps == null && config.steps != null) {
      cachedSteps = new ArrayList<>(config.steps.values());
    }

    if (selectedGui == null) {
      this.selectedGui = gameScreen.getGui(this.config.guiId);
      return;
    }

    if (cachedSteps != null && currentStep < cachedSteps.size()) {
      executeCurrentStep();
    } else {
      if (selectedGui.isVisible()) {
        selectedGui.setVisible(false);
        return;
      }
      handleProfileChange();
    }
  }

  /**
   * Executes the current step in the execution sequence. Makes the GUI visible if needed, waits
   * until the specified time, then performs the click and updates the wait time for the next step.
   */
  private void executeCurrentStep() {
    if (!selectedGui.isVisible()) {
      selectedGui.setVisible(true);
      return;
    }

    Step step = cachedSteps.get(currentStep);
    if (System.currentTimeMillis() < waitUntil || step == null) {
      return;
    }

    try {
      selectedGui.click(step.x, step.y);
      waitUntil = System.currentTimeMillis() + (step.secondsToWait * 1000);
      currentStep++;
    } catch (Exception e) {
      // Log error or handle exception
      System.out.println("Error executing step: " + e.getMessage());
      // Consider whether to retry, skip step, or abort execution
    }
  }

  private void handleProfileChange() {
    if (config.profileToChange != null && !config.profileToChange.isEmpty()) {
      try {
        api.requireAPI(eu.darkbot.api.managers.ConfigAPI.class)
            .setConfigProfile(config.profileToChange);
      } catch (Exception e) {
        System.out.println(e.getLocalizedMessage());
      }
    }
  }

  @Override
  public String getStatus() {
    int totalSteps = (config != null && config.steps != null) ? config.steps.size() : 0;
    return "Running step " + currentStep + "/" + totalSteps + " | GUI Found "
        + (selectedGui != null);
  }

  @Override
  public String getStoppedStatus() {
    return getStatus();
  }
}
