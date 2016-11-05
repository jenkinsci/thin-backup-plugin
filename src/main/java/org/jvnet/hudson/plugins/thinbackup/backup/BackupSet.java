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
package org.jvnet.hudson.plugins.thinbackup.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

/**
 * A BackupSet contains references to a full and zero or more diff backups. It can be created from either a reference to
 * a full or diff backup directory or from a reference to a ZIP file. If it is created from a ZIP file, the BackupSet
 * must be unzipped before the data contained can be actually accessed (it is never unzipped automatically).
 */
public class BackupSet implements Comparable<BackupSet> {
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

  public static final String BACKUPSET_ZIPFILE_PREFIX = "BACKUPSET";

  private boolean inZipFile = false;

  private File backupSetzipFile = null;
  private File unzipDir = null;

  private File fullBackup;
  private String fullBackupName = null;
  private List<File> diffBackups;
  private List<String> diffBackupsNames;

  /**
   * @param initial either a FULL or DIFF backup directory, or a BackupSet ZIP file.
   */
  public BackupSet(final File initial) {
    fullBackup = null;
    diffBackups = null;

    boolean success = false;

    final String name = initial.getName();
    if ((name.startsWith(BACKUPSET_ZIPFILE_PREFIX)) && (name.endsWith(HudsonBackup.ZIP_FILE_EXTENSION))) {
      inZipFile = true;
      backupSetzipFile = initial;
    } else {
      if (name.startsWith(BackupType.FULL.toString())) {
        fullBackup = initial;
      } else if (name.startsWith(BackupType.DIFF.toString())) {
        fullBackup = Utils.getReferencedFullBackup(initial);
      }
    }

    success = initialize();

    if (!success) {
      LOGGER.warning(String
          .format("Could not initialize backup set from file/directory '%s' as it is not valid.", name));
    }
  }

  private boolean initialize() {
    boolean success = false;

    diffBackupsNames = new ArrayList<String>();

    if (inZipFile) {
      success = initializeFromZipFile();
    } else {
      success = initializeFromDirs();
    }

    if (!success) {
      fullBackup = null;
      fullBackupName = null;
      if (diffBackups != null) {
        diffBackups.clear();
      }
      diffBackups = null;
      if (diffBackupsNames != null) {
        diffBackupsNames.clear();
      }
      diffBackupsNames = null;
    }

    if (success && (diffBackupsNames != null)) {
      Collections.sort(diffBackupsNames);
    }

    return success;
  }

  private boolean initializeFromZipFile() {
    boolean success = true;

    ZipFile zipFile = null;
    try {
      Utils.waitUntilFileCanBeRead(backupSetzipFile);
      zipFile = new ZipFile(backupSetzipFile);
      final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
      while (zipEntries.hasMoreElements() && success) {
        final ZipEntry entry = zipEntries.nextElement();
        String tmpName = entry.getName();
        tmpName = tmpName.substring(0, tmpName.indexOf(File.separator));
        if (tmpName.startsWith(BackupType.FULL.toString())) {
          if ((fullBackupName == null) || fullBackupName.equals(tmpName)) {
            fullBackupName = tmpName;
          } else {
            LOGGER.warning(String.format("Backup set '%s' contains multiple full backups and is therefore not valid.",
                zipFile.getName()));
            success = false;
          }
        } else if (tmpName.startsWith(BackupType.DIFF.toString()) &&
                   !diffBackupsNames.contains(tmpName)) {
          diffBackupsNames.add(tmpName);
        }
      }
    } catch (final IOException e) {
      LOGGER.log(Level.SEVERE,
          String.format("Cannot initialize BackupSet from ZIP file '%s'.", backupSetzipFile.getName()), e);
      success = false;
    } finally {
      try {
        if (zipFile != null) {
          zipFile.close();
        }
      } catch (final IOException e) {
        LOGGER.log(Level.SEVERE, String.format("Cannot close ZIP file '%s'.", backupSetzipFile.getName()), e);
        success = false;
      }
    }

    return success;
  }

  private boolean initializeFromDirs() {
    boolean success = false;

    if (fullBackup != null) {
      fullBackupName = fullBackup.getName();
      diffBackups = Utils.getReferencingDiffBackups(fullBackup);
      success = true;
    }
    if (success && (diffBackups != null) && !diffBackups.isEmpty()) {
      diffBackupsNames = new ArrayList<String>(diffBackups.size());
      for (final File diffBackup : diffBackups) {
        final String tmpName = diffBackup.getName();
        if (!diffBackupsNames.contains(tmpName)) {
          diffBackupsNames.add(tmpName);
        } else {
          LOGGER
              .warning("Backup set contains multiple diff backups with the same name. This is not allowed; backup set is invalid.");
          success = false;
        }
      }
    }

    return success;
  }

  /**
   * @return true if this backup set has a referenced full backup.
   */
  public boolean isValid() {
    return (fullBackupName != null);
  }

  public void delete() throws IOException {
    if (isValid()) {
      if (!inZipFile) {
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
      } else {
        FileUtils.deleteQuietly(backupSetzipFile);
        deleteUnzipDir();
      }
    }
  }

