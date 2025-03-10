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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.jvnet.hudson.plugins.thinbackup.TestHelper.newFile;
import static org.jvnet.hudson.plugins.thinbackup.TestHelper.newFolder;

import hudson.model.FreeStyleProject;
import hudson.model.Label;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.plugins.thinbackup.TestHelper;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class TestHudsonBackup {

    @TempDir
    private File tmpFolder;

    @Test
    void testBackup(JenkinsRule r) throws Exception {
        File backupDir = newFolder(tmpFolder, "junit");

        final ThinBackupPluginImpl thinBackupPlugin = ThinBackupPluginImpl.get();
        thinBackupPlugin.setBackupPath(backupDir.getAbsolutePath());
        thinBackupPlugin.setBackupBuildResults(true);
        final File rootDir = r.jenkins.getRootDir();
        final Date date = new Date();

        // create job
        final FreeStyleProject test = r.createFreeStyleProject("test");
        File buildDir = TestHelper.addNewBuildToJob(test.getRootDir());

        // create empty job
        TestHelper.createMaliciousMultiJob(rootDir, "jobs");

        // run backup
        new HudsonBackup(thinBackupPlugin, BackupType.FULL, date, r.jenkins).backup();
        final String[] listedBackupDirs = backupDir.list();
        assertEquals(1, listedBackupDirs.length);

        Path backupFolderName =
                Utils.getFormattedDirectory(backupDir, BackupType.FULL, date).toPath();
        final List<String> listedBackupFiles = List.of(backupFolderName.toFile().list());

        // check basic files are there
        assertThat(
                listedBackupFiles,
                hasItems(
                        "installedPlugins.xml",
                        "config.xml",
                        "org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl.xml",
                        "hudson.model.UpdateCenter.xml",
                        "jobs"));

        String[] list = backupDir.list();
        assertNotNull(list);
        assertEquals(1, list.length);
        final File backup = new File(backupDir, list[0]);
        list = backup.list();
        assertNotNull(list);
        assertThat(list.length, greaterThan(6));

        final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
        final List<String> arrayList = Arrays.asList(job.list());
        assertEquals(2, arrayList.size());
        assertFalse(arrayList.contains(HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME));

        final File build =
                new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME);
        list = build.list();
        assertNotNull(list);
        assertEquals(7, list.length);

        final File changelogHistory = new File(
                new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME),
                HudsonBackup.CHANGELOG_HISTORY_PLUGIN_DIR_NAME);
        list = changelogHistory.list();
        assertNotNull(list);
        assertEquals(2, list.length);
    }

    @Test
    void testBackupWithExcludes(JenkinsRule r) throws Exception {
        File backupDir = newFolder(tmpFolder, "junit");

        final ThinBackupPluginImpl thinBackupPlugin = ThinBackupPluginImpl.get();
        thinBackupPlugin.setBackupPath(backupDir.getAbsolutePath());
        thinBackupPlugin.setBackupBuildResults(true);
        thinBackupPlugin.setExcludedFilesRegex("^.*\\.(log)$");
        final File rootDir = r.jenkins.getRootDir();
        final Date date = new Date();

        // create job
        final FreeStyleProject test = r.createFreeStyleProject("test");
        File buildDir = TestHelper.addNewBuildToJob(test.getRootDir());

        // add some log files
        newFile(buildDir, "log"); // should be copied
        newFile(buildDir, "log.txt"); // should be copied
        newFile(buildDir, "logfile.log"); // should NOT be copied
        newFile(buildDir, "logfile.xlog"); // should be copied

        // create empty job
        TestHelper.createMaliciousMultiJob(rootDir, "jobs/emptyJob");

        // run backup
        new HudsonBackup(thinBackupPlugin, BackupType.FULL, date, r.jenkins).backup();

        // verify
        String[] list = backupDir.list();
        assertNotNull(list);
        assertEquals(1, list.length);
        final File backup = new File(backupDir, list[0]);
        list = backup.list();
        assertNotNull(list);
        assertThat(list.length, greaterThan(6));

        final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
        list = job.list();
        assertNotNull(list);
        assertEquals(2, list.length);

        final File build =
                new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME);
        list = build.list();
        assertNotNull(list);
        assertEquals(7, list.length);

        List<String> buildList = List.of(list);
        assertThat(buildList, not(hasItem("logfile.log")));
    }

    @Test
    void testBackupWithoutBuildResults(JenkinsRule r) throws Exception {
        File backupDir = newFolder(tmpFolder, "junit");

        final ThinBackupPluginImpl thinBackupPlugin = ThinBackupPluginImpl.get();
        thinBackupPlugin.setBackupPath(backupDir.getAbsolutePath());
        thinBackupPlugin.setBackupBuildResults(false);
        final File rootDir = r.jenkins.getRootDir();
        final Date date = new Date();

        // create job
        final FreeStyleProject test = r.createFreeStyleProject("test");
        File buildDir = TestHelper.addNewBuildToJob(test.getRootDir());

        // add some log files
        newFile(buildDir, "log");
        newFile(buildDir, "log.txt");
        newFile(buildDir, "logfile.log");
        newFile(buildDir, "logfile.xlog");

        // run backup
        new HudsonBackup(thinBackupPlugin, BackupType.FULL, date, r.jenkins).backup();

        String[] list = backupDir.list();
        assertNotNull(list);
        assertEquals(1, list.length);
        final File backup = new File(backupDir, list[0]);
        list = backup.list();
        assertNotNull(list);
        // check basic files are there
        assertThat(
                List.of(list),
                hasItems(
                        "installedPlugins.xml",
                        "config.xml",
                        "org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl.xml",
                        "hudson.model.UpdateCenter.xml",
                        "jobs"));

        final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
        list = job.list();
        assertNotNull(list);
        assertEquals(1, list.length);
        assertEquals("config.xml", list[0]);
    }

    @Test
    void testBackupNextBuildNumber(JenkinsRule r) throws Exception {
        File backupDir = newFolder(tmpFolder, "junit");

        final ThinBackupPluginImpl thinBackupPlugin = ThinBackupPluginImpl.get();
        thinBackupPlugin.setBackupPath(backupDir.getAbsolutePath());
        thinBackupPlugin.setBackupBuildResults(true);
        thinBackupPlugin.setBackupNextBuildNumber(true);
        final File rootDir = r.jenkins.getRootDir();
        final Date date = new Date();

        // create job
        final FreeStyleProject test = r.createFreeStyleProject("test");
        File buildDir = TestHelper.addNewBuildToJob(test.getRootDir());

        // add nextBuildNumber file
        newFile(test.getRootDir(), "nextBuildNumber");

        // run backup
        new HudsonBackup(thinBackupPlugin, BackupType.FULL, date, r.jenkins).backup();
        final String[] listedBackupDirs = backupDir.list();
        assertEquals(1, listedBackupDirs.length);

        Path backupFolderName =
                Utils.getFormattedDirectory(backupDir, BackupType.FULL, date).toPath();
        final List<String> listedBackupFiles = List.of(backupFolderName.toFile().list());

        // check basic files are there
        assertThat(
                listedBackupFiles,
                hasItems(
                        "installedPlugins.xml",
                        "config.xml",
                        "org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl.xml",
                        "hudson.model.UpdateCenter.xml",
                        "jobs"));

        String[] list = backupDir.list();
        assertNotNull(list);
        assertEquals(1, list.length);
        final File backup = new File(backupDir, list[0]);
        list = backup.list();
        assertNotNull(list);
        assertThat(list.length, greaterThan(6));

        final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
        final List<String> arrayList = Arrays.asList(job.list());
        assertEquals(3, arrayList.size());
        assertThat(arrayList, hasItem(containsString(HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME)));

        final File build =
                new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME);
        list = build.list();
        assertNotNull(list);
        assertEquals(7, list.length);

        final File changelogHistory = new File(
                new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME),
                HudsonBackup.CHANGELOG_HISTORY_PLUGIN_DIR_NAME);
        list = changelogHistory.list();
        assertNotNull(list);
        assertEquals(2, list.length);
    }

    @Test
    void testBackupArchive(JenkinsRule r) throws Exception {
        File backupDir = newFolder(tmpFolder, "junit");

        final ThinBackupPluginImpl thinBackupPlugin = ThinBackupPluginImpl.get();
        thinBackupPlugin.setBackupPath(backupDir.getAbsolutePath());
        thinBackupPlugin.setBackupBuildArchive(true);
        final File rootDir = r.jenkins.getRootDir();
        final Date date = new Date();

        // create job
        final FreeStyleProject test = r.createFreeStyleProject("test");
        File buildDir = TestHelper.addNewBuildToJob(test.getRootDir());

        // run backup
        new HudsonBackup(thinBackupPlugin, BackupType.FULL, date, r.jenkins).backup();
        final String[] listedBackupDirs = backupDir.list();
        assertEquals(1, listedBackupDirs.length);

        Path backupFolderName =
                Utils.getFormattedDirectory(backupDir, BackupType.FULL, date).toPath();
        final List<String> listedBackupFiles = List.of(backupFolderName.toFile().list());

        // check basic files are there
        assertThat(
                listedBackupFiles,
                hasItems(
                        "installedPlugins.xml",
                        "config.xml",
                        "org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl.xml",
                        "hudson.model.UpdateCenter.xml",
                        "jobs"));

        String[] list = backupDir.list();
        assertNotNull(list);
        assertEquals(1, list.length);
        final File backup = new File(backupDir, list[0]);
        list = backup.list();
        assertNotNull(list);
        assertThat(list.length, greaterThan(6));

        final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
        List<String> arrayList = Arrays.asList(job.list());
        assertEquals(2, arrayList.size());
        assertFalse(arrayList.contains(HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME));

        final File build =
                new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME);
        arrayList = Arrays.asList(build.list());
        assertEquals(8, arrayList.size());

        final File changelogHistory = new File(
                new File(new File(job, HudsonBackup.BUILDS_DIR_NAME), TestHelper.CONCRETE_BUILD_DIRECTORY_NAME),
                HudsonBackup.CHANGELOG_HISTORY_PLUGIN_DIR_NAME);
        list = changelogHistory.list();
        assertNotNull(list);
        assertEquals(2, list.length);
    }

    @Test
    void testBackupKeptBuildsOnly_doKeep(JenkinsRule r) throws Exception {
        File backupDir = newFolder(tmpFolder, "junit");

        final ThinBackupPluginImpl thinBackupPlugin = ThinBackupPluginImpl.get();
        thinBackupPlugin.setBackupPath(backupDir.getAbsolutePath());
        thinBackupPlugin.setBackupBuildsToKeepOnly(true);
        final File rootDir = r.jenkins.getRootDir();
        final Date date = new Date();

        // create job
        final FreeStyleProject test = r.createFreeStyleProject("test");

        File buildDir = TestHelper.addNewBuildToJob(test.getRootDir());

        // add some log files
        newFile(buildDir, "log");
        newFile(buildDir, "log.txt");

        // run backup
        new HudsonBackup(thinBackupPlugin, BackupType.FULL, date, r.jenkins).backup();
        final String[] listedBackupDirs = backupDir.list();
        assertEquals(1, listedBackupDirs.length);

        Path backupFolderName =
                Utils.getFormattedDirectory(backupDir, BackupType.FULL, date).toPath();
        final List<String> listedBackupFiles = List.of(backupFolderName.toFile().list());

        // check basic files are there
        assertThat(
                listedBackupFiles,
                hasItems(
                        "installedPlugins.xml",
                        "config.xml",
                        "org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl.xml",
                        "hudson.model.UpdateCenter.xml",
                        "jobs"));

        String[] list = backupDir.list();
        assertNotNull(list);
        assertEquals(1, list.length);
        final File backup = new File(backupDir, list[0]);
        list = backup.list();
        assertNotNull(list);
        assertThat(list.length, greaterThan(6));

        final File job = new File(new File(backup, HudsonBackup.JOBS_DIR_NAME), TestHelper.TEST_JOB_NAME);
        list = job.list();
        assertNotNull(list);
        assertEquals(2, list.length);
        assertThat(List.of(list), containsInAnyOrder("config.xml", "builds"));
    }

    @Test
    void testRemovingEmptyDirs(JenkinsRule r) throws Exception {
        File backupDir = newFolder(tmpFolder, "junit");

        final ThinBackupPluginImpl thinBackupPlugin = ThinBackupPluginImpl.get();
        thinBackupPlugin.setBackupPath(backupDir.getAbsolutePath());
        thinBackupPlugin.setBackupBuildResults(true);
        final File rootDir = r.jenkins.getRootDir();
        final Date date = new Date();

        // create job
        final FreeStyleProject test = r.createFreeStyleProject("test");
        TestHelper.addNewBuildToJob(test.getRootDir());

        // create a couple of empty dirs
        for (int i = 0; i < 10; i++) {
            File folder = newFolder(rootDir, "empty" + i);
            for (int j = 0; j < 3; j++) {
                File folder2 = newFolder(folder, "child" + j);
            }
        }

        // count files before
        List<String> filesAndFolders;
        try (Stream<Path> walk = Files.walk(rootDir.toPath())) {
            // exclude symbolic links, we ignore these in this test
            filesAndFolders = walk.filter(file -> !Files.isSymbolicLink(file))
                    .map(Path::toString)
                    .toList();
        }

        assertThat(filesAndFolders.size(), greaterThan(70));

        // run backup (which will clean up empty folders)
        new HudsonBackup(thinBackupPlugin, BackupType.FULL, date, r.jenkins).backup();
        final String[] listedBackupDirs = backupDir.list();
        assertEquals(1, listedBackupDirs.length);

        // count files
        try (Stream<Path> walk = Files.walk(backupDir.toPath())) {
            // exclude symbolic links, we ignore these in this test
            filesAndFolders = walk.filter(file -> !Files.isSymbolicLink(file))
                    .map(Path::toString)
                    .toList();
        }
        assertThat(filesAndFolders.size(), lessThan(30));
    }

    @Test
    void testBackupNodes(JenkinsRule r) throws Exception {
        File backupDir = newFolder(tmpFolder, "junit");

        final ThinBackupPluginImpl thinBackupPlugin = ThinBackupPluginImpl.get();
        thinBackupPlugin.setBackupPath(backupDir.getAbsolutePath());
        thinBackupPlugin.setBackupBuildResults(true);
        final File rootDir = r.jenkins.getRootDir();
        final Date date = new Date();

        // create job
        final FreeStyleProject test = r.createFreeStyleProject("test");
        TestHelper.addNewBuildToJob(test.getRootDir());

        r.createSlave(Label.get("label"));

        new HudsonBackup(thinBackupPlugin, BackupType.FULL, date, r.jenkins).backup();

        String[] list = backupDir.list();
        assertNotNull(list);
        assertEquals(1, list.length);
        final File backup = new File(backupDir, list[0]);
        list = backup.list();
        assertNotNull(list);
        assertThat(list.length, greaterThan(7));

        final File nodes = new File(backup, HudsonBackup.NODES_DIR_NAME);
        list = nodes.list();
        assertNotNull(list);
        assertEquals(1, list.length);
        assertEquals("slave0", list[0]);

        final File node = new File(nodes, "slave0");
        list = node.list();
        assertNotNull(list);
        assertEquals(1, list.length);
        assertEquals(HudsonBackup.CONFIG_XML, list[0]);
    }
}
