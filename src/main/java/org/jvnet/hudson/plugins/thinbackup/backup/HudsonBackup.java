package org.jvnet.hudson.plugins.thinbackup.backup;

import hudson.PluginWrapper;
import hudson.model.Hudson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

public class HudsonBackup {

  private static final String BACKUP_PREFIX = "backup";
  private static final String CHANGELOG_XML_NAME = "changelog.xml";
  private static final String BUILD_XML_NAME = "build.xml";
  private static final String NEXT_BUILD_NUMBER_FILENAME = "nextBuildNumber";
  private static final String CONFIG_XML = "config.xml";
  private static final String JOBS_DIR = "jobs";

  private final File hudsonDirectory;
  private final File backupDirectory;

  public HudsonBackup(final String backupRootPath) {
    hudsonDirectory = Hudson.getInstance().getRootDir();

    final Date date = new Date();
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
    final String backupPath = String.format("%s/%s_%s", backupRootPath, BACKUP_PREFIX, format.format(date));
    backupDirectory = new File(backupPath);
  }

  public boolean run() throws IOException {
    if (!hudsonDirectory.exists() || !hudsonDirectory.isDirectory()) {
      throw new FileNotFoundException("No Hudson directory found, thus cannot trigger backup.");
    }
    if (!backupDirectory.exists() || !backupDirectory.isDirectory()) {
      final boolean res = backupDirectory.mkdirs();
      if (!res) {
        throw new IOException("Could not create target directory.");
      }
    }

    backupGlobalXmls();
    backupJobs();
    storePluginList();

    return true;
  }

  private void backupGlobalXmls() throws IOException {
    IOFileFilter suffixFileFilter = FileFilterUtils.suffixFileFilter(".xml");
    FileFilterUtils.andFileFilter(FileFileFilter.FILE, suffixFileFilter);
    FileUtils.copyDirectory(hudsonDirectory, backupDirectory, suffixFileFilter);
  }

  private void backupJobs() throws IOException {
    final String jobsPath = String.format("%s/%s", hudsonDirectory.getAbsolutePath(), JOBS_DIR);
    final File jobsDirectory = new File(jobsPath);

    final String jobsBackupPath = String.format("%s/%s", backupDirectory.getAbsolutePath(), JOBS_DIR);
    final File jobsBackupDirectory = new File(jobsBackupPath);
    jobsBackupDirectory.mkdirs();

    IOFileFilter filter = FileFilterUtils.nameFileFilter(CONFIG_XML);
    filter = FileFilterUtils.orFileFilter(filter, FileFilterUtils.nameFileFilter(NEXT_BUILD_NUMBER_FILENAME));
    filter = FileFilterUtils.orFileFilter(filter, FileFilterUtils.nameFileFilter(BUILD_XML_NAME));
    filter = FileFilterUtils.orFileFilter(filter, FileFilterUtils.nameFileFilter(CHANGELOG_XML_NAME));
    filter = FileFilterUtils.andFileFilter(filter, FileFileFilter.FILE);
    filter = FileFilterUtils.orFileFilter(filter, DirectoryFileFilter.DIRECTORY);

    IOFileFilter notWorkspaceDirFilter = FileFilterUtils.nameFileFilter("workspace");
    notWorkspaceDirFilter = FileFilterUtils.notFileFilter(FileFilterUtils.andFileFilter(notWorkspaceDirFilter,
        DirectoryFileFilter.DIRECTORY));

    IOFileFilter notArchiveDirFilter = FileFilterUtils.nameFileFilter("archive");
    notWorkspaceDirFilter = FileFilterUtils.notFileFilter(FileFilterUtils.andFileFilter(notWorkspaceDirFilter,
        DirectoryFileFilter.DIRECTORY));

    filter = FileFilterUtils.andFileFilter(filter, notWorkspaceDirFilter);
    filter = FileFilterUtils.andFileFilter(filter, notArchiveDirFilter);
    FileUtils.copyDirectory(jobsDirectory, jobsBackupDirectory, filter);
  }

  private void storePluginList() throws IOException {
    final List<PluginWrapper> installedPlugins = Hudson.getInstance().getPluginManager().getPlugins();
    final File pluginVersionList = new File(backupDirectory, "installedPlugins.txt");
    pluginVersionList.createNewFile();
    final Writer w = new FileWriter(pluginVersionList);
    w.write(String.format("Hudson [%s]\n", Hudson.getVersion()));
    for (final PluginWrapper plugin : installedPlugins) {
      final String entry = String.format("Name: %s Version: %s\n", plugin.getShortName(), plugin.getVersion());
      w.write(entry);
    }
    w.close();
  }
}