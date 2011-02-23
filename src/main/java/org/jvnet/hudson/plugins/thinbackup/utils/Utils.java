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
package org.jvnet.hudson.plugins.thinbackup.utils;

import hudson.model.Computer;
import hudson.model.Hudson;

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.backup.BackupSet;

public class Utils {
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

  private static final int COMPUTER_TIMEOUT_WAIT_IN_MS = 500;
  private static SimpleDateFormat DIRECTORY_NAME_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
  private static SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
  private static final String DIRECTORY_NAME_DATE_EXTRACTION_REGEX = String.format("(%s|%s)-", BackupType.FULL,
      BackupType.DIFF);

  /**
   * Waits until all Hudson slaves are idle.
   */
  public static void waitUntilIdle() {
    final Computer computers[] = Hudson.getInstance().getComputers();

    boolean running;
    do {
      running = false;
      for (final Computer computer : computers) {
        if (computer.countBusy() != 0) {
          running = true;
          break;
        }
      }

      try {
        Thread.sleep(COMPUTER_TIMEOUT_WAIT_IN_MS);
      } catch (final InterruptedException e) {
        LOGGER.log(Level.WARNING, e.getMessage(), e);
      }
    } while (running);
  }

  /**
   * @param displayFormattedDate
   *          a String in the display format date
   * @return the string formatted in the directory names' date format
   * @throws ParseException
   */
  public static String convertToDirectoryNameDateFormat(final String displayFormattedDate) throws ParseException {
    final Date displayDate = DISPLAY_DATE_FORMAT.parse(displayFormattedDate);
    return DIRECTORY_NAME_DATE_FORMAT.format(displayDate);
  }

  /**
   * @param parent
   * @param backupType
   * @param date
   * @return a directory created in the given parent directory with a name formatted like
   *         "<BACKUP_TYPE>-yyyy-MM-dd_HH-mm".
   */
  public static File getFormattedDirectory(final File parent, final BackupType backupType, final Date date) {
    final File formattedDirectory = new File(parent, String.format("%s-%s", backupType,
        DIRECTORY_NAME_DATE_FORMAT.format(date)));
    return formattedDirectory;
  }

  /**
   * @param parentDir
   * @param backupType
   * @return an (unordered) list of backup directories of the given backup type.
   */
  public static List<File> getBackupTypeDirectories(final File parentDir, final BackupType backupType) {
    IOFileFilter prefixFilter = FileFilterUtils.prefixFileFilter(backupType.toString());
    prefixFilter = FileFilterUtils.andFileFilter(prefixFilter, DirectoryFileFilter.DIRECTORY);
    return Arrays.asList(parentDir.listFiles((FilenameFilter) prefixFilter));
  }

  /**
   * @param diffBackup
   * @return the full backup referenced by the given diff backup, or null if none can be found.
   */
  public static File getReferencedFullBackup(final File diffBackup) {
    if (diffBackup.getName().startsWith(BackupType.FULL.toString())) {
      return diffBackup;
    }

    final Collection<File> backups = getBackupTypeDirectories(diffBackup.getParentFile(), BackupType.FULL);

    if (backups.isEmpty()) {
      return null;
    }

    File referencedFullBackup = null;

    final Date curModifiedDate = new Date(diffBackup.lastModified());
    Date closestPreviousBackupDate = new Date(0);
    for (final File fullBackupDir : backups) {
      final Date tmpModifiedDate = new Date(fullBackupDir.lastModified());
      if (tmpModifiedDate.before(curModifiedDate) && tmpModifiedDate.after(closestPreviousBackupDate)) {
        closestPreviousBackupDate = tmpModifiedDate;
        referencedFullBackup = fullBackupDir;
      }
    }

    return referencedFullBackup;
  }

  /**
   * @param fullBackup
   * @return a list of all diff backups which reference the given full backup.
   */
  public static List<File> getReferencingDiffBackups(final File fullBackup) {
    final List<File> diffBackups = new ArrayList<File>();
    if (fullBackup.getName().startsWith(BackupType.DIFF.toString())) {
      return diffBackups;
    }

    final Collection<File> allDiffBackups = getBackupTypeDirectories(fullBackup.getParentFile(), BackupType.DIFF);

    for (final File diffBackup : allDiffBackups) {
      final File tmpFullBackup = getReferencedFullBackup(diffBackup);
      if ((tmpFullBackup != null) && (tmpFullBackup.getAbsolutePath().equals(fullBackup.getAbsolutePath()))) {
        diffBackups.add(diffBackup);
      }
    }

    return diffBackups;
  }

  /**
   * @param directory
   * @return a list of backups (both FULL and DIFF) in the given directory, ordered descending by the date encoded in
   *         the directories' name.
   */
  public static List<String> getBackups(final File directory) {
    IOFileFilter filter = FileFilterUtils.prefixFileFilter(BackupType.FULL.toString());
    filter = FileFilterUtils.orFileFilter(filter, FileFilterUtils.prefixFileFilter(BackupType.DIFF.toString()));
    filter = FileFilterUtils.andFileFilter(filter, DirectoryFileFilter.DIRECTORY);
    final String[] backups = directory.list(filter);

    final List<String> list = new ArrayList<String>(backups.length);
    for (final String name : backups) {
      try {
        final String dateOnly = name.replaceFirst(DIRECTORY_NAME_DATE_EXTRACTION_REGEX, "");
        final Date tmp = DIRECTORY_NAME_DATE_FORMAT.parse(dateOnly);
        list.add(DISPLAY_DATE_FORMAT.format(tmp));
      } catch (final ParseException e) {
        LOGGER.warning("Cannot parse directory name '" + name
            + "', therefore it will not show up in the list of available backups.");
      }
    }
    Collections.sort(list);
    Collections.reverse(list);

    return list;
  }

  /**
   * @param directory
   * @return a list of valid (@see BackupSet#isValid) backup sets in the given directory, ordered ascending by the last
   *         modified date of the BackupSets' full backup.
   */
  public static List<BackupSet> getValidBackupSets(final File directory) {
    final Collection<File> backups = Utils.getBackupTypeDirectories(directory, BackupType.FULL);

    final List<BackupSet> validSets = new ArrayList<BackupSet>();
    for (final File backup : backups) {
      final BackupSet set = new BackupSet(backup);
      if (set.isValid()) {
        validSets.add(set);
      }
    }
    Collections.sort(validSets);
    return validSets;
  }

}
