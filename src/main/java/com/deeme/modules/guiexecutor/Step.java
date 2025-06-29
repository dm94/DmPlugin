package com.deeme.modules.guiexecutor;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;

/**
 * Represents a single step in the GUI Executor module, containing coordinates and wait time.
 */
@Configuration("guiModule.step")
public class Step {
  @Option("guiModule.step.x")
  @Number(min = 1, max = 4000, step = 1)
  public int x = 1;

  @Option("guiModule.step.y")
  @Number(min = 1, max = 4000, step = 1)
  public int y = 1;

  @Number(min = 1, max = 1000, step = 1)
  @Option("guiModule.step.secondsToWait")
  public int secondsToWait = 1;

  /**
   * Gets the X coordinate for this step.
   * 
   * @return the X coordinate
   */
  public int getX() {
    return x;
  }

  /**
   * Gets the Y coordinate for this step.
   * 
   * @return the Y coordinate
   */
  public int getY() {
    return y;
  }

  /**
   * Gets the number of seconds to wait at this step.
   * 
   * @return seconds to wait
   */
  public int getSecondsToWait() {
    return secondsToWait;
  }

  /**
   * Sets the X coordinate for this step.
   * 
   * @param x the X coordinate
   */
  public void setX(int x) {
    this.x = x;
  }

  /**
   * Sets the Y coordinate for this step.
   * 
   * @param y the Y coordinate
   */
  public void setY(int y) {
    this.y = y;
  }

  /**
   * Sets the number of seconds to wait at this step.
   * 
   * @param secondsToWait seconds to wait
   */
  public void setSecondsToWait(int secondsToWait) {
    this.secondsToWait = secondsToWait;
  }

  /**
   * Returns a string representation of the step.
   * 
   * @return string representation
   */
  @Override
  public String toString() {
    return "Step{" + "x=" + x + ", y=" + y + ", secondsToWait=" + secondsToWait + "}";
  }
}
