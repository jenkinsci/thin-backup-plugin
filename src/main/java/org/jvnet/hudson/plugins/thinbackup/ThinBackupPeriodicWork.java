/**
 *  Copyright (C) 2011  Matthias Steinkogler, Thomas Fürer
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses.
 */
package org.jvnet.hudson.plugins.thinbackup;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.model.Hudson;
import hudson.scheduler.CronTab;
import hudson.util.TimeUnit2;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.plugins.thinbackup.backup.HudsonBackup;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

import antlr.ANTLRException;

@Extension
public class ThinBackupPeriodicWork extends AsyncPeriodicWork {
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");
  private final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.getInstance();

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

  @SuppressWarnings("unused")
  @Override
  protected void execute(final TaskListener arg0) throws IOException, InterruptedException {
    final long currentTime = System.currentTimeMillis();
    final String fullCron = plugin.getFullBackupSchedule();
    final String diffCron = plugin.getDiffBackupSchedule();

    final BackupType type = getNextScheduledBackup(currentTime, fullCron, diffCron);
    if (type != BackupType.NONE) {
      backupNow(type);
    }
  }

  protected void backupNow(final BackupType type) {
    final Hudson hudson = Hudson.getInstance();
    String backupPath = null;
    try {
      backupPath = plugin.getBackupPath();
      final boolean cleanupDiff = plugin.isCleanupDiff();
      final String noMaxStoredFull = plugin.getNrMaxStoredFull();
      int maxStoredFull;
      if (StringUtils.isEmpty(noMaxStoredFull)) {
        maxStoredFull = -1;
      } else {
        try {
          maxStoredFull = Integer.parseInt(noMaxStoredFull);
        } catch (final NumberFormatException nfe) {
          maxStoredFull = -1;
        }
      }

      if (!StringUtils.isEmpty(backupPath)) {
        hudson.doQuietDown();
        LOGGER.fine("Wait until executors are idle to perform backup.");
        Utils.waitUntilIdle();
        new HudsonBackup(new File(backupPath), Hudson.getInstance().getRootDir(), type, maxStoredFull, cleanupDiff,
            plugin.isMoveOldBackupsToZipFile(), plugin.isBackupBuildResults()).backup();
      } else {
        LOGGER.warning("ThinBackup is not configured yet: No backup path set.");
      }
    } catch (final IOException e) {
      final String msg = MessageFormat
          .format(
              "Cannot perform a backup. Please be sure jenkins/hudson has write privileges in the configured backup path '{0}'.",
              backupPath);
      LOGGER.log(Level.SEVERE, msg, e);
    } finally {
      hudson.doCancelQuietDown();
    }
  }

  BackupType getNextScheduledBackup(final long currentTime, final String fullCron, final String diffCron) {
    final long fullDelay = calculateDelay(currentTime, BackupType.FULL, fullCron);
    final long diffDelay = calculateDelay(currentTime, BackupType.DIFF, diffCron);

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

  long calculateDelay(final long currentTime, final BackupType backupType, final String cron) {
    CronTab cronTab;
    try {
      if (StringUtils.isEmpty(cron)) {
        return -1;
      }

      cronTab = new CronTab(cron);

      final Calendar nextExecution = cronTab.ceil(currentTime);
      final long delay = nextExecution.getTimeInMillis() - currentTime;

      LOGGER.fine(MessageFormat.format("Current time: {0}. Next execution ({3}) in {2} seconds which is {1} ",
          new Date(currentTime), nextExecution.getTime(), TimeUnit2.MILLISECONDS.toSeconds(delay), backupType));

      if (delay < 0) {
        final String msg = "Delay is a negative number, which means the next execution is in the past! This happens for Hudson/Jenkins installations with version 1.395 or below. Please upgrade to fix this.";
        LOGGER.severe(msg);
        throw new IllegalStateException(msg);
      }

      return delay;
    } catch (final ANTLRException e) {
      LOGGER.warning(MessageFormat.format(
          "Cannot parse the specified 'Backup schedule for {0} backups'. Check cron notation.", backupType.toString()));
      return -1;
    }
  }

}
