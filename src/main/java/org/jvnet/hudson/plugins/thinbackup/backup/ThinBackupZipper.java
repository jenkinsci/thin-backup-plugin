package org.jvnet.hudson.plugins.thinbackup.backup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class ThinBackupZipper {
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

  /**
   * Moves all backup sets other than the one containing currentBackup to ZIP files located in backupRoot.
   * 
   * @param backupRoot
   * @param currentBackup
   */
  public void moveOldBackupsToZipFile(final File backupRoot, final File currentBackup) {
    LOGGER.fine("Moving old backups to zip files...");

    final List<BackupSet> validBackupSets = Utils.getValidBackupSets(backupRoot);

    for (final BackupSet backupSet : validBackupSets) {
      if (!backupSet.containsDirectory(currentBackup)) {
        final boolean success = zipBackupSet(backupRoot, backupSet);
        if (success) {
          LOGGER.fine(String.format("Successfully zipped backup set %s.", backupSet));
          try {
            backupSet.delete();
            LOGGER.fine(String.format("Deleted backup set %s.", backupSet));
          } catch (final IOException ioe) {
            LOGGER.log(Level.WARNING, String.format("Could not delete backup set %s.", backupSet));
          }
        }
      }
    }

    LOGGER.fine("DONE moving old backups to zip files.");
  }

  private boolean zipBackupSet(final File backupRoot, final BackupSet backupSet) {
    boolean success = true;

    try {
      final String zipFileName = "BACKUPSET_" + getFullBackupDate(backupSet) + "_" + getLastDiffBackupDate(backupSet)
          + ".zip";
      final File zipFile = new File(backupRoot, zipFileName);
      final DirectoriesZipper zipper = new DirectoriesZipper(zipFile);

      zipper.addToZip(backupSet.getFullBackup());
      for (final File diffBackup : backupSet.getDiffBackups()) {
        zipper.addToZip(diffBackup);
      }

      zipper.close();
    } catch (final IOException ioe) {
      LOGGER.log(Level.WARNING, "Could not add backup set to ZIP file.", ioe);
      success = false;
    }

    return success;
  }

  private String getFullBackupDate(final BackupSet backupSet) {
    final String name = backupSet.getFullBackup().getName();
    return getDateOnly(name);
  }

  private String getLastDiffBackupDate(final BackupSet backupSet) {
    final List<File> diffBackups = backupSet.getDiffBackups();
    if (diffBackups.size() == 0) {
      return "";
    }

    final List<String> diffBackupNames = new ArrayList<String>(diffBackups.size());
    for (final File diffBackup : diffBackups) {
      diffBackupNames.add(diffBackup.getName());
    }
    Collections.sort(diffBackupNames);

    final String name = diffBackupNames.get(0);
    return getDateOnly(name);
  }

  private String getDateOnly(final String name) {
    return (name.length() >= 16) ? name.substring(name.length() - 16) : "";
  }

}
