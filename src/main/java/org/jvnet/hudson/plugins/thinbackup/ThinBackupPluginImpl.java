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
import hudson.Plugin;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.scheduler.CronTab;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.plugins.thinbackup.utils.EnvironmentVariableNotDefinedException;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import antlr.ANTLRException;

public class ThinBackupPluginImpl extends Plugin {

  private static final int VERY_HIGH_TIMEOUT = 12 * 60;

  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

  private String fullBackupSchedule = "";
  private String diffBackupSchedule = "";
  private String backupPath = "";
  private int nrMaxStoredFull = -1;
  private String excludedFilesRegex = null;
  private boolean waitForIdle = true;
  private int forceQuietModeTimeout = Utils.FORCE_QUIETMODE_TIMEOUT_MINUTES;
  private boolean cleanupDiff = false;
  private boolean moveOldBackupsToZipFile = false;
  private boolean backupBuildResults = true;
  private boolean backupBuildArchive = false;
  private boolean backupPluginArchives = false;
  private boolean backupUserContents = false;
  private boolean backupAdditionalFiles = false;
  private String backupAdditionalFilesRegex = null;
  private boolean backupNextBuildNumber = false;
  private boolean backupBuildsToKeepOnly = false;

  private static ThinBackupPluginImpl instance = null;

  public ThinBackupPluginImpl() {
    instance = this;
  }

  @Override
  public void start() throws Exception {
    super.start();
    load();
    LOGGER.fine("'thinBackup' plugin initialized.");
  }

  public static ThinBackupPluginImpl getInstance() {
    return instance;
  }

  public File getHudsonHome() {
    return Hudson.getInstance().getRootDir();
  }

  public void setFullBackupSchedule(final String fullBackupSchedule) {
    this.fullBackupSchedule = fullBackupSchedule;
  }

  public String getFullBackupSchedule() {
    return fullBackupSchedule;
  }

  public void setDiffBackupSchedule(final String diffBackupSchedule) {
    this.diffBackupSchedule = diffBackupSchedule;
  }

  public String getDiffBackupSchedule() {
    return diffBackupSchedule;
  }

  public int getForceQuietModeTimeout() {
    return forceQuietModeTimeout;
  }

  public void setForceQuietModeTimeout(int forceQuietModeTimeout) {
    this.forceQuietModeTimeout = forceQuietModeTimeout;
  }

  public void setBackupPath(final String backupPath) {
    this.backupPath = backupPath;
  }

  /**
   * Get the backup path as entered by the user. May contain traces of environment variables.
   * 
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

  public void setNrMaxStoredFull(final int nrMaxStoredFull) {
    this.nrMaxStoredFull = nrMaxStoredFull;
  }

  /**
   * @param nrMaxStoredFull
   *          if this string can be parsed as an Integer, nrMaxStoredFull is set to this value, otherwise it is set to
   *          -1.
   */
  public void setNrMaxStoredFullAsString(final String nrMaxStoredFull) {
    if (StringUtils.isEmpty(nrMaxStoredFull)) {
      this.nrMaxStoredFull = -1;
    } else {
      try {
        this.nrMaxStoredFull = Integer.parseInt(nrMaxStoredFull);
      } catch (final NumberFormatException nfe) {
        this.nrMaxStoredFull = -1;
      }
    }
  }

  public int getNrMaxStoredFull() {
    return nrMaxStoredFull;
  }

  public void setCleanupDiff(final boolean cleanupDiff) {
    this.cleanupDiff = cleanupDiff;
  }

  public boolean isCleanupDiff() {
    return cleanupDiff;
  }

  public void setMoveOldBackupsToZipFile(final boolean moveOldBackupsToZipFile) {
    this.moveOldBackupsToZipFile = moveOldBackupsToZipFile;
  }

  public boolean isMoveOldBackupsToZipFile() {
    return moveOldBackupsToZipFile;
  }

  public void setBackupBuildResults(final boolean backupBuildResults) {
    this.backupBuildResults = backupBuildResults;
  }

