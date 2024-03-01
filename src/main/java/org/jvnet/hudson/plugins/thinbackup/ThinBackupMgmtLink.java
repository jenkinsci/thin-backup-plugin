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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.model.TaskListener;
import hudson.scheduler.CronTab;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import jenkins.model.Jenkins;
import jenkins.util.Timer;
import org.jvnet.hudson.plugins.thinbackup.restore.HudsonRestore;
import org.jvnet.hudson.plugins.thinbackup.utils.EnvironmentVariableNotDefinedException;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * A backup solution for Hudson. Backs up configuration files from Hudson and its jobs.
 *
 * Originally based on the Backup plugin by Vincent Sellier, Manufacture Fran�aise des Pneumatiques Michelin, Romain
 * Seguy, et.al. Subsequently heavily modified.
 */
@Extension
public class ThinBackupMgmtLink extends ManagementLink {
    private static final String THIN_BACKUP_SUBPATH = "/thinBackup";
    private static final int VERY_HIGH_TIMEOUT = 12 * 60;
    private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

    @Override
    public String getDisplayName() {
        return "ThinBackup";
    }

    @Override
    public String getIconFileName() {
        return "symbol-archive-outline plugin-ionicons-api";
    }

    @Override
    public String getUrlName() {
        return "thinBackup";
    }

    @Override
    public String getDescription() {
        return "Backup your global and job specific configuration.";
    }

    public void doBackupManual(final StaplerRequest res, final StaplerResponse rsp) throws IOException {
        LOGGER.info("Starting manual backup.");

        final Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return;
        }
        jenkins.checkPermission(Jenkins.ADMINISTER);

        final ThinBackupPeriodicWork manualBackupWorker = new ThinBackupPeriodicWork() {
            @Override
            protected void execute(final TaskListener arg0) {
                backupNow(BackupType.FULL);
            }
        };
        Timer.get().schedule(manualBackupWorker, 0, TimeUnit.SECONDS);

