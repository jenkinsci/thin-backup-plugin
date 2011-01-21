/*
 * The MIT License
 *
 * Copyright (c) 2011, Borland (a Micro Focus Company), Matthias Steinkogler, Thomas Fuerer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
      LOGGER.info(String.format("BackupSet: Backup '%s' has no referenced full backup available.", initial.getName()));

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
