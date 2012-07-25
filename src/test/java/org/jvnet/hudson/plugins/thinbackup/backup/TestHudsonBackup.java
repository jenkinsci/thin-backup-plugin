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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.plugins.thinbackup.TestHelper;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;

public class TestHudsonBackup {

  private File backupDir;
  private File jenkinsHome;

  @Before
  public void setup() throws IOException {
    File base = new File(System.getProperty("java.io.tmpdir"));
    backupDir = TestHelper.createBackupFolder(base);

    jenkinsHome = TestHelper.createBasicFolderStructure(base);
    File jobDir = TestHelper.createJob(jenkinsHome, TestHelper.TEST_JOB_NAME);
    TestHelper.addNewBuildToJob(jobDir);
    
  }
  
  @After
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(jenkinsHome);
    FileUtils.deleteDirectory(backupDir);
    FileUtils.deleteDirectory(new File(Utils.THINBACKUP_TMP_DIR));
  }
  
  private ThinBackupPluginImpl createMockPlugin() {
    final ThinBackupPluginImpl mockPlugin = mock(ThinBackupPluginImpl.class);

    when(mockPlugin.getHudsonHome()).thenReturn(jenkinsHome);
    when(mockPlugin.getFullBackupSchedule()).thenReturn("");
    when(mockPlugin.getDiffBackupSchedule()).thenReturn("");
    when(mockPlugin.getExpandedBackupPath()).thenReturn(backupDir.getAbsolutePath());
    when(mockPlugin.getNrMaxStoredFull()).thenReturn(-1);
    when(mockPlugin.isCleanupDiff()).thenReturn(false);
    when(mockPlugin.isMoveOldBackupsToZipFile()).thenReturn(false);
    when(mockPlugin.isBackupBuildResults()).thenReturn(true);
    when(mockPlugin.isBackupBuildArchive()).thenReturn(false);
    when(mockPlugin.isBackupNextBuildNumber()).thenReturn(false);
    when(mockPlugin.getExcludedFilesRegex()).thenReturn("");

    return mockPlugin;
  }

  @Test
  public void testBackup() throws Exception {
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE - 10));

    final ThinBackupPluginImpl mockPlugin = createMockPlugin();

    new HudsonBackup(mockPlugin, BackupType.FULL, cal.getTime()).backup();

    String[] list = backupDir.list();
    Assert.assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    Assert.assertEquals(6, list.length);

    final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
    final List<String> arrayList = Arrays.asList(job.list());
    Assert.assertEquals(2, arrayList.size());
    Assert.assertFalse(arrayList.contains(HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME));

    final File build = new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRET_BUILD_DIRECTORY_NAME);
    list = build.list();
    Assert.assertEquals(7, list.length);

    final File changelogHistory = new File(
        new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRET_BUILD_DIRECTORY_NAME),
        HudsonBackup.CHANGELOG_HISTORY_PLUGIN_DIR_NAME);
    list = changelogHistory.list();
    Assert.assertEquals(2, list.length);
  }

  @Test
  public void testBackupWithExludes() throws Exception {
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE - 10));

    final ThinBackupPluginImpl mockPlugin = createMockPlugin();
    when(mockPlugin.getExcludedFilesRegex()).thenReturn("^.*\\.(log)$");

    new HudsonBackup(mockPlugin, BackupType.FULL, cal.getTime()).backup();

    String[] list = backupDir.list();
    Assert.assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    Assert.assertEquals(6, list.length);

    final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
    list = job.list();
    Assert.assertEquals(2, list.length);

    final File build = new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRET_BUILD_DIRECTORY_NAME);
    list = build.list();
    Assert.assertEquals(6, list.length);

    final File changelogHistory = new File(
        new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRET_BUILD_DIRECTORY_NAME),
        HudsonBackup.CHANGELOG_HISTORY_PLUGIN_DIR_NAME);
    list = changelogHistory.list();
    Assert.assertEquals(2, list.length);

    boolean containsLogfile = false;
    for (final String string : list) {
      if (string.equals("logfile.log")) {
        containsLogfile = true;
        break;
      }
    }
    Assert.assertFalse(containsLogfile);
  }

  @Test
  public void testBackupWithoutBuildResults() throws Exception {
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE - 10));

    final ThinBackupPluginImpl mockPlugin = createMockPlugin();
    when(mockPlugin.isBackupBuildResults()).thenReturn(false);

    new HudsonBackup(mockPlugin, BackupType.FULL, cal.getTime()).backup();

    String[] list = backupDir.list();
    Assert.assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    Assert.assertEquals(6, list.length);

    final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
    list = job.list();
    Assert.assertEquals(1, list.length);
    Assert.assertEquals("config.xml", list[0]);
  }

  public void performHudsonDiffBackup(final ThinBackupPluginImpl mockPlugin) throws Exception {
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE - 10));

    new HudsonBackup(mockPlugin, BackupType.FULL, cal.getTime()).backup();

    // fake modification
    backupDir.listFiles((FileFilter) FileFilterUtils.prefixFileFilter(BackupType.FULL.toString()))[0]
        .setLastModified(System.currentTimeMillis() - 60000 * 60);

    for (final File globalConfigFile : jenkinsHome.listFiles()) {
      globalConfigFile.setLastModified(System.currentTimeMillis() - 60000 * 120);
    }

    new HudsonBackup(mockPlugin, BackupType.DIFF, new Date()).backup();
  }

  @Test
  public void testHudsonDiffBackup() throws Exception {
    final ThinBackupPluginImpl mockPlugin = createMockPlugin();

    performHudsonDiffBackup(mockPlugin);

    final File lastDiffBackup = backupDir.listFiles((FileFilter) FileFilterUtils.prefixFileFilter(BackupType.DIFF
        .toString()))[0];
    Assert.assertEquals(1, lastDiffBackup.list().length);
  }

  @Test
  public void testBackupNextBuildNumber() throws Exception {
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE - 10));

    final ThinBackupPluginImpl mockPlugin = createMockPlugin();
    when(mockPlugin.isBackupNextBuildNumber()).thenReturn(true);

    new HudsonBackup(mockPlugin, BackupType.FULL, cal.getTime()).backup();

    String[] list = backupDir.list();
    Assert.assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    Assert.assertEquals(6, list.length);

    final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
    final List<String> arrayList = Arrays.asList(job.list());
    Assert.assertEquals(3, arrayList.size());
    Assert.assertTrue(TestHelper.containsStringEndingWith(arrayList, HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME));

    final File build = new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRET_BUILD_DIRECTORY_NAME);
    list = build.list();
    Assert.assertEquals(7, list.length);

    final File changelogHistory = new File(
        new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRET_BUILD_DIRECTORY_NAME),
        HudsonBackup.CHANGELOG_HISTORY_PLUGIN_DIR_NAME);
    list = changelogHistory.list();
    Assert.assertEquals(2, list.length);
  }

  @Test
  public void testBackupArchive() throws Exception {
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE - 10));

    final ThinBackupPluginImpl mockPlugin = createMockPlugin();
    when(mockPlugin.isBackupBuildArchive()).thenReturn(true);

    new HudsonBackup(mockPlugin, BackupType.FULL, cal.getTime()).backup();

    String[] list = backupDir.list();
    Assert.assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    Assert.assertEquals(6, list.length);

    final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
    List<String> arrayList = Arrays.asList(job.list());
    Assert.assertEquals(2, arrayList.size());
    Assert.assertFalse(arrayList.contains(HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME));

    final File build = new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRET_BUILD_DIRECTORY_NAME);
    arrayList = Arrays.asList(build.list());
    Assert.assertEquals(8, arrayList.size());

    final File changelogHistory = new File(
        new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRET_BUILD_DIRECTORY_NAME),
        HudsonBackup.CHANGELOG_HISTORY_PLUGIN_DIR_NAME);
    list = changelogHistory.list();
    Assert.assertEquals(2, list.length);
  }

}
