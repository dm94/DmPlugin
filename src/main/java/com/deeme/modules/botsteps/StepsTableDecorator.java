package com.deeme.modules.botsteps;

import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.annotations.Table;

public class StepsTableDecorator implements Table.Decorator<Step> {

  @Override
  public void handle(JTable jTable, JScrollPane jScrollPane, JPanel jPanel,
      ConfigSetting<Map<String, Step>> configSetting) {}

}
