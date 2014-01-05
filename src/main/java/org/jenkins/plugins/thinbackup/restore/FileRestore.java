package org.jenkins.plugins.thinbackup.restore;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jenkins.plugins.thinbackup.exceptions.RestoreException;
import org.jenkins.plugins.thinbackup.strategies.Strategy;

public final class FileRestore extends Restore {
  public FileRestore(File backupDir, List<? extends Strategy> strategies) {
    super(backupDir, strategies);
  }

  public void restore() throws RestoreException {
    Collection<File> toRestore = FileUtils.listFiles(getBackupDir(), null, true);
    
    for (Strategy strategy : getRegisteredStrategies()) {
      strategy.restore(toRestore);
    }
  }
}
