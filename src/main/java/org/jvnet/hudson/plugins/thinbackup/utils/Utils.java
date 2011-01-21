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
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;
import org.jvnet.hudson.plugins.thinbackup.backup.BackupSet;

public class Utils {
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

  private static final int COMPUTER_TIMEOUT_WAIT = 500; // ms
  private static SimpleDateFormat DIRECTORY_NAME_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
  private static SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
  private static final String DIRECTORY_NAME_DATE_EXTRACTION_REGEX = String.format("(%s|%s)-", BackupType.FULL,
      BackupType.DIFF);

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
        Thread.sleep(COMPUTER_TIMEOUT_WAIT);
      } catch (final InterruptedException e) {
        LOGGER.log(Level.WARNING, e.getMessage(), e);
      }
    } while (running);
  }

  /**
   * @param diffBackup
   * @return the full backup referenced by the given diff backup, or null if none can be found.
   */
  public static File getReferencedFullBackup(final File diffBackup) {
    if (diffBackup.getName().startsWith(BackupType.FULL.toString())) {
      return diffBackup;
    }

    IOFileFilter prefixFilter = FileFilterUtils.prefixFileFilter(BackupType.FULL.toString());
    prefixFilter = FileFilterUtils.andFileFilter(prefixFilter, DirectoryFileFilter.DIRECTORY);
    final Collection<File> backups = Arrays.asList(new File(diffBackup.getParent())
        .listFiles((FilenameFilter) prefixFilter));

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

  public static File getFormattedDirectory(final File directory, final BackupType backupType, final Date date) {
    final File formattedDirectory = new File(directory, String.format("%s-%s", backupType,
        DIRECTORY_NAME_DATE_FORMAT.format(date)));
    return formattedDirectory;
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

    IOFileFilter prefixFilter = FileFilterUtils.prefixFileFilter(BackupType.DIFF.toString());
    prefixFilter = FileFilterUtils.andFileFilter(prefixFilter, DirectoryFileFilter.DIRECTORY);
    final Collection<File> allDiffBackups = Arrays.asList(new File(fullBackup.getParent())
        .listFiles((FilenameFilter) prefixFilter));

    for (final File diffBackup : allDiffBackups) {
      final File tmpFullBackup = getReferencedFullBackup(diffBackup);
      if ((tmpFullBackup != null) && (tmpFullBackup.getAbsolutePath().equals(fullBackup.getAbsolutePath()))) {
        diffBackups.add(diffBackup);
      }
    }

    return diffBackups;
  }

  /**
   * @return a list of available backup sets, ordered ascending by date of the BackupSets full backup.
   */
  public static List<BackupSet> getAvailableValidBackupSets() {
    final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.getInstance();
    IOFileFilter prefixFilter = FileFilterUtils.prefixFileFilter(BackupType.FULL.toString());
    prefixFilter = FileFilterUtils.andFileFilter(prefixFilter, DirectoryFileFilter.DIRECTORY);
    final Collection<File> backups = Arrays.asList(new File(plugin.getBackupPath())
        .listFiles((FilenameFilter) prefixFilter));

    final List<BackupSet> sets = new ArrayList<BackupSet>();
    for (final File backup : backups) {
      final BackupSet set = new BackupSet(backup);
      if (set.isValid()) {
        sets.add(set);
      }
    }
    Collections.sort(sets);
    return sets;
  }

  /**
   * @return a list of available backups, ordered ascending by date.
   */
  public static List<String> getAvailableBackups() {
    final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.getInstance();
    IOFileFilter filter = FileFilterUtils.prefixFileFilter(BackupType.FULL.toString());
    filter = FileFilterUtils.orFileFilter(filter, FileFilterUtils.prefixFileFilter(BackupType.DIFF.toString()));
    filter = FileFilterUtils.andFileFilter(filter, DirectoryFileFilter.DIRECTORY);
    final String[] backups = new File(plugin.getBackupPath()).list(filter);

    final List<String> list = new ArrayList<String>(backups.length);
    for (final String name : backups) {
      try {
        final String dateOnly = name.replaceFirst(DIRECTORY_NAME_DATE_EXTRACTION_REGEX, "");
        final Date tmp = DIRECTORY_NAME_DATE_FORMAT.parse(dateOnly);
        list.add(DISPLAY_DATE_FORMAT.format(tmp));
      } catch (final ParseException e) {
        LOGGER.warning("Cannot parse directory name '" + name
            + "', so it will not show up in the list of available backups.");
      }
    }
    Collections.sort(list);
    Collections.reverse(list);

    return list;
  }

}
