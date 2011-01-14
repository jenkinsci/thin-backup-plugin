package org.jvnet.hudson.plugins.thinbackup.backup;

import hudson.PluginWrapper;
import hudson.model.Hudson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class HudsonBackup {
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

  private static final String INSTALLED_PLUGINS_XML = "installedPlugins.xml";
  private static final String BUILDS_DIR_NAME = "Builds";
  private static final String JOBS_DIR_NAME = "jobs";

  private final Hudson hudson;
  private final File hudsonDirectory;
  private final File backupRoot;
  private final File backupDirectory;
  private final BackupType backupType;
  private final Date latestFullBackupDate;
  private final boolean cleanupDiff;
  private final int nrMaxStoredFull;

  public HudsonBackup(final File backupRoot, final File hudsonHome, final BackupType backupType,
      final int nrMaxStoredFull, final boolean cleanupDiff) {
    hudson = Hudson.getInstance();

    hudsonDirectory = hudsonHome;
    this.cleanupDiff = cleanupDiff;
    this.nrMaxStoredFull = nrMaxStoredFull;

    this.backupRoot = backupRoot;
    if (!backupRoot.exists()) {
      backupRoot.mkdir();
    }

    latestFullBackupDate = getLatestFullBackupDate();
    // if no full backup has been done yet, do a FULL backup
    if (latestFullBackupDate == null) {
      this.backupType = BackupType.FULL;
    } else {
      this.backupType = backupType;
    }

    final Date date = new Date();
    backupDirectory = Utils.getFormattedDirectory(backupRoot, backupType, date);
  }

  public void backup() throws IOException {
    if (backupType == BackupType.NONE) {
      throw new IllegalStateException("Backup type must be FULL or DIFF.");
    }

    LOGGER.info(MessageFormat.format("Performing {0} backup.", backupType));

    if (!hudsonDirectory.exists() || !hudsonDirectory.isDirectory()) {
      throw new FileNotFoundException("No Hudson directory found, thus cannot trigger backup.");
    }
    if (!backupDirectory.exists() || !backupDirectory.isDirectory()) {
      final boolean res = backupDirectory.mkdirs();
      if (!res) {
        throw new IOException("Could not create backup directory.");
      }
    }

    backupGlobalXmls();
    backupJobs();
    storePluginListIfChanged();

    new DirectoryCleaner().removeEmptyDirectories(backupDirectory);

    if (backupType == BackupType.FULL) {
      removeSuperfluousBackups();
      cleanupDiffs();
    }
  }

  private void backupGlobalXmls() throws IOException {
    IOFileFilter suffixFileFilter = FileFilterUtils.suffixFileFilter(".xml");
    suffixFileFilter = FileFilterUtils.andFileFilter(FileFileFilter.FILE, suffixFileFilter);
    suffixFileFilter = FileFilterUtils.andFileFilter(suffixFileFilter, getDiffFilter());
    FileUtils.copyDirectory(hudsonDirectory, backupDirectory, suffixFileFilter);
  }

  private void backupJobs() throws IOException {
    final String jobsPath = String.format("%s/%s", hudsonDirectory.getAbsolutePath(), JOBS_DIR_NAME);
    final File jobsDirectory = new File(jobsPath);

    final String jobsBackupPath = String.format("%s/%s", backupDirectory.getAbsolutePath(), JOBS_DIR_NAME);
    final File jobsBackupDirectory = new File(jobsBackupPath);

    Collection<String> jobNames;
    if (hudson != null) {
      jobNames = hudson.getJobNames();
    } else {
      jobNames = Arrays.asList(jobsDirectory.list());
    }

    IOFileFilter filter = FileFilterUtils.suffixFileFilter(".xml");
    IOFileFilter jobFilter = null;
    for (final String jobName : jobNames) {
      final IOFileFilter nameFileFilter = FileFilterUtils.nameFileFilter(jobName);
      if (jobFilter == null) {
        jobFilter = nameFileFilter;
      } else {
        jobFilter = FileFilterUtils.orFileFilter(filter, nameFileFilter);
      }

      final File buildsDir = new File(new File(jobsDirectory, jobName), BUILDS_DIR_NAME);
      if (buildsDir.exists() && buildsDir.isDirectory()) {
        final Collection<String> builds = Arrays.asList(buildsDir.list());
        if (builds != null) {
          final String buildsBackupPath = String.format("%s/%s/%s", jobsBackupPath, jobName, BUILDS_DIR_NAME);
          for (final String build : builds) {
            final File srcDir = new File(buildsDir, build);
            final File destDir = new File(buildsBackupPath, build);
            final IOFileFilter buildFilter = FileFilterUtils.andFileFilter(FileFileFilter.FILE, getDiffFilter());
            FileUtils.copyDirectory(srcDir, destDir, buildFilter);
          }
        }
      }
    }
    if (jobFilter != null) {
      jobFilter = FileFilterUtils.andFileFilter(jobFilter, DirectoryFileFilter.DIRECTORY);
      filter = FileFilterUtils.orFileFilter(filter, jobFilter);
    }

    filter = FileFilterUtils.andFileFilter(filter, getDiffFilter());
    FileUtils.copyDirectory(jobsDirectory, jobsBackupDirectory, filter);
  }

  private void storePluginListIfChanged() throws IOException {
    final PluginList pluginList = getInstalledPlugins();
    PluginList latestFullPlugins = null;
    if (backupType == BackupType.DIFF) {
      latestFullPlugins = getPluginListFromLatestFull();
    }

    if (pluginList.compareTo(latestFullPlugins) != 0) {
      pluginList.save();
    }
  }

  private PluginList getInstalledPlugins() {
    final File pluginVersionList = new File(backupDirectory, INSTALLED_PLUGINS_XML);
    final PluginList newPluginList = new PluginList(pluginVersionList);
    if (hudson != null) {
      newPluginList.add("Hudson core", Hudson.getVersion().toString());
    }

    final List<PluginWrapper> installedPlugins;
    if (hudson != null) {
      installedPlugins = hudson.getPluginManager().getPlugins();
    } else {
      installedPlugins = Collections.emptyList();
    }
    for (final PluginWrapper plugin : installedPlugins) {
      newPluginList.add(plugin.getShortName(), plugin.getVersion());
    }

    return newPluginList;
  }

  private PluginList getPluginListFromLatestFull() throws IOException {
    final File latestFullBackupDir = Utils.getFormattedDirectory(backupRoot, BackupType.FULL, latestFullBackupDate);
    final File pluginsOfLatestFull = new File(latestFullBackupDir, INSTALLED_PLUGINS_XML);
    final PluginList latestFullPlugins = new PluginList(pluginsOfLatestFull);
    latestFullPlugins.load();
    return latestFullPlugins;
  }

  private void removeSuperfluousBackups() throws IOException {
    if (nrMaxStoredFull > 0) {
      final List<BackupSet> availableBackupSets = Utils.getAvailableValidBackupSets();
      while (availableBackupSets.size() > nrMaxStoredFull) {
        final BackupSet set = availableBackupSets.get(0);
        set.delete();
        availableBackupSets.remove(set);
      }
    }
  }

  private void cleanupDiffs() throws IOException {
    if (cleanupDiff) {
      IOFileFilter filter = FileFilterUtils.prefixFileFilter(BackupType.DIFF.toString());
      filter = FileFilterUtils.andFileFilter(filter, DirectoryFileFilter.DIRECTORY);
      final File[] diffDirs = backupDirectory.getParentFile().listFiles((FilenameFilter) filter);
      for (final File diffDirToDelete : diffDirs) {
        FileUtils.deleteDirectory(diffDirToDelete);
      }
    }
  }

  private IOFileFilter getDiffFilter() {
    IOFileFilter result = FileFilterUtils.trueFileFilter();

    if (backupType == BackupType.DIFF) {
      result = FileFilterUtils.ageFileFilter(latestFullBackupDate, false);
    }

    return result;
  }

  private Date getLatestFullBackupDate() {
    final IOFileFilter prefixFilter = FileFilterUtils.prefixFileFilter(BackupType.FULL.toString());
    final Collection<File> backups = Arrays.asList(backupRoot.listFiles((FilenameFilter) prefixFilter));

    if (backups.isEmpty()) {
      return null;
    }

    Date latestBackupDate = null;
    for (final File fullBackupDir : backups) {
      final Date curModifiedDate = new Date(fullBackupDir.lastModified());
      if ((latestBackupDate == null) || curModifiedDate.after(latestBackupDate)) {
        latestBackupDate = curModifiedDate;
      }
    }

    return latestBackupDate;
  }

}