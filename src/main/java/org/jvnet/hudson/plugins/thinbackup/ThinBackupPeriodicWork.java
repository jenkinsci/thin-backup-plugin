/*
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

import antlr.ANTLRException;
import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.scheduler.CronTab;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.plugins.thinbackup.backup.HudsonBackup;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

@Extension
public class ThinBackupPeriodicWork extends AsyncPeriodicWork {

    private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

    private final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.getInstance();

    public enum BackupType {
        NONE,
        FULL,
        DIFF
    }

    public ThinBackupPeriodicWork() {
        super("ThinBackup Worker Thread");
    }

    @Override
    public long getRecurrencePeriod() {
        return MIN;
    }

    @Override
    protected void execute(final TaskListener arg0) {
        final long currentTime = System.currentTimeMillis();
        final String fullCron = plugin.getFullBackupSchedule();
        final String diffCron = plugin.getDiffBackupSchedule();

        final BackupType type = getNextScheduledBackupType(currentTime, fullCron, diffCron);
        if (type != BackupType.NONE) {
            backupNow(type);
        }
    }

    protected void backupNow(final BackupType type) {
        final Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return;
        }
        final boolean inQuietModeBeforeBackup = jenkins.isQuietingDown();

        String backupPath = null;
        try {
            backupPath = plugin.getExpandedBackupPath();

            if (StringUtils.isNotEmpty(backupPath)) {
                if (plugin.isWaitForIdle()) {
                    LOGGER.fine("Wait until executors are idle to perform backup.");
                    Utils.waitUntilIdleAndSwitchToQuietMode(plugin.getForceQuietModeTimeout(), TimeUnit.MINUTES);
                } else {
                    LOGGER.warning(
                            "Do not wait until Jenkins is idle to perform backup. This could cause corrupt backups.");
                }

                new HudsonBackup(plugin, type).backup();
            } else {
                LOGGER.warning("ThinBackup is not configured yet: No backup path set.");
            }
        } catch (final IOException e) {
            final String msg = MessageFormat.format(
                    "Cannot perform a backup. Please be sure Jenkins has write privileges in the configured backup path ''{0}''.",
                    backupPath);
            LOGGER.log(Level.SEVERE, msg, e);
        } finally {
            if (!inQuietModeBeforeBackup) {
                jenkins.doCancelQuietDown();
            } else {
                LOGGER.info(
                        "Backup process finished, but still in quiet mode as before. The quiet mode needs to be canceled manually, because it is not clear who is putting Jenkins into quiet mode.");
            }
        }
    }

    BackupType getNextScheduledBackupType(final long currentTime, final String fullCron, final String diffCron) {
        final long fullDelay = calculateDelay(currentTime, BackupType.FULL, fullCron);
        final long diffDelay = calculateDelay(currentTime, BackupType.DIFF, diffCron);

        BackupType res = null;
        long delay;
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

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(MessageFormat.format(
                        "Current time: {0,date,medium} {0,time,long}. Next execution ({3}) in {2} seconds which is {1,date,medium} {1,time,long}",
                        new Date(currentTime),
                        nextExecution.getTime(),
                        TimeUnit.MILLISECONDS.toSeconds(delay),
                        backupType));
            }

            if (delay < 0) {
                final String msg =
                        "Delay is a negative number, which means the next execution is in the past! This happens for Hudson/Jenkins installations with version 1.395 or below. Please upgrade to fix this.";
                LOGGER.severe(msg);
                throw new IllegalStateException(msg);
            }

            return delay;
        } catch (final ANTLRException e) {
            LOGGER.warning(MessageFormat.format(
                    "Cannot parse the specified ''Backup schedule for {0} backups''. Check cron notation.",
                    backupType));
            return -1;
        }
    }
}