        rsp.sendRedirect(res.getContextPath() + THIN_BACKUP_SUBPATH);
    }

    public void doRestore(
            final StaplerRequest res,
            final StaplerResponse rsp,
            @QueryParameter("restoreBackupFrom") final String restoreBackupFrom,
            @QueryParameter("restoreNextBuildNumber") final String restoreNextBuildNumber,
            @QueryParameter("restorePlugins") final String restorePlugins)
            throws IOException {
        LOGGER.info("Starting restore operation.");

        final Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return;
        }
        jenkins.checkPermission(Jenkins.ADMINISTER);

        jenkins.doQuietDown();
        LOGGER.fine("Waiting until executors are idle to perform restore...");
        Utils.waitUntilIdle();

        try {
            final File hudsonHome = jenkins.getRootDir();
            final Date restoreFromDate = new SimpleDateFormat(Utils.DISPLAY_DATE_FORMAT).parse(restoreBackupFrom);

            final HudsonRestore hudsonRestore = new HudsonRestore(
                    hudsonHome,
                    ThinBackupPluginImpl.get().getExpandedBackupPath(),
                    restoreFromDate,
                    "on".equals(restoreNextBuildNumber),
                    "on".equals(restorePlugins));
            hudsonRestore.restore();

            LOGGER.info("Restore finished.");
        } catch (ParseException e) {
            LOGGER.severe("Cannot parse restore option. Aborting.");
        } catch (final Exception ise) {
            LOGGER.severe("Could not restore. Aborting.");
        } finally {
            jenkins.doCancelQuietDown();
            rsp.sendRedirect(res.getContextPath() + THIN_BACKUP_SUBPATH);
        }
    }

    public void doSaveSettings(
            final StaplerRequest res,
            final StaplerResponse rsp,
            @QueryParameter("backupPath") final String backupPath,
            @QueryParameter("fullBackupSchedule") final String fullBackupSchedule,
            @QueryParameter("diffBackupSchedule") final String diffBackupSchedule,
            @QueryParameter("nrMaxStoredFull") final String nrMaxStoredFull,
            @QueryParameter("excludedFilesRegex") final String excludedFilesRegex,
            @QueryParameter("moveOldBackupsToZipFile") final boolean moveOldBackupsToZipFile,
            @QueryParameter("cleanupDiff") final boolean cleanupDiff,
            @QueryParameter("backupBuildResults") final boolean backupBuildResults,
            @QueryParameter("backupBuildArchive") final boolean backupBuildArchive,
            @QueryParameter("backupBuildsToKeepOnly") final boolean backupBuildsToKeepOnly,
            @QueryParameter("backupUserContents") final boolean backupUserContents,
            @QueryParameter("backupNextBuildNumber") final boolean backupNextBuildNumber,
            @QueryParameter("backupPluginArchives") final boolean backupPluginArchives,
            @QueryParameter("backupAdditionalFiles") final boolean backupAdditionalFiles,
            @QueryParameter("backupAdditionalFilesRegex") final String backupAdditionalFilesRegex,
            @QueryParameter("waitForIdle") final boolean waitForIdle,
            @QueryParameter("backupConfigHistory") final boolean backupConfigHistory,
            @QueryParameter("forceQuietModeTimeout") final String forceQuietModeTimeout,
            @QueryParameter("failFast") final boolean failFast)
            throws IOException {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return;
        }
        jenkins.checkPermission(Jenkins.ADMINISTER);

        final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.get();
        plugin.setBackupPath(backupPath);
        plugin.setFullBackupSchedule(fullBackupSchedule);
        plugin.setDiffBackupSchedule(diffBackupSchedule);
        plugin.setNrMaxStoredFullAsString(nrMaxStoredFull);
        plugin.setExcludedFilesRegex(excludedFilesRegex);
        plugin.setCleanupDiff(cleanupDiff);
        plugin.setMoveOldBackupsToZipFile(moveOldBackupsToZipFile);
        plugin.setBackupBuildResults(backupBuildResults);
        plugin.setBackupBuildArchive(backupBuildArchive);
        plugin.setBackupBuildsToKeepOnly(backupBuildsToKeepOnly);
        plugin.setBackupUserContents(backupUserContents);
        plugin.setBackupConfigHistory(backupConfigHistory);
        plugin.setBackupNextBuildNumber(backupNextBuildNumber);
        plugin.setBackupPluginArchives(backupPluginArchives);
        plugin.setBackupAdditionalFiles(backupAdditionalFiles);
        plugin.setBackupAdditionalFilesRegex(backupAdditionalFilesRegex);
        plugin.setWaitForIdle(waitForIdle);
        plugin.setForceQuietModeTimeout(Integer.parseInt(forceQuietModeTimeout));
        plugin.setFailFast(failFast);
        plugin.save();
        LOGGER.finest("Saving backup settings done.");
        rsp.sendRedirect(res.getContextPath() + THIN_BACKUP_SUBPATH);
    }

    public ThinBackupPluginImpl getConfiguration() {
        return ThinBackupPluginImpl.get();
    }

    public List<String> getAvailableBackups() {
        final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.get();
        return Utils.getBackupsAsDates(new File(plugin.getExpandedBackupPath()));
    }

    public ListBoxModel doFillBackupItems() {
        final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.get();
        final List<String> backupsAsDates = Utils.getBackupsAsDates(new File(plugin.getExpandedBackupPath()));
        var model = new ListBoxModel();
        for (String entry : backupsAsDates) {
            model.add(new ListBoxModel.Option(entry));
        }
        return model;
    }

    public FormValidation doCheckBackupPath(@QueryParameter String value) {
        if ((value == null) || value.trim().isEmpty()) {
            return FormValidation.error("Backup path must not be empty.");
        }

        String expandedPathMessage = "";
        String expandedPath = "";
        try {
            expandedPath = Utils.expandEnvironmentVariables(value);
        } catch (final EnvironmentVariableNotDefinedException evnd) {
            return FormValidation.error(evnd.getMessage());
        }
        if (!expandedPath.equals(value)) {
            expandedPathMessage = String.format("The path will be expanded to '%s'.%n%n", expandedPath);
        }

        final File backupdir = new File(expandedPath);
        if (!backupdir.exists()) {
            return FormValidation.warning(
                    expandedPathMessage + "The directory does not exist, but will be created before the first run.");
        }
        if (!backupdir.isDirectory()) {
            return FormValidation.error(expandedPathMessage
                    + "A file with this name exists, thus a directory with the same name cannot be created.");
        }
        final File tmp = new File(expandedPath + File.separator + "test.txt");
        try {
            tmp.createNewFile();
        } catch (final Exception e) {
            if (!tmp.canWrite()) {
                return FormValidation.error(expandedPathMessage + "The directory exists, but is not writable.");
            }
        } finally {
            if (tmp.exists()) {
                final boolean deleted = tmp.delete();
                if (!deleted) {
                    LOGGER.log(Level.WARNING, "Temp-file " + tmp.getAbsolutePath() + " could not be deleted.");
                }
            }
        }
        if (!expandedPath.trim().equals(expandedPath)) {
            return FormValidation.warning(
                    expandedPathMessage + "Path contains leading and/or trailing whitespaces - is this intentional?");
        }

        if (!expandedPathMessage.isEmpty()) {
            return FormValidation.warning(expandedPathMessage.substring(0, expandedPathMessage.length() - 2));
        }

        return FormValidation.ok();
    }

    public FormValidation doCheckBackupSchedule(@QueryParameter("value") final String schedule) {
        if ((schedule != null) && !schedule.isEmpty()) {
            String message;
            try {
                message = new CronTab(schedule).checkSanity();
            } catch (final IllegalArgumentException e) {
                return FormValidation.error("Invalid cron schedule. " + e.getMessage());
            }
            if (message != null) {
                return FormValidation.warning("Cron schedule warning: " + message);
            } else {
                return FormValidation.ok();
            }
        } else {
            return FormValidation.ok();
        }
    }

    public FormValidation doCheckExcludedFilesRegex(@QueryParameter("value") final String regex) {

        if ((regex == null) || (regex.isEmpty())) {
            return FormValidation.ok();
        }

        try {
            Pattern.compile(regex);
        } catch (final PatternSyntaxException pse) {
            return FormValidation.error("Regex syntax is invalid.");
        }

        if (regex.trim().isEmpty()) {
            return FormValidation.warning(
                    "Regex is valid, but consists entirely of whitespaces - is this intentional?");
        }

        if (!regex.trim().equals(regex)) {
            return FormValidation.warning(
                    "Regex is valid, but contains leading and/or trailing whitespaces - is this intentional?");
        }

        return FormValidation.ok();
    }

    public FormValidation doCheckWaitForIdle(@QueryParameter("value") final String waitForIdle) {
        if (Boolean.parseBoolean(waitForIdle)) {
            return FormValidation.ok();
        } else {
            return FormValidation.warning(
                    "This may or may not generate corrupt backups! Be aware that no data get changed during the backup process!");
        }
    }

    public FormValidation doCheckForceQuietModeTimeout(@QueryParameter("value") final String timeout) {
        FormValidation validation = FormValidation.validateIntegerInRange(timeout, -1, Integer.MAX_VALUE);
        if (!FormValidation.ok().equals(validation)) {
            return validation;
        }

        int intTimeout = Integer.parseInt(timeout);
        if (intTimeout > VERY_HIGH_TIMEOUT) {
            return FormValidation.warning("You choose a very long timeout. The value need to be in minutes.");
        } else {
            return FormValidation.ok();
        }
    }

    /**
     * Name of the category for this management link. Exists so that plugins with core dependency pre-dating the version
     * when this was introduced can define a category.
     * <p>
     *
     * @return name of the desired category, one of the enum values of Category, e.g. {@code STATUS}.
     * @since 2.226
     */
    @NonNull
    @Override
    public Category getCategory() {
        return Category.TOOLS;
    }
}