  public boolean isBackupBuildResults() {
    return backupBuildResults;
  }

  public void setBackupBuildArchive(final boolean backupBuildArchive) {
    this.backupBuildArchive = backupBuildArchive;
  }

  public boolean isBackupBuildArchive() {
    return backupBuildArchive;
  }

  public void setBackupBuildsToKeepOnly(boolean backupBuildsToKeepOnly) {
    this.backupBuildsToKeepOnly = backupBuildsToKeepOnly;
  }

  public boolean isBackupBuildsToKeepOnly() {
    return backupBuildsToKeepOnly;
  }

  public void setBackupNextBuildNumber(final boolean backupNextBuildNumber) {
    this.backupNextBuildNumber = backupNextBuildNumber;
  }

  public boolean isBackupNextBuildNumber() {
    return backupNextBuildNumber;
  }

  public void setExcludedFilesRegex(final String excludedFilesRegex) {
    this.excludedFilesRegex = excludedFilesRegex;
  }

  public boolean isBackupUserContents() {
    return this.backupUserContents;
  }

  public void setBackupUserContents(boolean backupUserContents) {
    this.backupUserContents = backupUserContents;
  }

  public String getExcludedFilesRegex() {
    return excludedFilesRegex;
  }
  
  public void setBackupPluginArchives(final boolean backupPluginArchives) {
    this.backupPluginArchives = backupPluginArchives;
  }

  public boolean isBackupPluginArchives() {
    return backupPluginArchives;
  }
	  
  public void setBackupAdditionalFiles(final boolean backupAdditionalFiles) {
    this.backupAdditionalFiles = backupAdditionalFiles;
  }

  public boolean isBackupAdditionalFiles() {
    return backupAdditionalFiles;
  }

  public void setBackupAdditionalFilesRegex(final String backupAdditionalFilesRegex) {
    this.backupAdditionalFilesRegex = backupAdditionalFilesRegex;
  }

  public String getBackupAdditionalFilesRegex() {
    return backupAdditionalFilesRegex;
  }

  public void setWaitForIdle(boolean waitForIdle) {
    this.waitForIdle = waitForIdle;
  }

  public boolean isWaitForIdle() {
    return this.waitForIdle;
  }

  public FormValidation doCheckForceQuietModeTimeout(final StaplerRequest res, final StaplerResponse rsp,
      @QueryParameter("value") final String timeout) {
    FormValidation validation = FormValidation.validateNonNegativeInteger(timeout);
    if (!FormValidation.ok().equals(validation))
      return validation;
    
    int intTimeout = Integer.parseInt(timeout);
    if (intTimeout > VERY_HIGH_TIMEOUT)
      return FormValidation.warning("You choose a very long timeout. The value need to be in minutes.");
    else
      return FormValidation.ok();
  }

  public FormValidation doCheckBackupPath(final StaplerRequest res, final StaplerResponse rsp,
      @QueryParameter("value") final String path) {
    if ((path == null) || path.trim().isEmpty()) {
      return FormValidation.error("Backup path must not be empty.");
    }

    String expandedPathMessage = "";
    String expandedPath = "";
    try {
      expandedPath = Utils.expandEnvironmentVariables(path);
    } catch (final EnvironmentVariableNotDefinedException evnd) {
      return FormValidation.error(evnd.getMessage());
    }
    if (!expandedPath.equals(path)) {
      expandedPathMessage = String.format("The path will be expanded to '%s'.\n\n", expandedPath);
    }

    final File backupdir = new File(expandedPath);
    if (!backupdir.exists()) {
      return FormValidation.warning(expandedPathMessage
          + "The directory does not exist, but will be created before the first run.");
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
        tmp.delete();
      }
    }
    if (!expandedPath.trim().equals(expandedPath)) {
      return FormValidation.warning(expandedPathMessage
          + "Path contains leading and/or trailing whitespaces - is this intentional?");
    }

