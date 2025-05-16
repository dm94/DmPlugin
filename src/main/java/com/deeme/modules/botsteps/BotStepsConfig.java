package com.deeme.modules.botsteps;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.shared.config.ProfileNames;
import eu.darkbot.api.config.annotations.Dropdown;
import com.deeme.types.gui.JStepsTable;
import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.core.utils.Lazy;
import java.util.HashMap;
import java.util.Map;

@Configuration("bot_steps_module")
public class BotStepsConfig {
  @Option("Select GUI to use")
  @Dropdown(options = BotStepsModule.GuiDropdown.class)
  public String guiId = "";

  @Option("Steps to execute")
  @Editor(value = JStepsTable.class, shared = true)
  public Map<String, Step> steps = new HashMap<>();
  private Lazy<String> addedSteps = new Lazy<>();

  public Lazy<String> getAddedSteps() {
    return addedSteps;
  }

  @Option("Profile to change to after steps")
  @Dropdown(options = ProfileNames.class)
  public String profileToChange = "config";
}
