package com.deeme.modules.botsteps;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;

@Configuration("guiModule.step")
public class Step {
  @Option("guiModule.step.x")
  @Number(min = 1, max = 4000, step = 1)
  public int x = 1;

  @Option("guiModule.step.y")
  @Number(min = 1, max = 4000, step = 1)
  public int y = 1;

  @Number(min = 1000, max = 1000000, step = 1)
  @Option("guiModule.step.waitMs")
  public int waitMs = 1000;

  public void setX(int x) {
    this.x = x;
  }

  public void setY(int y) {
    this.y = y;
  }

  public void setWaitMs(int waitMs) {
    this.waitMs = waitMs;
  }

  public String toString() {
    return "Step{" + "x=" + x + ", y=" + y + ", waitMs=" + waitMs + "}";
  }
}