  /**
   * Deletes the directory used for unzipping this BackupSet. Note that this will make BackupSets created from that
   * directory no longer usable.
   */
  public void deleteUnzipDir() {
    if ((unzipDir != null) && (unzipDir.exists())) {
      try {
        FileUtils.deleteDirectory(unzipDir);
      } catch (final IOException e) {
        LOGGER.warning(String.format("Could not delete unzipping directory '%s'. Please delete manually.",
            unzipDir.getAbsolutePath()));
      }
    }
  }

  @Override
  public String toString() {
    final StringBuffer strBuf = new StringBuffer();

    strBuf.append("[FULL backup: ");
    if (fullBackupName != null) {
      strBuf.append(fullBackupName);
    } else {
      strBuf.append("NONE");
    }
    strBuf.append("; DIFF backups: ");
    boolean hasDiffs = false;
    if (diffBackupsNames != null) {
      for (final String diffBackup : diffBackupsNames) {
        strBuf.append(diffBackup);
        strBuf.append(",");
      }
      if (diffBackupsNames.size() > 0) {
        strBuf.deleteCharAt(strBuf.length() - 1);
        hasDiffs = true;
      }
    }
    if (!hasDiffs) {
      strBuf.append("NONE");
    }
    strBuf.append("]");

    return strBuf.toString();
  }

  /**
   * Unzips this backup set if it was initialized from a ZIP file. Does does NOT change <i>this</i>. Unzip location is
   * Utils.THINBACKUP_TMP_DIR. deleteUnzipDir() may be called if the unzipped BackupSet is no longer needed. Before
   * using the returned BackupSet, it should be checked if it is valid.
   * 
   * @return a new BackupSet referencing the unzipped directories, or the current BackupSet if it was either not created
   *         from a ZIP file or is invalid. In case of an error an invalid BackupSet is returned.
   * @throws IOException - if an I/O error occurs.
   */
  public BackupSet unzip() throws IOException {
    BackupSet result = null;

    if (inZipFile && isValid()) {
      result = unzipTo(new File(Utils.THINBACKUP_TMP_DIR));
    } else {
      result = this;
    }

    return result;
  }

  /**
   * Unzips this backup set into a directory within the specified directory if it was initialized from a ZIP file. Does
   * NOT change <i>this</i>. deleteUnzipDir() may be called if the unzipped BackupSet is no longer needed. The directory
   * the BackupSet was unzipped to can be retrieved with getUnzipDir(). Before using the returned BackupSet, it should
   * be checked if it is valid.
   * 
   * @param directory target directory
   * @return a new BackupSet referencing the unzipped directories, or the current BackupSet if it was either not created
   *         from a ZIP file or is invalid. In case of an error an invalid BackupSet is returned.
   * @throws IOException - if an I/O error occurs.
   */
  public BackupSet unzipTo(final File directory) throws IOException {
    BackupSet result = null;

    if (inZipFile && isValid()) {
      if (!directory.exists()) {
        directory.mkdirs();
      }
      unzipDir = new File(directory, getBackupSetZipFileName().replace(HudsonBackup.ZIP_FILE_EXTENSION, ""));
      if (!unzipDir.exists()) {
        unzipDir.mkdirs();
      }

      final ZipFile zipFile = new ZipFile(backupSetzipFile);
      final byte data[] = new byte[DirectoriesZipper.BUFFER_SIZE];
      final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
      while (zipEntries.hasMoreElements()) {
        final ZipEntry entry = zipEntries.nextElement();

        final String fullPathToEntry = entry.getName();
        final String pathToEntry = fullPathToEntry.substring(0, fullPathToEntry.lastIndexOf(File.separator));
        final File entryDir = new File(unzipDir, pathToEntry);
        entryDir.mkdirs();
        final String entryName = fullPathToEntry.substring(fullPathToEntry.lastIndexOf(File.separator) + 1);

        final BufferedInputStream is = new BufferedInputStream(zipFile.getInputStream(entry));

        final FileOutputStream fos = new FileOutputStream(new File(unzipDir + File.separator + pathToEntry, entryName));
        final BufferedOutputStream dest = new BufferedOutputStream(fos, DirectoriesZipper.BUFFER_SIZE);

        int count = 0;
        while ((count = is.read(data)) != -1) {
          dest.write(data, 0, count);
        }

        dest.flush();
        dest.close();
      }
      zipFile.close();

      final File[] backups = unzipDir.listFiles();
      if (backups.length > 0) {
        result = new BackupSet(backups[0]);
      } else {
        // in case of an error (i.e. nothing was unzipped) return an invalid BackupSet
        result = new BackupSet(unzipDir);
      }
    } else {
      result = this;
    }

    return result;
  }

