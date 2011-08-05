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
import java.io.IOException;
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
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.backup.BackupSet;

public class Utils {
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

  private static final int COMPUTER_TIMEOUT_WAIT_IN_MS = 500;
  private static final String DIRECTORY_NAME_DATE_EXTRACTION_REGEX = String.format("(%s|%s)-", BackupType.FULL,
      BackupType.DIFF);

  public static SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
  public static SimpleDateFormat DIRECTORY_NAME_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
  public static final String THINBACKUP_TMP_DIR = System.getProperty("java.io.tmpdir") + File.separator
      + "thinBackupTmpDir";

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
   * @param directory
   * @return the date component of a directory name formatted in the thinBackup standard
   */
  public static Date getDateFromBackupDirectory(final File directory) {
    return getDateFromBackupDirectoryName(directory.getName());
  }

  /**
   * @param directoryName
   * @return the date component of a directory name formatted in the thinBackup standard
   */
  public static Date getDateFromBackupDirectoryName(final String directoryName) {
    Date result = null;

    try {
      if (directoryName.startsWith(BackupType.FULL.toString()) || directoryName.startsWith(BackupType.DIFF.toString())) {
        final String dateOnly = directoryName.replaceFirst(DIRECTORY_NAME_DATE_EXTRACTION_REGEX, "");
        if (!dateOnly.isEmpty()) {
          result = DIRECTORY_NAME_DATE_FORMAT.parse(dateOnly);
        }
      }
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, String.format("Could not parse directory name '%s'.", directoryName));
    }

