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

import hudson.PluginWrapper;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.RunList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class HudsonBackup {

  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

  public static final String BUILDS_DIR_NAME = "builds";
  public static final String CONFIGURATIONS_DIR_NAME = "configurations";
  public static final String JOBS_DIR_NAME = "jobs";
  public static final String USERS_DIR_NAME = "users";
  public static final String ARCHIVE_DIR_NAME = "archive";
  public static final String USERSCONTENTS_DIR_NAME = "userContent";
  public static final String NEXT_BUILD_NUMBER_FILE_NAME = "nextBuildNumber";
  public static final String CONFIG_XML = "config.xml";
  public static final String XML_FILE_EXTENSION = ".xml";
  public static final String ZIP_FILE_EXTENSION = ".zip";
  public static final String INSTALLED_PLUGINS_XML = "installedPlugins" + XML_FILE_EXTENSION;
  public static final String CHANGELOG_HISTORY_PLUGIN_DIR_NAME = "changelog-history";

  private final ThinBackupPluginImpl plugin;
  private final File hudsonHome;
  private final File backupRoot;
  private final File backupDirectory;
  private final BackupType backupType;
  private final Date latestFullBackupDate;
  private Pattern excludedFilesRegexPattern = null;
  private ItemGroup<TopLevelItem> hudson;

  public HudsonBackup(final ThinBackupPluginImpl plugin, final BackupType backupType) {
    this(plugin, backupType, new Date(), Hudson.getInstance());
  }

  /**
   * package visible constructor for unit testing purposes only.
   */
  protected HudsonBackup(final ThinBackupPluginImpl plugin, final BackupType backupType, final Date date, ItemGroup<TopLevelItem> hudson) {
    this.hudson = hudson;
    this.plugin = plugin;
    this.hudsonHome = plugin.getHudsonHome();

    final String excludedFilesRegex = plugin.getExcludedFilesRegex();
    if ((excludedFilesRegex != null) && !excludedFilesRegex.isEmpty()) {
      try {
        excludedFilesRegexPattern = Pattern.compile(excludedFilesRegex);
      } catch (final PatternSyntaxException pse) {
        LOGGER.log(Level.SEVERE, String.format("Regex pattern '%s' for excluding files is invalid, and will be disregarded.", excludedFilesRegex), pse);
        excludedFilesRegexPattern = null;
      }
    }

    this.backupRoot = new File(plugin.getExpandedBackupPath());
    if (!backupRoot.exists()) {
      backupRoot.mkdirs();
    }

    latestFullBackupDate = getLatestFullBackupDate();
    // if no full backup has been done yet, do a FULL backup
    if (latestFullBackupDate == null) {
      LOGGER.info("No previous full backup found, thus creating one.");
      this.backupType = BackupType.FULL;
    } else {
      this.backupType = backupType;
    }

    backupDirectory = Utils.getFormattedDirectory(backupRoot, this.backupType, date);
  }

  public void backup() throws IOException {
    if (backupType == BackupType.NONE) {
      final String msg = "Backup type must be FULL or DIFF. Backup cannot be performed.";
      LOGGER.severe(msg);
      throw new IllegalStateException(msg);
    }

    LOGGER.fine(MessageFormat.format("Performing {0} backup.", backupType));

    if (!hudsonHome.exists() || !hudsonHome.isDirectory()) {
      final String msg = "No Hudson directory found. Backup cannot be performed.";
      LOGGER.severe(msg);
      throw new FileNotFoundException(msg);
    }
    if (!backupDirectory.exists() || !backupDirectory.isDirectory()) {
      final boolean res = backupDirectory.mkdirs();
      if (!res) {
        final String msg = "Could not create backup directory. Backup cannot be performed.";
        LOGGER.severe(msg);
        throw new IOException(msg);
      }
    }

    backupGlobalXmls();
    backupJobs();
    backupRootFolder(USERS_DIR_NAME);
    storePluginListIfChanged();

    if (plugin.isBackupUserContents())
      backupRootFolder(USERSCONTENTS_DIR_NAME);

    new DirectoryCleaner().removeEmptyDirectories(backupDirectory);

    if (backupType == BackupType.FULL) {
      cleanupDiffs();
      moveOldBackupsToZipFile(backupDirectory);
      removeSuperfluousBackupSets();
    }
  }

  private void backupGlobalXmls() throws IOException {
    LOGGER.fine("Backing up global configuration files...");

    IOFileFilter suffixFileFilter = FileFilterUtils.suffixFileFilter(XML_FILE_EXTENSION);
    suffixFileFilter = FileFilterUtils.andFileFilter(FileFileFilter.FILE, suffixFileFilter);
    suffixFileFilter = FileFilterUtils.andFileFilter(suffixFileFilter, getFileAgeDiffFilter());
    suffixFileFilter = FileFilterUtils.andFileFilter(suffixFileFilter, getExcludedFilesFilter());
    FileUtils.copyDirectory(hudsonHome, backupDirectory, suffixFileFilter);

    LOGGER.fine("DONE backing up global configuration files.");
  }

  private void backupJobs() throws IOException {
    LOGGER.fine("Backing up job specific configuration files...");
    final File jobsDirectory = new File(hudsonHome.getAbsolutePath(), JOBS_DIR_NAME);
    final File jobsBackupDirectory = new File(backupDirectory.getAbsolutePath(), JOBS_DIR_NAME);

    backupJobsDirectory(jobsDirectory, jobsBackupDirectory);
    LOGGER.fine("DONE backing up job specific configuration files.");
  }

  private void backupJobsDirectory(final File jobsDirectory, final File jobsBackupDirectory) throws IOException {
    Collection<String> jobNames = Arrays.asList(jobsDirectory.list());
    LOGGER.info(String.format("Found %d jobs to back up.", jobNames.size()));
    LOGGER.fine(String.format("\t%s", jobNames));    

    for (final String jobName : jobNames) {
      final File jobDirectory = new File(jobsDirectory, jobName);
      if (jobDirectory.exists() && jobDirectory.isDirectory()) { // sub jobs e.g. maven modules need not be copied
        if (jobDirectory.canRead()) {
          File childJobsFolder = new File(jobDirectory, HudsonBackup.JOBS_DIR_NAME);
          if (childJobsFolder.exists()) { // found CloudBeesFolder
            File folderBackupDirectory = new File(jobsBackupDirectory, jobName);
            File folderJobsBackupDirectory = new File(folderBackupDirectory, JOBS_DIR_NAME);
            folderJobsBackupDirectory.mkdirs();
            FileUtils.copyFile(new File(jobDirectory, CONFIG_XML), new File(folderBackupDirectory, CONFIG_XML));
            backupJobsDirectory(childJobsFolder, folderJobsBackupDirectory);
          } else
            backupJob(jobDirectory, jobsBackupDirectory, jobName);
        } else {
          final String msg = String.format("Read access denied on directory '%s', cannot back up the job '%s'.", jobDirectory.getAbsolutePath(), jobName);
          LOGGER.severe(msg);
        }
      }
    }
  }

  private void backupJob(final File jobDirectory, final File jobsBackupDirectory, final String jobName) throws IOException {
    final File jobBackupDirectory = new File(jobsBackupDirectory, jobName);
    backupJobConfigFor(jobDirectory, jobBackupDirectory);
    backupBuildsFor(jobDirectory, jobBackupDirectory);
    if (isMatrixJob(jobDirectory)) {
      List<File> configurations = findAllConfigurations(new File(jobDirectory, HudsonBackup.CONFIGURATIONS_DIR_NAME));
      for (File configurationDirectory : configurations) {
        File configurationBackupDirectory = createConfigurationBackupDirectory(jobBackupDirectory, jobDirectory, configurationDirectory);
        backupJobConfigFor(configurationDirectory, configurationBackupDirectory);
        backupBuildsFor(configurationDirectory, configurationBackupDirectory);
      }
    }
  }

  private File createConfigurationBackupDirectory(File jobBackupdirectory, File jobDirectory, File configurationDirectory) {
    String pathToConfiguration = configurationDirectory.getAbsolutePath();
    String pathToJob = jobDirectory.getAbsolutePath();

    return new File(jobBackupdirectory, pathToConfiguration.substring(pathToJob.length()));
  }

  private List<File> findAllConfigurations(File dir) {
    @SuppressWarnings("unchecked")
    Collection<File> listFiles = FileUtils.listFiles(dir, FileFilterUtils.nameFileFilter(CONFIG_XML), TrueFileFilter.INSTANCE);

    List<File> confs = new ArrayList<File>();
    for (File file : listFiles) {
      confs.add(file.getParentFile());
    }

    return confs;
  }

  private boolean isMatrixJob(File jobDirectory) {
    return new File(jobDirectory, CONFIGURATIONS_DIR_NAME).isDirectory();
  }

  private void backupJobConfigFor(final File jobDirectory, final File jobBackupDirectory) throws IOException {
    IOFileFilter filter = FileFilterUtils.suffixFileFilter(XML_FILE_EXTENSION);
    filter = FileFilterUtils.andFileFilter(filter, getFileAgeDiffFilter());
    filter = FileFilterUtils.andFileFilter(filter, getExcludedFilesFilter());
    FileUtils.copyDirectory(jobDirectory, jobBackupDirectory, filter);
    backupNextBuildNumberFile(jobDirectory, jobBackupDirectory);
  }

  private void backupNextBuildNumberFile(final File jobDirectory, final File jobBackupDirectory) throws IOException {
    if (plugin.isBackupNextBuildNumber()) {
      final File nextBuildNumberFile = new File(jobDirectory, NEXT_BUILD_NUMBER_FILE_NAME);
      if (nextBuildNumberFile.exists()) {
        FileUtils.copyFileToDirectory(nextBuildNumberFile, jobBackupDirectory, true);
      }
    }
  }

  private void backupBuildsFor(final File jobDirectory, final File jobBackupDirectory) throws IOException {
    if (plugin.isBackupBuildResults()) {
      final File buildsDir = new File(jobDirectory, BUILDS_DIR_NAME);
      if (buildsDir.exists() && buildsDir.isDirectory()) {
        final Collection<String> builds = Arrays.asList(buildsDir.list());
        if (builds != null) {
          TopLevelItem job = hudson.getItem(jobDirectory.getName());
          for (final String build : builds) {
            final File srcDir = new File(buildsDir, build);
            if (!isSymLinkFile(srcDir) && (!plugin.isBackupBuildsToKeepOnly() || isBuildToKeep(job, srcDir))) {
              final File destDir = new File(new File(jobBackupDirectory, BUILDS_DIR_NAME), build);
              backupBuildFiles(srcDir, destDir);
              backupBuildArchive(srcDir, destDir);
            }
          }
        }
      }
    }
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  private boolean isBuildToKeep(TopLevelItem item, File buildDir) {
    if (item instanceof Job) {
      Job job = (Job) item;
      RunList<Run> builds = job.getBuilds();
      for (Run run : builds) {
        if (run.getRootDir().equals(buildDir)) {
          return run.isKeepLog();
        }
      }
    }
    // default to true, in the case we can't resolve this folder in the Hudson instance
    return true;
  }
  
  private void backupBuildFiles(final File srcDir, final File destDir) throws IOException {
    final IOFileFilter changelogFilter = FileFilterUtils.andFileFilter(DirectoryFileFilter.DIRECTORY,
        FileFilterUtils.nameFileFilter(CHANGELOG_HISTORY_PLUGIN_DIR_NAME));
    final IOFileFilter fileFilter = FileFilterUtils.andFileFilter(FileFileFilter.FILE, getFileAgeDiffFilter());

    IOFileFilter filter = FileFilterUtils.orFileFilter(changelogFilter, fileFilter);
    filter = FileFilterUtils.andFileFilter(filter, getExcludedFilesFilter());
    filter = FileFilterUtils.andFileFilter(filter, FileFilterUtils.notFileFilter(FileFilterUtils.suffixFileFilter(ZIP_FILE_EXTENSION)));
    FileUtils.copyDirectory(srcDir, destDir, filter);
  }

  private void backupBuildArchive(final File buildSrcDir, final File buildDestDir) throws IOException {
    if (plugin.isBackupBuildArchive()) {
      final File archiveSrcDir = new File(buildSrcDir, ARCHIVE_DIR_NAME);
      if (archiveSrcDir.isDirectory()) {
        final IOFileFilter filter = FileFilterUtils.orFileFilter(
                FileFilterUtils.directoryFileFilter(), 
                FileFilterUtils.andFileFilter(FileFileFilter.FILE, getFileAgeDiffFilter()));
        FileUtils.copyDirectory(archiveSrcDir, new File(buildDestDir, "archive"), filter);
      }
    }
  }

  private void backupRootFolder(String folderName) throws IOException {
    final File srcDirectory = new File(hudsonHome.getAbsolutePath(), folderName);
    if (srcDirectory.exists() && srcDirectory.isDirectory()) {
      LOGGER.fine(String.format("Backing up %s...", folderName));
      final File destDirectory = new File(backupDirectory.getAbsolutePath(), folderName);
      IOFileFilter filter = getFileAgeDiffFilter();
      filter = FileFilterUtils.andFileFilter(filter, getExcludedFilesFilter());
      filter = FileFilterUtils.orFileFilter(filter, DirectoryFileFilter.DIRECTORY);
      FileUtils.copyDirectory(srcDirectory, destDirectory, filter);
      LOGGER.fine(String.format("DONE backing up %s.", folderName));
    }
  }

  private boolean isSymLinkFile(final File file) throws IOException {
    final String canonicalPath = file.getCanonicalPath();
    final String absolutePath = file.getAbsolutePath();
    return !canonicalPath.substring(canonicalPath.lastIndexOf(File.separatorChar)).equals(absolutePath.substring(absolutePath.lastIndexOf(File.separatorChar)));
  }

  private void storePluginListIfChanged() throws IOException {
    final PluginList pluginList = getInstalledPlugins();
    PluginList latestFullPlugins = null;
    if (backupType == BackupType.DIFF) {
      latestFullPlugins = getPluginListFromLatestFull();
    }

    if (pluginList.compareTo(latestFullPlugins) != 0) {
      LOGGER.fine("Storing list of installed plugins...");
      pluginList.save();
    } else {
      LOGGER.fine("No changes in plugin list since last full backup.");
    }

    LOGGER.fine("DONE storing list of installed plugins.");
  }

  private PluginList getInstalledPlugins() {
    final File pluginVersionList = new File(backupDirectory, INSTALLED_PLUGINS_XML);
    final PluginList newPluginList = new PluginList(pluginVersionList);
    final Hudson hudson = Hudson.getInstance();
    if (hudson != null) {
      newPluginList.add("Hudson core", Hudson.getVersion().toString());
    }

    final List<PluginWrapper> installedPlugins;
    if (hudson != null) {
      installedPlugins = hudson.getPluginManager().getPlugins();
    } else {
      installedPlugins = Collections.emptyList();
    }
    for (final PluginWrapper pluginWrapper : installedPlugins) {
      newPluginList.add(pluginWrapper.getShortName(), pluginWrapper.getVersion());
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

  private void removeSuperfluousBackupSets() throws IOException {
    if (plugin.getNrMaxStoredFull() > 0) {
      LOGGER.fine("Removing superfluous backup sets...");
      final List<BackupSet> validBackupSets = Utils.getValidBackupSets(new File(plugin.getExpandedBackupPath()));
      int nrOfRemovedBackups = 0;
      while (validBackupSets.size() > plugin.getNrMaxStoredFull()) {
        final BackupSet set = validBackupSets.get(0);
        set.delete();
        validBackupSets.remove(set);
        ++nrOfRemovedBackups;
      }
      LOGGER.fine(String.format("DONE. Removed %d superfluous backup sets.", nrOfRemovedBackups));
    }
  }

  private void cleanupDiffs() throws IOException {
    if (plugin.isCleanupDiff()) {
      LOGGER.fine("Cleaning up diffs...");

      final Collection<File> diffDirs = Utils.getBackupTypeDirectories(backupDirectory.getParentFile(), BackupType.DIFF);

      for (final File diffDirToDelete : diffDirs) {
        FileUtils.deleteDirectory(diffDirToDelete);
      }
      LOGGER.fine(String.format("DONE. Removed %s unnecessary diff directories.", diffDirs.size()));
    }
  }

  private void moveOldBackupsToZipFile(final File currentBackup) {
    if (plugin.isMoveOldBackupsToZipFile()) {
      final ZipperThread zipperThread = new ZipperThread(backupRoot, currentBackup);
      zipperThread.start();
    }
  }

  private IOFileFilter getFileAgeDiffFilter() {
    IOFileFilter result = FileFilterUtils.trueFileFilter();

    if (backupType == BackupType.DIFF) {
      result = FileFilterUtils.ageFileFilter(latestFullBackupDate, false);
    }

    return result;
  }

  private IOFileFilter getExcludedFilesFilter() {
    IOFileFilter result = FileFilterUtils.trueFileFilter();

    if (excludedFilesRegexPattern != null) {
      result = FileFilterUtils.notFileFilter(new RegexFileFilter(excludedFilesRegexPattern));
    }

    return result;
  }

  private Date getLatestFullBackupDate() {
    final List<File> fullBackups = Utils.getBackupTypeDirectories(backupRoot, BackupType.FULL);
    if ((fullBackups == null) || (fullBackups.size() == 0)) {
      return null;
    }

    Date result = new Date(0);
    for (final File fullBackup : fullBackups) {
      final Date tmp = Utils.getDateFromBackupDirectory(fullBackup);
      if (tmp != null) {
        if (tmp.after(result)) {
          result = tmp;
        }
      } else {
        LOGGER.info(String.format("Cannot parse directory name '%s', thus ignoring it when getting latest backup date.", fullBackup.getName()));
      }
    }

    return result;
  }

  /**
   * Zipping the old backups is done in a thread so the rest of Hudson/Jenkins is not blocked.
   */
  public static class ZipperThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

    private final File backupRoot;
    private final File currentBackup;

    public ZipperThread(final File backupRoot, final File currentBackup) {
      this.backupRoot = backupRoot;
      this.currentBackup = currentBackup;
    }

    @Override
    public void run() {
      LOGGER.fine("Starting zipper thread...");
      try {
        Utils.moveOldBackupsToZipFile(backupRoot, currentBackup);
      } catch (final IOException ioe) {
        LOGGER.log(Level.SEVERE, "Cannot zip old backups.", ioe);
      }
      LOGGER.fine("DONE zipping.");
    }
  }
}