    if (!expandedPathMessage.isEmpty()) {
      return FormValidation.warning(expandedPathMessage.substring(0, expandedPathMessage.length() - 2));
    }

    return FormValidation.ok();
  }

  public FormValidation doCheckBackupSchedule(final StaplerRequest res, final StaplerResponse rsp,
      @QueryParameter("value") final String schedule) {
    if ((schedule != null) && !schedule.isEmpty()) {
      String message;
      try {
        message = new CronTab(schedule).checkSanity();
      } catch (final ANTLRException e) {
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

  public FormValidation doCheckExcludedFilesRegex(final StaplerRequest res, final StaplerResponse rsp,
      @QueryParameter("value") final String regex) {

    if ((regex == null) || (regex.isEmpty())) {
      return FormValidation.ok();
    }

    try {
      Pattern.compile(regex);
    } catch (final PatternSyntaxException pse) {
      return FormValidation.error("Regex syntax is invalid.");
    }

    if (regex.trim().isEmpty()) {
      return FormValidation.warning("Regex is valid, but consists entirely of whitespaces - is this intentional?");
    }

    if (!regex.trim().equals(regex)) {
      return FormValidation
          .warning("Regex is valid, but contains leading and/or trailing whitespaces - is this intentional?");
    }

    return FormValidation.ok();
  }

  public FormValidation doCheckWaitForIdle(final StaplerRequest res, final StaplerResponse rsp,
      @QueryParameter("value") final String waitForIdle) {
    if (Boolean.parseBoolean(waitForIdle))
      return FormValidation.ok();
    else
      return FormValidation
          .warning("This may or may not generate corrupt backups! Be aware that no data get changed during the backup process!");
  }


  @Extension
  public static final class DescriptorImpl
      extends BuildStepDescriptor<Publisher> {

    public DescriptorImpl() {
      this(true);
    }

    protected DescriptorImpl(boolean load) {
      if (load) load();
    }


    private String fullBackupSchedule = "";
    private String diffBackupSchedule = "";
    private String backupPath = "";
    private int nrMaxStoredFull = -1;
    private String excludedFilesRegex = null;
    private boolean waitForIdle = true;
    private int forceQuietModeTimeout = Utils.FORCE_QUIETMODE_TIMEOUT_MINUTES;
    private boolean cleanupDiff = false;
    private boolean moveOldBackupsToZipFile = false;
    private boolean backupBuildResults = true;
    private boolean backupBuildArchive = false;
    private boolean backupPluginArchives = false;
    private boolean backupUserContents = false;
    private boolean backupAdditionalFiles = false;
    private String backupAdditionalFilesRegex = null;
    private boolean backupNextBuildNumber = false;
    private boolean backupBuildsToKeepOnly = false;


    public String getFullBackupSchedule() {
      return fullBackupSchedule;
    }

    @DataBoundSetter
    public void setFullBackupSchedule(String fullBackupSchedule) {
      this.fullBackupSchedule = fullBackupSchedule;
    }

    public String getDiffBackupSchedule() {
      return diffBackupSchedule;
    }

    @DataBoundSetter
    public void setDiffBackupSchedule(String diffBackupSchedule) {
      this.diffBackupSchedule = diffBackupSchedule;
    }

    public String getBackupPath() {
      return backupPath;
    }

    @DataBoundSetter
    public void setBackupPath(String backupPath) {
      this.backupPath = backupPath;
    }

    public int getNrMaxStoredFull() {
      return nrMaxStoredFull;
    }

    @DataBoundSetter
    public void setNrMaxStoredFull(int nrMaxStoredFull) {
      this.nrMaxStoredFull = nrMaxStoredFull;
    }

    public String getExcludedFilesRegex() {
      return excludedFilesRegex;
    }

    @DataBoundSetter
    public void setExcludedFilesRegex(String excludedFilesRegex) {
      this.excludedFilesRegex = excludedFilesRegex;
    }

    public boolean isWaitForIdle() {
      return waitForIdle;
    }

    @DataBoundSetter
    public void setWaitForIdle(boolean waitForIdle) {
      this.waitForIdle = waitForIdle;
    }

    public int getForceQuietModeTimeout() {
      return forceQuietModeTimeout;
    }

    @DataBoundSetter
    public void setForceQuietModeTimeout(int forceQuietModeTimeout) {
      this.forceQuietModeTimeout = forceQuietModeTimeout;
    }

    public boolean isCleanupDiff() {
      return cleanupDiff;
    }

    @DataBoundSetter
    public void setCleanupDiff(boolean cleanupDiff) {
      this.cleanupDiff = cleanupDiff;
    }

    public boolean isMoveOldBackupsToZipFile() {
      return moveOldBackupsToZipFile;
    }

    @DataBoundSetter
    public void setMoveOldBackupsToZipFile(boolean moveOldBackupsToZipFile) {
      this.moveOldBackupsToZipFile = moveOldBackupsToZipFile;
    }

    public boolean isBackupBuildResults() {
      return backupBuildResults;
    }

    @DataBoundSetter
    public void setBackupBuildResults(boolean backupBuildResults) {
      this.backupBuildResults = backupBuildResults;
    }

    public boolean isBackupBuildArchive() {
      return backupBuildArchive;
    }

    @DataBoundSetter
    public void setBackupBuildArchive(boolean backupBuildArchive) {
      this.backupBuildArchive = backupBuildArchive;
    }

    public boolean isBackupPluginArchives() {
      return backupPluginArchives;
    }

    @DataBoundSetter
    public void setBackupPluginArchives(boolean backupPluginArchives) {
      this.backupPluginArchives = backupPluginArchives;
    }

    public boolean isBackupUserContents() {
      return backupUserContents;
    }

    @DataBoundSetter
    public void setBackupUserContents(boolean backupUserContents) {
      this.backupUserContents = backupUserContents;
    }

    public boolean isBackupAdditionalFiles() {
      return backupAdditionalFiles;
    }

    @DataBoundSetter
    public void setBackupAdditionalFiles(boolean backupAdditionalFiles) {
      this.backupAdditionalFiles = backupAdditionalFiles;
    }

    public String getBackupAdditionalFilesRegex() {
      return backupAdditionalFilesRegex;
    }

    @DataBoundSetter
    public void setBackupAdditionalFilesRegex(String backupAdditionalFilesRegex) {
      this.backupAdditionalFilesRegex = backupAdditionalFilesRegex;
    }

    public boolean isBackupNextBuildNumber() {
      return backupNextBuildNumber;
    }

    @DataBoundSetter
    public void setBackupNextBuildNumber(boolean backupNextBuildNumber) {
      this.backupNextBuildNumber = backupNextBuildNumber;
    }

    public boolean isBackupBuildsToKeepOnly() {
      return backupBuildsToKeepOnly;
    }

    @DataBoundSetter
    public void setBackupBuildsToKeepOnly(boolean backupBuildsToKeepOnly) {
      this.backupBuildsToKeepOnly = backupBuildsToKeepOnly;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return "Thin Backup Plugin";
    }


    @Override
    public boolean configure(
        StaplerRequest req,
        JSONObject formData) throws FormException {
      this.fullBackupSchedule = "";
      this.diffBackupSchedule = "";
      this.backupPath = "";
      this.nrMaxStoredFull = -1;
      this.excludedFilesRegex = null;
      this.waitForIdle = true;
      this.forceQuietModeTimeout = Utils.FORCE_QUIETMODE_TIMEOUT_MINUTES;
      this.cleanupDiff = false;
      this.moveOldBackupsToZipFile = false;
      this.backupBuildResults = true;
      this.backupBuildArchive = false;
      this.backupPluginArchives = false;
      this.backupUserContents = false;
      this.backupAdditionalFiles = false;
      this.backupAdditionalFilesRegex = null;
      this.backupNextBuildNumber = false;
      this.backupBuildsToKeepOnly = false;
      req.bindJSON(this, formData);
      save();
      return true;
    }
  }

}
