package org.jenkins.plugins.thinbackup.backup;

import java.io.File;
import java.util.List;

import org.jenkins.plugins.thinbackup.exceptions.BackupException;
import org.jenkins.plugins.thinbackup.strategies.Strategy;

public abstract class Backup {
  private final File backupDir;
  private final List<? extends Strategy> registeredStrategies;
  
  public Backup(File backupDir, List<? extends Strategy> strategies) {
    this.backupDir = backupDir;
    this.registeredStrategies = strategies; 
  }

  public File getBackupDir() {
    return backupDir;
  }

  public List<? extends Strategy> getRegisteredStrategies() {
    return registeredStrategies;
  }
  
  public abstract void backup() throws BackupException;
}
