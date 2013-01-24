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

import hudson.PluginManager;
import hudson.model.Hudson;
import hudson.model.UpdateCenter;
import hudson.model.UpdateCenter.UpdateCenterJob;
import hudson.model.UpdateSite;
import hudson.model.UpdateSite.Plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.backup.BackupSet;
import org.jvnet.hudson.plugins.thinbackup.backup.HudsonBackup;
import org.jvnet.hudson.plugins.thinbackup.backup.PluginList;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class HudsonRestore {
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

  private final String backupPath;
  private final File hudsonHome;
  private final Date restoreFromDate;
  private final boolean restoreNextBuildNumber;
  private final boolean restorePlugins;
  private final Map<String, List<Plugin>> availablePluginLocations;

  public HudsonRestore(final File hudsonConfigurationPath, final String backupPath, final Date restoreFromDate,
      final boolean restoreNextBuildNumber, final boolean restorePlugins) {
    this.hudsonHome = hudsonConfigurationPath;
    this.backupPath = backupPath;
    this.restoreFromDate = restoreFromDate;
    this.restoreNextBuildNumber = restoreNextBuildNumber;
    this.restorePlugins = restorePlugins;
    //this.restorePlugins = false;

    this.availablePluginLocations = new HashMap<String, List<Plugin>>();
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
    final IOFileFilter nextBuildNumberFileFilter = FileFilterUtils.nameFileFilter("nextBuildNumber");
    IOFileFilter restoreNextBuildNumberFilter;

    if (restoreNextBuildNumber) {
      restoreNextBuildNumberFilter = FileFilterUtils.trueFileFilter();

      @SuppressWarnings("unchecked")
      final Collection<File> restore = FileUtils.listFiles(toRestore, nextBuildNumberFileFilter,
          TrueFileFilter.INSTANCE);
      final Map<String, Integer> nextBuildNumbers = new HashMap<String, Integer>();
      for (final File file : restore) {
        BufferedReader reader = null;
        try {
          reader = new BufferedReader(new FileReader(file));
          nextBuildNumbers.put(file.getParentFile().getName(), Integer.parseInt(reader.readLine()));
        } finally {
          if (reader != null) {
            reader.close();
          }
        }
      }

      @SuppressWarnings("unchecked")
      final Collection<File> current = FileUtils.listFiles(hudsonHome, nextBuildNumberFileFilter,
          TrueFileFilter.INSTANCE);
      for (final File file : current) {
        BufferedReader reader = null;
        try {
          reader = new BufferedReader(new FileReader(file));
          final int currentBuildNumber = Integer.parseInt(reader.readLine());
          final Integer toRestoreNextBuildNumber = nextBuildNumbers.get(file.getParentFile().getName());
          if (currentBuildNumber < toRestoreNextBuildNumber) {
            restoreNextBuildNumber(file, toRestoreNextBuildNumber);
          }
        } finally {
          if (reader != null) {
            reader.close();
          }
        }
      }
    } else {
      restoreNextBuildNumberFilter = FileFilterUtils.notFileFilter(nextBuildNumberFileFilter);
    }

    FileUtils.copyDirectory(toRestore, this.hudsonHome, restoreNextBuildNumberFilter, true);

    if (restorePlugins)
      restorePlugins(toRestore);
  }

  private void restorePlugins(File toRestore) throws IOException {
    File[] list = toRestore.listFiles((FilenameFilter) FileFilterUtils.nameFileFilter("installedPlugins.xml"));

    if (list.length != 1) {
      LOGGER
          .severe("Cannot restore plugins because no or mulitble files with the name 'installedPlugins.xml' are in the backup.");
      return;
    }

    File backupedPlugins = list[0];

    PluginList pluginList = new PluginList(backupedPlugins);
    pluginList.load();
    Map<String, String> toRestorePlugins = pluginList.getPlugins();
    List<Future<UpdateCenterJob>> pluginRestoreJobs = new ArrayList<Future<UpdateCenterJob>>(toRestorePlugins.size());
    PluginManager pluginManager = Hudson.getInstance().getPluginManager();
    for (Entry<String, String> entry : toRestorePlugins.entrySet()) {
      if (pluginManager.getPlugin(entry.getKey()) == null) { // if any version of this plugin is installed do nothing
        Future<UpdateCenterJob> monitor = installPlugin(entry.getKey(), entry.getValue());
        if (monitor != null)
          pluginRestoreJobs.add(monitor);
      } else
        LOGGER.info("Plugin '" + entry.getKey() + "' already installed. Please check manually.");
    }

    boolean finished = false;
    while (!finished) {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        // nothing to do
      }
      for (Future<UpdateCenterJob> future : pluginRestoreJobs) {
        finished = future.isDone();
        if (!finished)
          break;
      }
    }
  }

  private void restoreNextBuildNumber(File file, Integer toRestoreNextBuildNumber) throws IOException {
    file.delete();
    file.createNewFile();
    Writer writer = null;
    try {
      writer = new FileWriter(file);
      writer.write(toRestoreNextBuildNumber);
    } finally {
      if (writer != null) {
        writer.close();
      }
    }
  }

  private Future<UpdateCenterJob> installPlugin(String pluginID, String version) {
    if (!version.contains("SNAPSHOT") && !"Hudson core".equals(pluginID) && !"Jenkins core".equals(pluginID)) {
      UpdateCenter updateCenter = Hudson.getInstance().getUpdateCenter();

      for (UpdateSite site : updateCenter.getSites()) {
        List<Plugin> availablePlugins;
        if (this.availablePluginLocations.containsKey(site.getId()))
          availablePlugins = this.availablePluginLocations.get(site.getId());
        else {
          availablePlugins = site.getAvailables();
          this.availablePluginLocations.put(site.getId(), availablePlugins);
        }

        for (Plugin plugin : availablePlugins) {
          if (plugin.name.equals(pluginID)) {
            LOGGER.info("Restore plugin '" + pluginID + "'.");
            if (!plugin.version.equals(version)) {
              Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
              PluginRestoreUpdateCenter pruc = new PluginRestoreUpdateCenter();
              
              return pruc._addJob(pruc.new PluginRestoreJob(site, Hudson.getAuthentication(), plugin, version));
            } else
              return plugin.deploy();
          }
        }
      }
    }
    LOGGER
        .warning("Cannot find plugin '" + pluginID + "' with the version '" + version + "'. Please install manually!");
    return null;
  }
}
