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

  public void setMoveOldBackupsToZipFile(boolean moveOldBackupsToZipFile) {
    this.moveOldBackupsToZipFile = moveOldBackupsToZipFile;
  }

  public boolean isMoveOldBackupsToZipFile() {
    return moveOldBackupsToZipFile;
  }

  public FormValidation doCheckBackupPath(final StaplerRequest res, final StaplerResponse rsp,
      @QueryParameter("value") final String value) {
    if ((value == null) || value.isEmpty()) {
      return FormValidation.error("'Backup Path' is not mandatory.");
    }

    final File backupdir = new File(value);
    if (!backupdir.exists()) {
      return FormValidation.warning("The given directory does not exist, it will be created during the first run.");
    }
    if (!backupdir.isDirectory()) {
      return FormValidation.error("A file with this name already exists.");
    }
    return FormValidation.ok();
  }

  public FormValidation doCheckFullBackupSchedule(final StaplerRequest res, final StaplerResponse rsp,
      @QueryParameter("fullBackupSchedule") final String backupSchedule) {
    return validateCronSchedule(backupSchedule);
  }

  public FormValidation doCheckDiffBackupSchedule(final StaplerRequest res, final StaplerResponse rsp,
      @QueryParameter("diffBackupSchedule") final String backupSchedule) {
    return validateCronSchedule(backupSchedule);
  }

  private FormValidation validateCronSchedule(final String backupTime) {
    if ((backupTime != null) && !backupTime.isEmpty()) {
      String message;
      try {
        message = new CronTab(backupTime).checkSanity();
      } catch (final ANTLRException e) {
        return FormValidation.error(e.getMessage());
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

}
