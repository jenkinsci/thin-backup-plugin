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

import hudson.Plugin;
import hudson.scheduler.CronTab;
import hudson.util.FormValidation;

import java.io.File;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import antlr.ANTLRException;

public class ThinBackupPluginImpl extends Plugin {

  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

  private static ThinBackupPluginImpl instance = null;

  private String fullBackupSchedule;
  private String diffBackupSchedule;
  private String backupPath;
  private String nrMaxStoredFull;
  private boolean cleanupDiff;
  private boolean moveOldBackupsToZipFile;
  private boolean backupBuildResults = true;
  private String excludedFilesRegex;

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

  public void setBackupPath(final String backupPath) {
    this.backupPath = backupPath;
  }

  public String getBackupPath() {
    return backupPath;
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

  public void setNrMaxStoredFull(final String nrMaxStoredFull) {
    this.nrMaxStoredFull = nrMaxStoredFull;
  }

  public String getNrMaxStoredFull() {
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

  public void setExcludedFilesRegex(final String excludedFilesRegex) {
    this.excludedFilesRegex = excludedFilesRegex;
  }

  public String getExcludedFilesRegex() {
    return excludedFilesRegex;
  }

  public FormValidation doCheckBackupPath(final StaplerRequest res, final StaplerResponse rsp,
      @QueryParameter("value") final String path) {
    if ((path == null) || path.trim().isEmpty()) {
      return FormValidation.error("Backup path must not be empty.");
    }

    final File backupdir = new File(path);
    if (!backupdir.exists()) {
      return FormValidation.warning("The directory does not exist, but will be created before the first run.");
    }
    if (!backupdir.isDirectory()) {
      return FormValidation.error("A file with this name already exists.");
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
        return FormValidation.warning(message);
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
      Pattern.compile(excludedFilesRegex);
    } catch (final PatternSyntaxException pse) {
      return FormValidation.error("Regex syntax is invalid.");
    }

    if (regex.trim().isEmpty()) {
      return FormValidation.warning("Regex is valid, but consists entirely of whitespaces - is this intentional?");
    }

    if (!regex.trim().equals(regex)) {
      return FormValidation
          .warning("Regex is valid, but contains starting and/or trailing whitespaces - is this intentional?");
    }

    return FormValidation.ok();
  }

}
