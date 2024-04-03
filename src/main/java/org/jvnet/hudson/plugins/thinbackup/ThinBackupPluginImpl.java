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

import hudson.Extension;
import hudson.ExtensionList;
import hudson.scheduler.CronTab;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.jvnet.hudson.plugins.thinbackup.utils.EnvironmentVariableNotDefinedException;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
@Symbol("thinBackup")
public class ThinBackupPluginImpl extends GlobalConfiguration {

    private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

    private String fullBackupSchedule = "";
    private String diffBackupSchedule = "";
    private String backupPath = "";
    private int nrMaxStoredFull = -1;
    private String excludedFilesRegex = null;
    private boolean waitForIdle = true;
    private int forceQuietModeTimeout = Utils.FORCE_QUIETMODE_TIMEOUT_MINUTES;
    private static final int VERY_HIGH_TIMEOUT = 12 * 60;
    private boolean cleanupDiff = false;
    private boolean moveOldBackupsToZipFile = false;
    private boolean backupBuildResults = true;
    private boolean backupBuildArchive = false;
    private boolean backupPluginArchives = false;
    private boolean backupUserContents = false;

    private boolean backupConfigHistory = false;
    private boolean backupAdditionalFiles = false;
    private String backupAdditionalFilesRegex = null;
    private boolean backupNextBuildNumber = false;
    private boolean backupBuildsToKeepOnly = false;
    private boolean failFast = true;

