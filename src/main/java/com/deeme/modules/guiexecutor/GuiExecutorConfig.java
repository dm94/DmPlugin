package com.deeme.modules.guiexecutor;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Table;
import eu.darkbot.api.config.annotations.Table.Control;
import eu.darkbot.shared.config.ProfileNames;
import eu.darkbot.api.config.annotations.Dropdown;
import java.util.HashMap;
import java.util.Map;

@Configuration("guiModule")
public class GuiExecutorConfig {
  @Option("guiModule.guiToUse")
  public String guiId = "";

  @Option("guiModule.stepsToFollow")
  @Table(controls = {Control.ADD, Control.REMOVE}, decorator = {StepsTableDecorator.class})
  public Map<String, Step> steps = new HashMap<>();

  @Option("guiModule.profileToChange")
  @Dropdown(options = ProfileNames.class)
  public String profileToChange = "config";
}
