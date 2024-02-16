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

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.ExtensionList;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jvnet.hudson.plugins.thinbackup.utils.EnvironmentVariableNotDefinedException;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.kohsuke.stapler.StaplerRequest;

import jenkins.model.Jenkins;

@Extension
@Symbol("ThinBackup")
public class ThinBackupPluginImpl extends GlobalConfiguration {

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

  private boolean backupConfigHistory = false;
  private boolean backupAdditionalFiles = false;
  private String backupAdditionalFilesRegex = null;
  private boolean backupNextBuildNumber = false;
  private boolean backupBuildsToKeepOnly = false;
  private boolean failFast = true;

  @Override
  public boolean configure(StaplerRequest request, JSONObject jsonObject) {
    return true;
  }

  public ThinBackupPluginImpl() {
    load();
    LOGGER.fine("'thinBackup' plugin initialized.");
  }

  public static ThinBackupPluginImpl get() {
    ExtensionList<GlobalConfiguration> all = GlobalConfiguration.all();
    return all.get(ThinBackupPluginImpl.class);
  }

  public File getHudsonHome() {
    Jenkins jenkins = Jenkins.getInstanceOrNull();
    if (jenkins == null) {
      return null;
    }
    return jenkins.getRootDir();
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


  public boolean isBackupConfigHistory() {
    return backupConfigHistory;
  }

  public void setBackupConfigHistory(boolean backupConfigHistory) {
    this.backupConfigHistory = backupConfigHistory;
  }

  public boolean isFailFast()
  {
    return failFast;
  }

  public void setFailFast(boolean failFast)
  {
    this.failFast = failFast;
  }
}
