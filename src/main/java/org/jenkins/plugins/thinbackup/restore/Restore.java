package org.jenkins.plugins.thinbackup.restore;

import java.io.File;
import java.util.List;

import org.jenkins.plugins.thinbackup.strategies.Strategy;

public abstract class Restore {
  private final File backupDir;
  private final List<? extends Strategy> registeredStrategies;
  
  public Restore(File backupDir, List<? extends Strategy> strategies) {
    this.backupDir = backupDir;
    this.registeredStrategies = strategies; 
  }
  
  public List<? extends Strategy> getRegisteredStrategies() {
    return registeredStrategies;
  }

  public File getBackupDir() {
    return backupDir;
  }
}
