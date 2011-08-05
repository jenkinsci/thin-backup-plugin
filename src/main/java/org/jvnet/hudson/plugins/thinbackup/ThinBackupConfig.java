package org.jvnet.hudson.plugins.thinbackup;

import org.apache.commons.lang.StringUtils;

public class ThinBackupConfig {
  private String fullBackupSchedule = "";
  private String diffBackupSchedule = "";
  private String backupPath = "";
  private int nrMaxStoredFull = -1;
  private boolean cleanupDiff = false;
  private boolean moveOldBackupsToZipFile = false;
  private boolean backupBuildResults = true;
  private boolean backupBuildArchive = false;
  private boolean backupNextBuildNumber = false;
  private String excludedFilesRegex = null;

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

  public void setBackupPath(final String backupPath) {
    this.backupPath = backupPath;
  }

  public String getBackupPath() {
    return backupPath;
  }

  public void setNrMaxStoredFull(final int nrMaxStoredFull) {
    this.nrMaxStoredFull = nrMaxStoredFull;
  }

  /**
   * @param nrMaxStoredFull if this string can be parsed as an Integer, nrMaxStoredFull is set to this value, otherwise
   *          it is set to -1.
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

  public void setBackupNextBuildNumber(final boolean backupNextBuildNumber) {
    this.backupNextBuildNumber = backupNextBuildNumber;
  }

  public boolean isBackupNextBuildNumber() {
    return backupNextBuildNumber;
  }

  public void setExcludedFilesRegex(final String excludedFilesRegex) {
    this.excludedFilesRegex = excludedFilesRegex;
  }

  public String getExcludedFilesRegex() {
    return excludedFilesRegex;
  }

}