    @DataBoundConstructor
    public ThinBackupPluginImpl() {
        // check if old config is there and no new config exists
        final File oldConfig = new File(Jenkins.get().getRootDir(), "thinBackup.xml");
        final File newConfig = getConfigFile().getFile();
        boolean oldConfigExists = oldConfig.exists();
        boolean newConfigExits = newConfig.exists();
        if (oldConfigExists && !newConfigExits) {
            LOGGER.warning("old config of 'thinBackup' detected, moving to new name");
            try {
                Files.move(oldConfig.toPath(), newConfig.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOGGER.severe(
                        "unable to move old config to new config, you will need to reconfigure thinBackup plugin manually");
            }
        }
        load();
        LOGGER.fine("'thinBackup' plugin initialized.");
    }

    public static ThinBackupPluginImpl get() {
        return ExtensionList.lookupSingleton(ThinBackupPluginImpl.class);
    }

    public File getJenkinsHome() {
        Jenkins jenkins = Jenkins.get();
        return jenkins.getRootDir();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        // hidden in optionalBlock
        if (json.containsKey("waitForIdle")) {
            forceQuietModeTimeout = json.getJSONObject("waitForIdle").getInt("forceQuietModeTimeout");
        }

        if (json.containsKey("backupAdditionalFiles")) {
            backupAdditionalFilesRegex =
                    json.getJSONObject("backupAdditionalFiles").getString("backupAdditionalFilesRegex");
        }

        waitForIdle = json.containsKey("waitForIdle");
        backupAdditionalFiles = json.containsKey("backupAdditionalFiles");
        backupBuildArchive = json.containsKey("backupBuildArchive");
        backupBuildResults = json.containsKey("backupBuildResults");
        backupBuildsToKeepOnly = json.containsKey("backupBuildsToKeepOnly");
        backupConfigHistory = json.containsKey("backupConfigHistory");
        backupNextBuildNumber = json.containsKey("backupNextBuildNumber");
        backupPluginArchives = json.containsKey("backupPluginArchives");
        backupUserContents = json.containsKey("backupUserContents");
        cleanupDiff = json.containsKey("cleanupDiff");
        failFast = json.containsKey("failFast");
        moveOldBackupsToZipFile = json.containsKey("moveOldBackupsToZipFile");

        nrMaxStoredFull = json.getInt("nrMaxStoredFull");

        backupPath = json.getString("backupPath");
        diffBackupSchedule = json.getString("diffBackupSchedule");
        excludedFilesRegex = json.getString("excludedFilesRegex");
        fullBackupSchedule = json.getString("fullBackupSchedule");
        save();
        return true;
    }

    @DataBoundSetter
    public void setFullBackupSchedule(final String fullBackupSchedule) {
        this.fullBackupSchedule = fullBackupSchedule;
        save();
    }

    public String getFullBackupSchedule() {
        return fullBackupSchedule;
    }

    @DataBoundSetter
    public void setDiffBackupSchedule(final String diffBackupSchedule) {
        this.diffBackupSchedule = diffBackupSchedule;
        save();
    }

    public String getDiffBackupSchedule() {
        return diffBackupSchedule;
    }

    public int getForceQuietModeTimeout() {
        return forceQuietModeTimeout;
    }

    @DataBoundSetter
    public void setForceQuietModeTimeout(int forceQuietModeTimeout) {
        this.forceQuietModeTimeout = forceQuietModeTimeout;
        save();
    }

    @DataBoundSetter
    public void setBackupPath(final String backupPath) {
        this.backupPath = backupPath;
        save();
    }

    /**
     * Get the backup path as entered by the user. May contain traces of environment variables.
     * <p>
     * If you need a path that can be used as is (env. vars expanded), please use @link{getExpandedBackupPath}.
     *
     * @return the backup path as stored in the settings page.
     */
    public String getBackupPath() {
        return backupPath;
    }

    /**
     * @return the backup path with possibly contained environment variables expanded.
     */
    public String getExpandedBackupPath() {
        String expandedPath = "";

        try {
            expandedPath = Utils.expandEnvironmentVariables(backupPath);
        } catch (final EnvironmentVariableNotDefinedException evnde) {
            LOGGER.log(Level.SEVERE, evnde.getMessage() + " Using unexpanded path.");
            expandedPath = backupPath;
        }

        return expandedPath;
    }

    @DataBoundSetter
    public void setNrMaxStoredFull(final int nrMaxStoredFull) {
        this.nrMaxStoredFull = nrMaxStoredFull;
        save();
    }

    public int getNrMaxStoredFull() {
        return nrMaxStoredFull;
    }

    @DataBoundSetter
    public void setCleanupDiff(final boolean cleanupDiff) {
        this.cleanupDiff = cleanupDiff;
        save();
    }

    public boolean isCleanupDiff() {
        return cleanupDiff;
    }

    @DataBoundSetter
    public void setMoveOldBackupsToZipFile(final boolean moveOldBackupsToZipFile) {
        this.moveOldBackupsToZipFile = moveOldBackupsToZipFile;
        save();
    }

    public boolean isMoveOldBackupsToZipFile() {
        return moveOldBackupsToZipFile;
    }

    @DataBoundSetter
    public void setBackupBuildResults(final boolean backupBuildResults) {
        this.backupBuildResults = backupBuildResults;
        save();
    }

    public boolean isBackupBuildResults() {
        return backupBuildResults;
    }

    @DataBoundSetter
    public void setBackupBuildArchive(final boolean backupBuildArchive) {
        this.backupBuildArchive = backupBuildArchive;
        save();
    }

    public boolean isBackupBuildArchive() {
        return backupBuildArchive;
    }

    @DataBoundSetter
    public void setBackupBuildsToKeepOnly(boolean backupBuildsToKeepOnly) {
        this.backupBuildsToKeepOnly = backupBuildsToKeepOnly;
        save();
    }

    public boolean isBackupBuildsToKeepOnly() {
        return backupBuildsToKeepOnly;
    }

    @DataBoundSetter
    public void setBackupNextBuildNumber(final boolean backupNextBuildNumber) {
        this.backupNextBuildNumber = backupNextBuildNumber;
        save();
    }

    public boolean isBackupNextBuildNumber() {
        return backupNextBuildNumber;
    }

    @DataBoundSetter
    public void setExcludedFilesRegex(final String excludedFilesRegex) {
        this.excludedFilesRegex = excludedFilesRegex;
        save();
    }

    public boolean isBackupUserContents() {
        return this.backupUserContents;
    }

    @DataBoundSetter
    public void setBackupUserContents(boolean backupUserContents) {
        this.backupUserContents = backupUserContents;
        save();
    }

    public String getExcludedFilesRegex() {
        return excludedFilesRegex;
    }

    @DataBoundSetter
    public void setBackupPluginArchives(final boolean backupPluginArchives) {
        this.backupPluginArchives = backupPluginArchives;
        save();
    }

    public boolean isBackupPluginArchives() {
        return backupPluginArchives;
    }

    @DataBoundSetter
    public void setBackupAdditionalFiles(final boolean backupAdditionalFiles) {
        this.backupAdditionalFiles = backupAdditionalFiles;
        save();
    }

    public boolean isBackupAdditionalFiles() {
        return backupAdditionalFiles;
    }

    @DataBoundSetter
    public void setBackupAdditionalFilesRegex(final String backupAdditionalFilesRegex) {
        this.backupAdditionalFilesRegex = backupAdditionalFilesRegex;
        save();
    }

    public String getBackupAdditionalFilesRegex() {
        return backupAdditionalFilesRegex;
    }

    @DataBoundSetter
    public void setWaitForIdle(boolean waitForIdle) {
        this.waitForIdle = waitForIdle;
        save();
    }

    public boolean isWaitForIdle() {
        return this.waitForIdle;
    }

    public boolean isBackupConfigHistory() {
        return backupConfigHistory;
    }

    @DataBoundSetter
    public void setBackupConfigHistory(boolean backupConfigHistory) {
        this.backupConfigHistory = backupConfigHistory;
        save();
    }

    public boolean isFailFast() {
        return failFast;
    }

    @DataBoundSetter
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
        save();
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
}
