/**
 *  Copyright (C) 2011  Matthias Steinkogler, Thomas F�rer
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
package org.jvnet.hudson.plugins.thinbackup.backup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class BackupSet implements Comparable<BackupSet> {
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

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
      LOGGER
          .warning(String.format("BackupSet: Backup '%s' has no referenced full backup available.", initial.getName()));

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
