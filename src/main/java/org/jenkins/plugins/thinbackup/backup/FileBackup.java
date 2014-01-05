package org.jenkins.plugins.thinbackup.backup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jenkins.plugins.thinbackup.exceptions.BackupException;
import org.jenkins.plugins.thinbackup.strategies.Strategy;

public final class FileBackup extends Backup {

  public FileBackup(File backupDir, List<? extends Strategy> strategies) {
    super(backupDir, strategies);
  }

  public void backup() throws BackupException {
    Collection<File> toBackup = new ArrayList<File>(); 
    for (Strategy strategy : getRegisteredStrategies()) {
      toBackup.addAll(strategy.backup());
    }
    
    for (File file : toBackup) {
      try {
        if (file.isDirectory())
          FileUtils.copyDirectoryToDirectory(file, getBackupDir());
        else 
          FileUtils.copyFileToDirectory(file, getBackupDir());
      } catch (IOException e) {
        throw new BackupException("Cannot backup "+ file.getName() +". ", e);
      }
    }
  }

}
