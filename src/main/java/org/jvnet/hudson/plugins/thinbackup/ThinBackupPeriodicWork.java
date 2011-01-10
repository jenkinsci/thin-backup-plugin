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
  private static final Logger LOGGER = Logger
      .getLogger("hudson.plugins.thinbackup");
  private static final int COMPUTER_TIMEOUT_WAIT = 500; // ms

  public ThinBackupPeriodicWork() {
    super("ThinBackup Worker Thread");
  }

  @Override
  public long getRecurrencePeriod() {
    return MIN;
  }

  @Override
  protected void execute(final TaskListener arg0) throws IOException,
      InterruptedException {
    if (executeNow()) {
      backupNow();
    }
  }

  protected void backupNow() {
    try {
      final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.getInstance();
      final String backupPath = plugin.getBackupPath();
      if (!StringUtils.isEmpty(backupPath)) {
        final Hudson hudson = Hudson.getInstance();
        hudson.doQuietDown();
        LOGGER.fine("Wait until executors are idle to perform backup.");
        waitUntilIdle();
        LOGGER.info("Perform backup task.");
        new HudsonBackup(backupPath, Hudson.getInstance().getRootDir()).run();
        hudson.doCancelQuietDown();
      } else {
        LOGGER.warning("ThinBackup is not configured yet: No backup path set.");
      }
    } catch (final IOException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  private boolean executeNow() {
    CronTab cronTab = null;
    try {
      final long currentTime = System.currentTimeMillis();
      cronTab = new CronTab(getCronTimeFromConfig());
      final Calendar nextExecution = cronTab.ceil(currentTime);
      final long delay = nextExecution.getTimeInMillis() - currentTime;
      LOGGER.fine(MessageFormat.format(
          "current time: {0} next execution: {1} delay [s]: {2}", new Date(
              currentTime), nextExecution.getTime(), TimeUnit2.MILLISECONDS
              .toSeconds(delay)));
      return delay < MIN;
    } catch (final ANTLRException e) {
      LOGGER
          .warning("Cannot parse the specified 'BackupTime'. Check cron notation.");
      return false;
    }
  }

  private String getCronTimeFromConfig() {
    final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.getInstance();
    final String backupTime = plugin.getBackupTime();
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
