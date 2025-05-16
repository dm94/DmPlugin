package com.deeme.modules.botsteps;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Table;
import eu.darkbot.shared.config.ProfileNames;
import eu.darkbot.api.config.annotations.Dropdown;
import java.util.HashMap;
import java.util.Map;

@Configuration("bot_steps_module")
public class BotStepsConfig {
  @Option("guiModule.guiToUse")
  @Dropdown(options = BotStepsModule.GuiDropdown.class)
  public String guiId = "";

  @Table(decorator = {StepsTableDecorator.class})
  public Map<String, Step> steps = new HashMap<>();

  @Option("general.bot_profile")
  @Dropdown(options = ProfileNames.class)
  public String profileToChange = "config";
}
