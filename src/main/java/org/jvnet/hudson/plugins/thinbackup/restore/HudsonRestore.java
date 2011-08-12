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
import java.text.ParseException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.backup.BackupSet;
import org.jvnet.hudson.plugins.thinbackup.backup.HudsonBackup;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class HudsonRestore {
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

  private final String backupPath;
  private final File hudsonHome;
  private Date restoreFromDate;

  public HudsonRestore(final File hudsonConfigurationPath, final String backupPath, final Date restoreFromDate) {
    this.hudsonHome = hudsonConfigurationPath;
    this.backupPath = backupPath;
    this.restoreFromDate = restoreFromDate;
  }

  public HudsonRestore(final File hudsonConfigurationPath, final String backupPath, final String restoreBackupFrom) {
    this.hudsonHome = hudsonConfigurationPath;
    this.backupPath = backupPath;
    try {
      restoreFromDate = Utils.DISPLAY_DATE_FORMAT.parse(restoreBackupFrom);
    } catch (final ParseException e) {
      restoreFromDate = null;
    }
  }

  public void restore() {
    if (StringUtils.isEmpty(backupPath)) {
      LOGGER.severe("Backup path not specified for restoration. Aborting.");
      return;
    }
    if (restoreFromDate == null) {
      LOGGER.severe("Backup date to restore from was not specified. Aborting.");
      return;
    }

    try {
      boolean success = restoreFromDirectories(backupPath);
      if (!success) {
        success = restoreFromZipFile();
      }
      if (!success) {
        LOGGER.severe("Could not restore backup.");
      }
    } catch (final IOException ioe) {
      LOGGER.log(Level.SEVERE, "Could not restore backup.", ioe);
    }
  }

  private boolean restoreFromDirectories(final String parentDirectory) throws IOException {
    boolean success = false;

    IOFileFilter suffixFilter = FileFilterUtils.suffixFileFilter(Utils.DIRECTORY_NAME_DATE_FORMAT
        .format(restoreFromDate));
    suffixFilter = FileFilterUtils.andFileFilter(suffixFilter, DirectoryFileFilter.DIRECTORY);

    final File[] candidates = new File(parentDirectory).listFiles((FileFilter) suffixFilter);
    if (candidates.length > 1) {
      LOGGER.severe(String.format("More than one backup with date '%s' found. This is not allowed. Aborting restore.",
          Utils.DISPLAY_DATE_FORMAT.format(restoreFromDate)));
    } else if (candidates.length == 1) {
      final File toRestore = candidates[0];
      if (toRestore.getName().startsWith(BackupType.DIFF.toString())) {
        final File referencedFullBackup = Utils.getReferencedFullBackup(toRestore);
        restore(referencedFullBackup);
      }
      restore(toRestore);
      success = true;
    } else {
      LOGGER.info(String.format(
          "No backup directories with date '%s' found. Will try to find a backup in ZIP files next...",
          Utils.DISPLAY_DATE_FORMAT.format(restoreFromDate)));
    }

    return success;
  }

  private boolean restoreFromZipFile() throws IOException {
    boolean success = false;

    IOFileFilter zippedBackupSetsFilter = FileFilterUtils.prefixFileFilter(BackupSet.BACKUPSET_ZIPFILE_PREFIX);
    zippedBackupSetsFilter = FileFilterUtils.andFileFilter(zippedBackupSetsFilter,
        FileFilterUtils.suffixFileFilter(HudsonBackup.ZIP_FILE_EXTENSION));
    zippedBackupSetsFilter = FileFilterUtils.andFileFilter(zippedBackupSetsFilter, FileFileFilter.FILE);

    final File[] candidates = new File(backupPath).listFiles((FileFilter) zippedBackupSetsFilter);
    for (final File candidate : candidates) {
      final BackupSet backupSet = new BackupSet(candidate);
      if (backupSet.isValid() && backupSet.containsBackupForDate(restoreFromDate)) {
        final BackupSet unzippedBackup = backupSet.unzip();
        if (unzippedBackup.isValid()) {
          success = restoreFromDirectories(backupSet.getUnzipDir().getAbsolutePath());
        }
        backupSet.deleteUnzipDir();
      }
    }

    return success;
  }

  private void restore(final File toRestore) throws IOException {
    FileUtils.copyDirectory(toRestore, this.hudsonHome);
  }

}
