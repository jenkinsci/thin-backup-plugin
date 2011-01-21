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
