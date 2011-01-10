package org.jvnet.hudson.plugins.thinbackup;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.scheduler.CronTab;
import hudson.util.TimeUnit2;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.plugins.thinbackup.backup.HudsonBackup;

import antlr.ANTLRException;

@Extension
public class ThinBackupPeriodicWork extends AsyncPeriodicWork {
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");
  private static final int COMPUTER_TIMEOUT_WAIT = 500; // ms

  public enum BackupType {
    NONE, FULL, DIFF
  }

  public ThinBackupPeriodicWork() {
    super("ThinBackup Worker Thread");
  }

  @Override
  public long getRecurrencePeriod() {
    return MIN;
  }

  @Override
  protected void execute(final TaskListener arg0) throws IOException, InterruptedException {
    final BackupType type = executeNow();
    if (type != BackupType.NONE) {
      backupNow(type);
    }
  }

  protected void backupNow(final BackupType type) {
    try {
      final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.getInstance();
      final String backupPath = plugin.getBackupPath();
      if (!StringUtils.isEmpty(backupPath)) {
        final Hudson hudson = Hudson.getInstance();
        hudson.doQuietDown();
        LOGGER.fine("Wait until executors are idle to perform backup.");
        waitUntilIdle();
        LOGGER.info("Perform backup task.");
        new HudsonBackup(backupPath, Hudson.getInstance().getRootDir(), type).run();
        hudson.doCancelQuietDown();
      } else {
        LOGGER.warning("ThinBackup is not configured yet: No backup path set.");
      }
    } catch (final IOException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  private BackupType executeNow() {
    final long currentTime = System.currentTimeMillis();
    final long fullDelay = calculateDelay(currentTime, BackupType.FULL);
    final long diffDelay = calculateDelay(currentTime, BackupType.DIFF);

    BackupType res = BackupType.NONE;
    long delay = Long.MAX_VALUE;
    if ((fullDelay == -1) && (diffDelay == -1)) {
      return BackupType.NONE;
    } else if ((fullDelay != -1) && (diffDelay == -1)) {
      res = BackupType.FULL;
      delay = fullDelay;
    } else if ((fullDelay == -1) && (diffDelay != -1)) {
      res = BackupType.DIFF;
      delay = diffDelay;
    } else {
      res = BackupType.DIFF;
      delay = diffDelay;
      if (fullDelay <= diffDelay) {
        delay = fullDelay;
        res = BackupType.FULL;
      }
    }

    return delay < MIN ? res : BackupType.NONE;
  }

  private long calculateDelay(final long currentTime, final BackupType backupType) {
    CronTab cronTab;
    try {
      String cron = null;
      switch (backupType) {
      case FULL:
        cron = getFullCronTimeFromConfig();
        break;
      case DIFF:
        cron = getDiffCronTimeFromConfig();
        break;
      default:
        return -1;
      }
      if (StringUtils.isEmpty(cron)) {
        return -1;
      }

      cronTab = new CronTab(cron);

      final Calendar nextFullExecution = cronTab.ceil(currentTime);
      final long delay = nextFullExecution.getTimeInMillis() - currentTime;
      LOGGER.fine(MessageFormat.format("current time: {0} next {3} execution: {1} delay [s]: {2}",
          new Date(currentTime), nextFullExecution.getTime(), TimeUnit2.MILLISECONDS.toSeconds(delay), backupType));

      return delay;
    } catch (final ANTLRException e) {
      LOGGER.warning("Cannot parse the specified 'BackupTime'. Check cron notation.");
      return -1;
    }
  }

  private String getFullCronTimeFromConfig() {
    final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.getInstance();
    final String backupTime = plugin.getFullBackupSchedule();
    return backupTime;
  }

  private String getDiffCronTimeFromConfig() {
    final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.getInstance();
    final String backupTime = plugin.getDiffBackupSchedule();
    return backupTime;
  }

  private void waitUntilIdle() {
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
