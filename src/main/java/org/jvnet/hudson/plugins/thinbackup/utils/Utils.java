package org.jvnet.hudson.plugins.thinbackup.utils;

import hudson.model.Computer;
import hudson.model.Hudson;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {
  private static final int COMPUTER_TIMEOUT_WAIT = 500; // ms
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

  public static void waitUntilIdle() {
    final Computer computers[] = Hudson.getInstance().getComputers();

    boolean running;
    do {
      running = false;
      for (final Computer computer : computers) {
        if (computer.countBusy() != 0) {
          running = true;
          break;
        }
      }

      try {
        Thread.sleep(COMPUTER_TIMEOUT_WAIT);
      } catch (final InterruptedException e) {
        LOGGER.log(Level.WARNING, e.getMessage(), e);
      }
    } while (running);
  }
}
