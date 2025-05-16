package com.deeme.modules.botsteps;

import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.types.Condition;

public class Step {
  @Option("X position")
  @Number(min = 1, max = 4000, step = 1)
  public int x = 1;

  @Option("Y position")
  @Number(min = 1, max = 4000, step = 1)
  public int y = 1;

  @Number(min = 1000, max = 1000000, step = 1)
  @Option("Wait time (ms)")
  public int waitMs = 1000;

  @Option("general.condition")
  public Condition condition;
}
