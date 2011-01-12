package org.jvnet.hudson.plugins.thinbackup;

import hudson.Plugin;

import java.util.logging.Logger;

public class ThinBackupPluginImpl extends Plugin {
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");
  private static ThinBackupPluginImpl instance = null;

  private String fullBackupSchedule;
  private String diffBackupSchedule;
  private String backupPath;
  private String nrMaxStoredFull;
  private boolean cleanupDiff;

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

  public void setCleanupDiff(final boolean cleanupDiff) {
    this.cleanupDiff = cleanupDiff;
  }

  public boolean isCleanupDiff() {
    return cleanupDiff;
  }

  public void setNrMaxStoredFull(String nrMaxStoredFull) {
    this.nrMaxStoredFull = nrMaxStoredFull;
  }

  public String getNrMaxStoredFull() {
    return nrMaxStoredFull;
  }

}
