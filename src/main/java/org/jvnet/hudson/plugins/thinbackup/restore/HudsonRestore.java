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
package org.jvnet.hudson.plugins.thinbackup.restore;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class HudsonRestore {
  private final String backupPath;
  private final String restoreBackupFrom;
  private final File hudsonHome;

  public HudsonRestore(final File hudsonConfigurationPath, final String backupPath, final String restoreBackupFrom) {
    this.hudsonHome = hudsonConfigurationPath;
    this.backupPath = backupPath;
    this.restoreBackupFrom = restoreBackupFrom;
  }

  public void restore() throws IOException {
    IOFileFilter suffixFilter = FileFilterUtils.suffixFileFilter(restoreBackupFrom);
    suffixFilter = FileFilterUtils.andFileFilter(suffixFilter, DirectoryFileFilter.DIRECTORY);

    if (!StringUtils.isEmpty(backupPath)) {
      final File[] candidates = new File(backupPath).listFiles((FileFilter) suffixFilter);
      if (candidates.length == 1) {
        final File toRestore = candidates[0];
        if (toRestore.getName().startsWith(BackupType.DIFF.toString())) {
          restore(Utils.getReferencedFullBackup(toRestore));
        }
        restore(toRestore);
      }
    }
  }

  private void restore(final File toRestore) throws IOException {
    FileUtils.copyDirectory(toRestore, this.hudsonHome);
  }

}
