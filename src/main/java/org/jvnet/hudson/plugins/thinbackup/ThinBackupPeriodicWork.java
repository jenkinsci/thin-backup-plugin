/*
 * The MIT License
 *
 * Copyright (c) 2011, Borland (a Micro Focus Company), Matthias Steinkogler, Thomas Fuerer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
    final BackupType type = getNextScheduledBackup();
    if (type != BackupType.NONE) {
      backupNow(type);
    }
  }

  protected void backupNow(final BackupType type) {
    try {
      final String backupPath = plugin.getBackupPath();
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
        final Hudson hudson = Hudson.getInstance();
        hudson.doQuietDown();
        LOGGER.fine("Wait until executors are idle to perform backup.");
        Utils.waitUntilIdle();
        new HudsonBackup(new File(backupPath), Hudson.getInstance().getRootDir(), type, maxStoredFull, cleanupDiff)
            .backup();
        hudson.doCancelQuietDown();
      } else {
        LOGGER.warning("ThinBackup is not configured yet: No backup path set.");
      }
    } catch (final IOException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  private BackupType getNextScheduledBackup() {
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
        cron = plugin.getFullBackupSchedule();
        break;
      case DIFF:
        cron = plugin.getDiffBackupSchedule();
        break;
      default:
        return -1;
      }
      if (StringUtils.isEmpty(cron)) {
        return -1;
      }

      cronTab = new CronTab(cron);

      final Calendar nextExecution = cronTab.ceil(currentTime);
      final long delay = nextExecution.getTimeInMillis() - currentTime;

      LOGGER.fine(MessageFormat.format("Current time: {0}. Next execution ({3}) in {2} seconds which is {1} ",
          new Date(currentTime), nextExecution.getTime(), TimeUnit2.MILLISECONDS.toSeconds(delay), backupType));

      if (delay < 0) {
        final String msg = "Delay is a negative number, which means the next execution is in the past! Something bad happened.";
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
