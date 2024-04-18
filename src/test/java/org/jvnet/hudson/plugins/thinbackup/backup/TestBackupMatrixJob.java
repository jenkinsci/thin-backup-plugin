package org.jvnet.hudson.plugins.thinbackup.backup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleProject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.plugins.thinbackup.TestHelper;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.jvnet.hudson.test.JenkinsRule;

public class TestBackupMatrixJob {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testGeneralFilesAndFolders() throws IOException {
        File backupDir = tmpFolder.newFolder();

        final ThinBackupPluginImpl thinBackupPlugin = ThinBackupPluginImpl.get();
        thinBackupPlugin.setBackupPath(backupDir.getAbsolutePath());
        thinBackupPlugin.setBackupBuildResults(true);
        final File rootDir = r.jenkins.getRootDir();
        final Date date = new Date();

        // create job
        FreeStyleProject project = r.createFreeStyleProject("test");
        project.getRootDir();

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
    }

    @Test
    public void testMatrixFilesAndFolders() throws IOException, InterruptedException {
        File backupDir = tmpFolder.newFolder();

        final ThinBackupPluginImpl thinBackupPlugin = ThinBackupPluginImpl.get();
        thinBackupPlugin.setBackupPath(backupDir.getAbsolutePath());
        thinBackupPlugin.setBackupBuildResults(true);
        final File rootDir = r.jenkins.getRootDir();
        final Date date = new Date();

        // create job
        FreeStyleProject project = r.createFreeStyleProject("test");

        // put in additional stuff in jobs folder
        TestHelper.addNewBuildToJob(project.getRootDir());

        TestHelper.addSingleConfigurationResult(project.getRootDir());

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

        File jobBackup = new File(backupFolderName.toFile(), "jobs/" + TestHelper.TEST_JOB_NAME);

        assertTrue(new File(jobBackup, HudsonBackup.CONFIGURATIONS_DIR_NAME).exists());
        assertTrue(new File(jobBackup, HudsonBackup.CONFIGURATIONS_DIR_NAME + "/axis-x/a").exists());
        assertTrue(new File(jobBackup, HudsonBackup.CONFIGURATIONS_DIR_NAME + "/axis-x/b").exists());
        assertTrue(new File(
                        jobBackup,
                        HudsonBackup.CONFIGURATIONS_DIR_NAME + "/axis-x/a/" + HudsonBackup.BUILDS_DIR_NAME + "/"
                                + TestHelper.CONCRETE_BUILD_DIRECTORY_NAME + "/build.xml")
                .exists());
        assertTrue(new File(
                        jobBackup,
                        HudsonBackup.CONFIGURATIONS_DIR_NAME + "/axis-x/b/" + HudsonBackup.BUILDS_DIR_NAME + "/"
                                + TestHelper.CONCRETE_BUILD_DIRECTORY_NAME + "/build.xml")
                .exists());
    }
}
