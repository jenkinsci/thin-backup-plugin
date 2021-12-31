/*
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

import hudson.model.FreeStyleBuild;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.FreeStyleProject;
import hudson.util.RunList;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.plugins.thinbackup.TestHelper;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class TestHudsonBackup {

  private File backupDir;
  private File jenkinsHome;
  private File buildDir;
  private ItemGroup<TopLevelItem> mockHudson;

  @Before
  public void setup() throws IOException, InterruptedException {
    mockHudson = mock(ItemGroup.class);
    
    File base = new File(System.getProperty("java.io.tmpdir"));
    backupDir = TestHelper.createBackupFolder(base);

    jenkinsHome = TestHelper.createBasicFolderStructure(base);
    File jobDir = TestHelper.createJob(jenkinsHome, TestHelper.TEST_JOB_NAME);
    TestHelper.createMaliciousMultiJob(jenkinsHome, "emptyJob");
    buildDir = TestHelper.addNewBuildToJob(jobDir);
  }
  
  @After
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(jenkinsHome);
    FileUtils.deleteDirectory(backupDir);
    FileUtils.deleteDirectory(new File(Utils.THINBACKUP_TMP_DIR));
  }
  
  @Test
  public void testBackup() throws Exception {
    final ThinBackupPluginImpl mockPlugin = TestHelper.createMockPlugin(jenkinsHome, backupDir);

    new HudsonBackup(mockPlugin, BackupType.FULL, new Date(), mockHudson).backup();

    String[] list = backupDir.list();
    Assert.assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    Assert.assertEquals(6, list.length);

    final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
    final List<String> arrayList = Arrays.asList(job.list());
    Assert.assertEquals(2, arrayList.size());
    Assert.assertFalse(arrayList.contains(HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME));

    final File build = new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME);
    list = build.list();
    Assert.assertEquals(7, list.length);

    final File changelogHistory = new File(
        new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME),
        HudsonBackup.CHANGELOG_HISTORY_PLUGIN_DIR_NAME);
    list = changelogHistory.list();
    Assert.assertEquals(2, list.length);
  }  

  @Test
  public void testBackupWithExcludes() throws Exception {
    final ThinBackupPluginImpl mockPlugin = TestHelper.createMockPlugin(jenkinsHome, backupDir);
    when(mockPlugin.getExcludedFilesRegex()).thenReturn("^.*\\.(log)$");

    new HudsonBackup(mockPlugin, BackupType.FULL, new Date(), mockHudson).backup();

    String[] list = backupDir.list();
    Assert.assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    Assert.assertEquals(6, list.length);

    final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
    list = job.list();
    Assert.assertEquals(2, list.length);

    final File build = new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME);
    list = build.list();
    Assert.assertEquals(6, list.length);

    final File changelogHistory = new File(
        new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME),
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
    final ThinBackupPluginImpl mockPlugin = TestHelper.createMockPlugin(jenkinsHome, backupDir);
    when(mockPlugin.isBackupBuildResults()).thenReturn(false);

    new HudsonBackup(mockPlugin, BackupType.FULL, new Date(), mockHudson).backup();

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
    cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE) - 10);

    new HudsonBackup(mockPlugin, BackupType.FULL, cal.getTime(), mockHudson).backup();

    // fake modification
    backupDir.listFiles((FileFilter) FileFilterUtils.prefixFileFilter(BackupType.FULL.toString()))[0]
        .setLastModified(System.currentTimeMillis() - 60000 * 60);

    for (final File globalConfigFile : jenkinsHome.listFiles()) {
      globalConfigFile.setLastModified(System.currentTimeMillis() - 60000 * 120);
    }

    new HudsonBackup(mockPlugin, BackupType.DIFF, new Date(), mockHudson).backup();
  }

  @Test
  public void testHudsonDiffBackup() throws Exception {
    final ThinBackupPluginImpl mockPlugin = TestHelper.createMockPlugin(jenkinsHome, backupDir);

    performHudsonDiffBackup(mockPlugin);

    final File lastDiffBackup = backupDir.listFiles((FileFilter) FileFilterUtils.prefixFileFilter(BackupType.DIFF
        .toString()))[0];
    Assert.assertEquals(1, lastDiffBackup.list().length);
  }

  @Test
  public void testBackupNextBuildNumber() throws Exception {
    final ThinBackupPluginImpl mockPlugin = TestHelper.createMockPlugin(jenkinsHome, backupDir);
    when(mockPlugin.isBackupNextBuildNumber()).thenReturn(true);

    new HudsonBackup(mockPlugin, BackupType.FULL, new Date(), mockHudson).backup();

    String[] list = backupDir.list();
    Assert.assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    Assert.assertEquals(6, list.length);

    final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
    final List<String> arrayList = Arrays.asList(job.list());
    Assert.assertEquals(3, arrayList.size());
    Assert.assertTrue(TestHelper.containsStringEndingWith(arrayList, HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME));

    final File build = new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME);
    list = build.list();
    Assert.assertEquals(7, list.length);

    final File changelogHistory = new File(
        new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME),
        HudsonBackup.CHANGELOG_HISTORY_PLUGIN_DIR_NAME);
    list = changelogHistory.list();
    Assert.assertEquals(2, list.length);
  }

  @Test
  public void testBackupArchive() throws Exception {
    final ThinBackupPluginImpl mockPlugin = TestHelper.createMockPlugin(jenkinsHome, backupDir);
    when(mockPlugin.isBackupBuildArchive()).thenReturn(true);

    new HudsonBackup(mockPlugin, BackupType.FULL, new Date(), mockHudson).backup();

    String[] list = backupDir.list();
    Assert.assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    Assert.assertEquals(6, list.length);

    final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
    List<String> arrayList = Arrays.asList(job.list());
    Assert.assertEquals(2, arrayList.size());
    Assert.assertFalse(arrayList.contains(HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME));

    final File build = new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME);
    arrayList = Arrays.asList(build.list());
    Assert.assertEquals(8, arrayList.size());

    final File changelogHistory = new File(
        new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME),
        HudsonBackup.CHANGELOG_HISTORY_PLUGIN_DIR_NAME);
    list = changelogHistory.list();
    Assert.assertEquals(2, list.length);
  }

  @Test
  public void testBackupKeptBuildsOnly_doNotKeep() throws Exception {
    final ThinBackupPluginImpl mockPlugin = TestHelper.createMockPlugin(jenkinsHome, backupDir);
    
    when(mockPlugin.isBackupBuildsToKeepOnly()).thenReturn(true);
    
    FreeStyleProject mockJob = mock(FreeStyleProject.class);
    when(mockHudson.getItem(TestHelper.TEST_JOB_NAME)).thenReturn(mockJob);
    FreeStyleBuild mockRun = mock(FreeStyleBuild.class);
    when(mockJob.getBuilds()).thenReturn(RunList.fromRuns(Collections.singleton(mockRun)));
    when(mockRun.getRootDir()).thenReturn(buildDir);

    new HudsonBackup(mockPlugin, BackupType.FULL, new Date(), mockHudson).backup();

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
  
  @Test
  public void testBackupKeptBuildsOnly_keep() throws Exception {
    final ThinBackupPluginImpl mockPlugin = TestHelper.createMockPlugin(jenkinsHome, backupDir);
    
    when(mockPlugin.isBackupBuildsToKeepOnly()).thenReturn(true);
    
    FreeStyleProject mockJob = mock(FreeStyleProject.class);
    when(mockHudson.getItem(TestHelper.TEST_JOB_NAME)).thenReturn(mockJob);
    FreeStyleBuild mockRun = mock(FreeStyleBuild.class);
    // for some reason Mockito has issues mocking this interface and so is is used to return true to isKeepLog()
    when(mockRun.getWhyKeepLog()).thenReturn("x");
    when(mockJob.getBuilds()).thenReturn(RunList.fromRuns(Collections.singleton(mockRun)));
    when(mockRun.getRootDir()).thenReturn(buildDir);

    new HudsonBackup(mockPlugin, BackupType.FULL, new Date(), mockHudson).backup();

    String[] list = backupDir.list();
    Assert.assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    Assert.assertEquals(6, list.length);

    final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
    final List<String> arrayList = Arrays.asList(job.list());
    Assert.assertEquals(2, arrayList.size());
    Assert.assertFalse(arrayList.contains(HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME));

    final File build = new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME);
    list = build.list();
    Assert.assertEquals(7, list.length);
  }
  
  @Test
  public void testBackupNodes() throws Exception {
    TestHelper.createNode(jenkinsHome, TestHelper.TEST_NODE_NAME);

    final ThinBackupPluginImpl mockPlugin = TestHelper.createMockPlugin(jenkinsHome, backupDir);

    new HudsonBackup(mockPlugin, BackupType.FULL, new Date(), mockHudson).backup();

    String[] list = backupDir.list();
    Assert.assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    Assert.assertEquals(7, list.length);

    final File nodes = new File(backup, HudsonBackup.NODES_DIR_NAME);
    list = nodes.list();
    Assert.assertEquals(1, list.length);
    Assert.assertEquals(TestHelper.TEST_NODE_NAME, list[0]);

    final File node = new File(nodes, TestHelper.TEST_NODE_NAME);
    list = node.list();
    Assert.assertEquals(1, list.length);
    Assert.assertEquals(HudsonBackup.CONFIG_XML, list[0]);
  }
}
