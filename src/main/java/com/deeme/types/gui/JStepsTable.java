package com.deeme.types.gui;

import com.deeme.modules.botsteps.Step;
import com.deeme.modules.botsteps.BotStepsConfig;
import com.github.manolo8.darkbot.gui.tree.components.InfoTable;
import com.github.manolo8.darkbot.gui.utils.GenericTableModel;
import com.github.manolo8.darkbot.gui.tree.OptionEditor;

// Table editor for bot steps, for use in DarkBot config UI
@SuppressWarnings({"deprecation", "RedundantSuppression"})
public class JStepsTable extends InfoTable<GenericTableModel<?>, Step> implements OptionEditor {
  public JStepsTable(BotStepsConfig config) {
    super(Step.class, config.steps, config.getAddedSteps(), Step::new);
  }
}
