package org.jvnet.hudson.plugins.thinbackup.backup;

import hudson.PluginWrapper;
import hudson.model.Hudson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HudsonBackup {

  private static final String BACKUP_PREFIX = "backup_hudson";
  private static final String CHANGELOG_XML_NAME = "\\changelog.xml";
  private static final String BUILD_XML_NAME = "\\build.xml";
  private static final String BUILDS_DIR = "\\builds";
  private static final String NEXT_BUILD_NUMBER_FILENAME = "\\nextBuildNumber";
  private static final String CONFIG_XML = "\\config.xml";
  private static final String JOBS_DIR = "\\jobs";

  private final File hudsonDirectory;
  private final File backupDirectory;

  public HudsonBackup(final String backupRootPath) {
    hudsonDirectory = Hudson.getInstance().getRootDir();

    final Date date = new Date();
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
    final String backupPath = backupRootPath + "\\" + BACKUP_PREFIX + "_"
        + format.format(date);
    backupDirectory = new File(backupPath);
  }

  public boolean run() throws IOException {
    if (!hudsonDirectory.exists() || !hudsonDirectory.isDirectory()) {
      throw new FileNotFoundException(
          "No Hudson directory found, thus cannot trigger backup.");
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
    final File[] files = hudsonDirectory.listFiles();
    for (final File source : files) {
      if (source.getName().endsWith(".xml")) {
        final File target = new File(backupDirectory.getAbsolutePath() + "\\"
            + source.getName());
        copyFile(source, target);
      }
    }
  }

  private void copyFile(final File source, final File target)
      throws IOException {
    if (target.exists() && (source.lastModified() <= target.lastModified())) {
      return;
    }
    InputStream in = null;
    OutputStream out = null;
    try {
      try {
        in = new FileInputStream(source);
      } catch (final IOException ioe) {
        System.out.println("Required file " + source.getAbsolutePath()
            + " does not exist, backup for this file was cancelled.");
        return;
      }
      target.createNewFile();
      out = new FileOutputStream(target);

      final byte[] buf = new byte[1024];
      int len = 0;
      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
    } finally {
      if (in != null) {
        in.close();
      }
      if (out != null) {
        out.close();
      }
    }
  }

  private void backupJobs() throws IOException {
    final String jobsPath = hudsonDirectory.getAbsolutePath() + JOBS_DIR;
    final File jobsDirectory = new File(jobsPath);

    final String jobsBackupPath = backupDirectory.getAbsolutePath() + JOBS_DIR;
    final File jobsBackupDirectory = new File(jobsBackupPath);
    jobsBackupDirectory.mkdirs();

    final String[] jobs = jobsDirectory.list();
    for (final String job : jobs) {
      final String currentJobPath = jobsPath + "\\" + job;
      final String currentBackupPath = jobsBackupPath + "\\" + job;
      final File currentJobBackupDirectory = new File(currentBackupPath);
      currentJobBackupDirectory.mkdirs();

      final File configXmlSource = new File(currentJobPath + CONFIG_XML);
      final File configXmlTarget = new File(currentBackupPath + CONFIG_XML);
      copyFile(configXmlSource, configXmlTarget);

      final File nextBuildNumberSource = new File(currentJobPath
          + NEXT_BUILD_NUMBER_FILENAME);
      final File nextBuildNumberTarget = new File(currentBackupPath
          + NEXT_BUILD_NUMBER_FILENAME);
      if (configXmlSource.exists()) {
        copyFile(nextBuildNumberSource, nextBuildNumberTarget);
      }

      saveBuilds(currentJobPath, currentBackupPath);
    }
  }

  private void saveBuilds(final String hudson, final String backup)
      throws IOException {
    final File hudsonDir = new File(hudson + BUILDS_DIR);
    final File backupDir = new File(backup + BUILDS_DIR);

    if ((hudsonDir == null) || !hudsonDir.isDirectory()) {
      return;
    } else {
      backupDir.mkdirs();
    }

    final String[] builds = hudsonDir.list();
    for (final String build : builds) {
      final String curHudsonBuild = hudson + BUILDS_DIR + "\\" + build;
      final String curBackupBuild = backup + BUILDS_DIR + "\\" + build;

      final File curBackupBuildDir = new File(curBackupBuild);
      curBackupBuildDir.mkdirs();

      // build.xml
      final File buildfileSource = new File(curHudsonBuild + BUILD_XML_NAME);
      final File buildfileTarget = new File(curBackupBuild + BUILD_XML_NAME);

      copyFile(buildfileSource, buildfileTarget);

      // changelog.xml
      final File changelogSource = new File(curHudsonBuild + CHANGELOG_XML_NAME);
      final File changelogTarget = new File(curBackupBuild + CHANGELOG_XML_NAME);

      copyFile(changelogSource, changelogTarget);
    }
  }

  private void storePluginList() throws IOException {
    final List<PluginWrapper> installedPlugins = Hudson.getInstance()
        .getPluginManager().getPlugins();
    final File pluginVersionList = new File(backupDirectory
        + "/installedPlugins.xml");
    pluginVersionList.createNewFile();
    final Writer w = new FileWriter(pluginVersionList);
    w.write(String.format("Hudson [%s]\n", Hudson.getVersion()));
    for (final PluginWrapper plugin : installedPlugins) {
      final String entry = String.format("Name: %s Version: %s\n",
          plugin.getShortName(), plugin.getVersion());
      w.write(entry);
    }
    w.close();
  }
}