  /**
   * @param directory target directory
   * @return a reference to the created ZIP file, the current ZIP file if the BackupSet was created from one (because no
   *         zipping is performed in this case), or null if this BackupSet is invalid.
   */
  public File zipTo(final File directory) {
    File zipFile = null;

    if (isValid()) {
      if (!inZipFile) {
        DirectoriesZipper zipper = null;
        try {
          if (!directory.exists()) {
            final boolean success = directory.mkdirs();
            if (!success) {
              throw new IOException(String.format("Could not create directory '%s'.", directory.getAbsolutePath()));
            }
          }

          final String zipFileName = getBackupSetZipFileName();
          zipFile = new File(directory, zipFileName);
          zipper = new DirectoriesZipper(zipFile);

          zipper.addToZip(getFullBackup());
          for (final File diffBackup : getDiffBackups()) {
            zipper.addToZip(diffBackup);
          }
        } catch (final IOException ioe) {
          LOGGER.log(Level.SEVERE, "Could not zip backup set.", ioe);
        } finally {
          try {
            zipper.close();
          } catch (final IOException ioe) {
            LOGGER.log(Level.SEVERE, "Could not zip backup set.", ioe);
          }
        }
      } else {
        zipFile = backupSetzipFile;
      }
    }

    return zipFile;
  }

  private String getBackupSetZipFileName() {
    return String.format("%s_%s_%s%s", BACKUPSET_ZIPFILE_PREFIX, getFormattedFullBackupDate(),
        getFormattedLatestDiffBackupDate(), HudsonBackup.ZIP_FILE_EXTENSION);
  }

  private String getFormattedFullBackupDate() {
    String result = "";

    final Date tmp = Utils.getDateFromBackupDirectoryName(fullBackupName);
    if (tmp != null) {
      result = Utils.DIRECTORY_NAME_DATE_FORMAT.format(tmp);
    }

    return result;
  }

  private String getFormattedLatestDiffBackupDate() {
    String result = "";

    if ((diffBackupsNames != null) && (diffBackupsNames.size() > 0)) {
      final Date tmp = Utils.getDateFromBackupDirectoryName(diffBackupsNames.get(diffBackupsNames.size() - 1));
      if (tmp != null) {
        result = Utils.DIRECTORY_NAME_DATE_FORMAT.format(tmp);
      }
    }

    return result;
  }

  /**
   * Compares the backup sets by using the sets' associated full backups' backup date.
   * 
   * @return -1 if this BackupSet's full backup date is before the other's, 0 if they are equal, 1 if is after the
   *         other's.
   */
  @Override
  public int compareTo(final BackupSet other) {
    final String otherFullBackupName = other.getFullBackupName();
    if ((other == this) || ((fullBackupName == null) && (otherFullBackupName == null))) {
      return 0;
    } else if (fullBackupName == null) {
      return -1;
    } else if (otherFullBackupName == null) {
      return 1;
    }

    return fullBackupName.compareTo(otherFullBackupName);
  }

  /**
   * @param directory directory to search for
   * @return true if this BackupSet contains a backup directory with the same name or if directory is null. Note that
   *         only the top level backup directories are checked, not any other contents of the BackupSet.
   */
  public boolean containsDirectory(final File directory) {
    if ((directory == null) || (!directory.isDirectory()) || !isValid()) {
      return false;
    }

    final String directoryName = directory.getName();

    boolean inDiffs = false;
    if (diffBackupsNames != null) {
      for (final String diffBackupName : diffBackupsNames) {
        inDiffs = directoryName.equals(diffBackupName);
        if (inDiffs) {
          break;
        }
      }
    }

    return (inDiffs || directoryName.equals(fullBackupName));
  }

  /**
   * @param date date to search for
   * @return true if a backup for the given date exists in this BackupSet.
   */
  public boolean containsBackupForDate(final Date date) {
    if ((date == null) || !isValid()) {
      return false;
    }

    Date tmp = null;

    boolean inDiffs = false;
    if (diffBackupsNames != null) {
      for (final String diffBackupName : diffBackupsNames) {
        tmp = Utils.getDateFromBackupDirectoryName(diffBackupName);
        inDiffs = (tmp != null) && date.equals(tmp);
        if (inDiffs) {
          break;
        }
      }
    }

    tmp = Utils.getDateFromBackupDirectoryName(fullBackupName);
    return (inDiffs || ((tmp != null) && date.equals(tmp)));
  }

  /**
   * @return true if this BackupSet was constructed from a ZIP file.
   */
  public boolean isInZipFile() {
    return inZipFile;
  }

  /**
   * @return the full backup directory reference, or null if this BackupSet is in a ZIP file.
   */
  public File getFullBackup() {
    return fullBackup;
  }

  /**
   * @return the name of full backup directory.
   */
  public String getFullBackupName() {
    return fullBackupName;
  }

  /**
   * @return a List of diff backup directory references, or null if this BackupSet is in a ZIP file.
   */
  public List<File> getDiffBackups() {
    return diffBackups;
  }

  /**
   * @return a List of diff backup directory names.
   */
  public List<String> getDiffBackupsNames() {
    return diffBackupsNames;
  }

  /**
   * @return a reference to the directory where this BackupSet was unzipped, or null if it either was not yet unzipped
   *         or if this BackupSet is not in a ZIP file.
   */
  public File getUnzipDir() {
    return unzipDir;
  }

}
