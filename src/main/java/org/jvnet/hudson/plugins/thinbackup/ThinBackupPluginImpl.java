package org.jvnet.hudson.plugins.thinbackup;

import hudson.Plugin;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.scheduler.CronTab;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.plugins.thinbackup.backup.HudsonBackup;

import antlr.ANTLRException;

public class ThinBackupPluginImpl extends Plugin {
  private static final int COMPUTER_TIMEOUT_WAIT = 500; // ms
  private static final int MIN_TIMER_DELAY = 1 * 60 * 1000; // one minute
  private static final Logger LOGGER = Logger
      .getLogger("hudson.plugins.thinbackup");

  private static ThinBackupPluginImpl instance = null;

  private String backupPath;
  private String backupTime;
  transient private Timer currentTimer;

  public ThinBackupPluginImpl() {
    instance = this;
  }

  @Override
  public void start() throws Exception {
    super.start();
    load();
    LOGGER.fine("'thinBackup' plugin initialized.");
    scheduleNextRun();
  }

  public void runBackup() {
    try {
      final String backupPath = getBackupPath();
      if (!StringUtils.isEmpty(backupPath)) {
        final Hudson hudson = Hudson.getInstance();
        hudson.doQuietDown();
        LOGGER.info("Wait until executors are idle to perform backup.");
        waitUntilIdle();
        LOGGER.info("Perform backup task.");
        new HudsonBackup(backupPath).run();
        hudson.doCancelQuietDown();
      } else {
        LOGGER.warning("ThinBackup is not configured yet: No backup path set.");
      }
    } catch (final IOException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  public void scheduleNextRun() {
    final Timer timer = new Timer("BackupTimer");
    currentTimer = timer;
    final TimerTask task = new BackupTask(this);
    long delay;
    try {
      delay = calculateTimerDelay();
      LOGGER.info(MessageFormat.format(
          "Schedule new backup task in {0} seconds.", delay / 1000));
      timer.schedule(task, delay);
    } catch (final ANTLRException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } catch (final IllegalArgumentException iae) {
      LOGGER.severe(iae.getMessage());
    }
  }

  public void waitUntilIdle() {
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

  private long calculateTimerDelay() throws ANTLRException {
    final String backupTime = getBackupTime();
    if (!StringUtils.isEmpty(backupTime)) {
      final long currentTime = System.currentTimeMillis();
      final long nextExecution = new CronTab(backupTime).ceil(currentTime)
          .getTimeInMillis();
      long delay = nextExecution - currentTime;
      if (delay <= 0) {
        delay = MIN_TIMER_DELAY;
      }
      return delay;
    } else {
      throw new IllegalArgumentException(
          "ThinBackup is not configured yet: No cron specification set.");
    }
  }

  public static ThinBackupPluginImpl getInstance() {
    return instance;
  }

  public void setBackupPath(final String backupPath) {
    this.backupPath = backupPath;
    currentTimer.cancel();
    scheduleNextRun();
  }

  public String getBackupPath() {
    return backupPath;
  }

  public void setBackupTime(final String backupTime) {
    this.backupTime = backupTime;
    currentTimer.cancel();
    scheduleNextRun();
  }

  public String getBackupTime() {
    return backupTime;
  }

}