    return result;
  }

  /**
   * @param displayFormattedDate a String in the display format date
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
   * @return a reference to a file in the given parent directory with a name formatted like
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
   * @return an unordered list of backup directories of the given backup type.
   */
  public static List<File> getBackupTypeDirectories(final File parentDir, final BackupType backupType) {
    IOFileFilter prefixFilter = FileFilterUtils.prefixFileFilter(backupType.toString());
    prefixFilter = FileFilterUtils.andFileFilter(prefixFilter, DirectoryFileFilter.DIRECTORY);

    final File[] existingDirs = parentDir.listFiles((FilenameFilter) prefixFilter);
    if (existingDirs == null) {
      return Collections.emptyList();
    }

    return Arrays.asList(existingDirs);
  }

  /**
   * @param parentDir
   * @return an unordered list of zipped backupsets in the given directory.
   */
  public static List<File> getBackupSetZipFiles(final File parentDir) {
    IOFileFilter zipFileFilter = FileFilterUtils.prefixFileFilter(BackupSet.BACKUPSET_ZIPFILE_PREFIX);
    zipFileFilter = FileFilterUtils.andFileFilter(zipFileFilter,
        FileFilterUtils.suffixFileFilter(BackupSet.BACKUPSET_ZIPFILE_SUFFIX));
    zipFileFilter = FileFilterUtils.andFileFilter(zipFileFilter, FileFileFilter.FILE);

    final File[] existingZips = parentDir.listFiles((FilenameFilter) zipFileFilter);
    if (existingZips == null) {
      return Collections.emptyList();
    }

    return Arrays.asList(existingZips);
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

    final Date curBackupDate = getDateFromBackupDirectory(diffBackup);
    if (curBackupDate != null) {
      Date closestPreviousBackupDate = new Date(0);
      for (final File fullBackupDir : backups) {
        final Date tmpBackupDate = getDateFromBackupDirectory(fullBackupDir);
        if ((tmpBackupDate != null)
            && (tmpBackupDate.after(closestPreviousBackupDate) && (tmpBackupDate.getTime() <= curBackupDate.getTime()))) {
          closestPreviousBackupDate = tmpBackupDate;
          referencedFullBackup = fullBackupDir;
        }
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
   * @return a list of backups in the given directory (both FULL and DIFF), displayed as the respective backup date,
   *         from both directories and ZIP files, ordered descending by the date encoded in the backups' name.
   */
  public static List<String> getBackupsAsDates(final File directory) {
    final List<String> backupDates = new ArrayList<String>();

    final List<BackupSet> backupSets = getValidBackupSets(directory);
    for (final BackupSet backupSet : backupSets) {
      final String fullName = backupSet.getFullBackupName();
      try {
        final Date tmp = getDateFromBackupDirectoryName(fullName);
        if (tmp != null) {
          backupDates.add(DISPLAY_DATE_FORMAT.format(tmp));
        } else {
          throw new ParseException("", 0);
        }
      } catch (final ParseException e) {
        LOGGER.warning(String.format(
            "Cannot parse directory name '%s' , therefore it will not show up in the list of available backups.",
            fullName));
      }

      for (final String diffName : backupSet.getDiffBackupsNames()) {
        try {
          final Date tmp = getDateFromBackupDirectoryName(diffName);
          if (tmp != null) {
            backupDates.add(DISPLAY_DATE_FORMAT.format(tmp));
          } else {
            throw new ParseException("", 0);
          }
        } catch (final ParseException e) {
          LOGGER.warning(String.format(
              "Cannot parse directory name '%s' , therefore it will not show up in the list of available backups.",
              diffName));
        }
      }
    }

    Collections.sort(backupDates);
    Collections.reverse(backupDates);

    return backupDates;
  }

  /**
   * @param directory
   * @return a list of valid (@see BackupSet#isValid) backup sets that exists as directories (not as ZIP files) in the
   *         given directory, ordered ascending by the backup date of the BackupSets' full backup.
   */
  public static List<BackupSet> getValidBackupSetsFromDirectories(final File directory) {
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

  /**
   * @param directory
   * @return a list of valid (@see BackupSet#isValid) backup sets that exists as ZIP files (not as directories) in the
   *         given directory, ordered ascending by the backup date of the BackupSets' full backup.
   */
  public static List<BackupSet> getValidBackupSetsFromZips(final File directory) {
    final Collection<File> backups = Utils.getBackupSetZipFiles(directory);

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

  /**
   * @param directory
   * @return a list of valid (@see BackupSet#isValid) backup sets in the given directory, ordered ascending by the
   *         backup date of the BackupSets' full backup.
   */
  public static List<BackupSet> getValidBackupSets(final File directory) {
    final List<BackupSet> validSets = new ArrayList<BackupSet>();

    validSets.addAll(getValidBackupSetsFromDirectories(directory));
    validSets.addAll(getValidBackupSetsFromZips(directory));
    Collections.sort(validSets);

    return validSets;
  }

  /**
   * Moves all backup sets (that are not already zipped) other than the one containing currentBackup to ZIP files
   * located in backupRoot.
   * 
   * @param backupRoot
   * @param currentBackup specified which backup should be omitted from being moved. If null, all backups are moved to
   *          ZIP files.
   * @throws IOException
   */
  public static void moveOldBackupsToZipFile(final File backupRoot, final File currentBackup) throws IOException {
    LOGGER.fine("Moving old backups to zip files...");

    final List<BackupSet> validBackupSets = Utils.getValidBackupSetsFromDirectories(backupRoot);
    int numberOfZippedBackupSets = 0;
    int numberOfMovedBackupSets = 0;
    for (final BackupSet backupSet : validBackupSets) {
      if ((!backupSet.containsDirectory(currentBackup)) && (!backupSet.isInZipFile())) {
        final File zippedBackupSet = backupSet.zipTo(backupRoot);
        ++numberOfZippedBackupSets;
        if (zippedBackupSet != null) {
          LOGGER.fine(String.format("Successfully zipped backup set %s to '%s'.", backupSet,
              zippedBackupSet.getAbsolutePath()));
          try {
            backupSet.delete();
            LOGGER.fine(String.format("Deleted backup set %s after zipping it.", backupSet));
            ++numberOfMovedBackupSets;
          } catch (final IOException ioe) {
            LOGGER.log(Level.WARNING, String.format("Could not delete backup set %s.", backupSet));
          }
        }
      }
    }

    if (numberOfMovedBackupSets == numberOfZippedBackupSets) {
      LOGGER.info(String.format("DONE moving %d backup set(s) to ZIP files.", numberOfMovedBackupSets));
    } else {
      LOGGER
          .info(String
              .format(
                  "DONE zipping %d backup set(s). %d of those could be moved to ZIP files, the rest remain as files/directories as well.",
                  numberOfZippedBackupSets, numberOfMovedBackupSets));
    }
  }
}
