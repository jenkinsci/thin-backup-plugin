package org.jvnet.hudson.plugins.thinbackup.backup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class BackupSet implements Comparable<BackupSet> {
  private File fullBackup;
  private List<File> diffBackups;

  public BackupSet(final File initial) {
    fullBackup = null;
    diffBackups = null;

    File tmpFull = null;
    if (initial.getName().startsWith(BackupType.FULL.toString())) {
      tmpFull = initial;
    } else if (initial.getName().startsWith(BackupType.DIFF.toString())) {
      tmpFull = Utils.getReferencedFullBackup(initial);
    }
    fullBackup = tmpFull;

    if (fullBackup != null) {
      diffBackups = Utils.getReferencingDiffBackups(fullBackup);
    } else if (initial.getName().startsWith(BackupType.DIFF.toString())) {
      diffBackups = new ArrayList<File>(1);
      diffBackups.add(initial);
    }
  }

  /**
   * @return true if this backup set has a referenced full backup.
   */
  public boolean isValid() {
    return (fullBackup != null);
  }

  public void delete() throws IOException {
    if (fullBackup != null) {
      FileUtils.deleteDirectory(fullBackup);
      fullBackup = null;
    }
    if (diffBackups != null) {
      for (final File diffBackup : diffBackups) {
        FileUtils.deleteDirectory(diffBackup);
      }
      diffBackups = null;
    }
  }

  public File getFullBackup() {
    return fullBackup;
  }

  public List<File> getDiffBackups() {
    return diffBackups;
  }

  public int compareTo(final BackupSet other) {
    final File otherFullBackup = other.getFullBackup();
    if ((fullBackup == null) && (otherFullBackup == null)) {
      return 0;
    } else if (fullBackup == null) {
      return -1;
    } else if (otherFullBackup == null) {
      return 1;
    }

    final long thisLastModified = fullBackup.lastModified();
    final long otherLastModified = otherFullBackup.lastModified();

    if (thisLastModified == otherLastModified) {
      return 0;
    } else if (thisLastModified < otherLastModified) {
      return -1;
    } else {
      return 1;
    }
  }

}
