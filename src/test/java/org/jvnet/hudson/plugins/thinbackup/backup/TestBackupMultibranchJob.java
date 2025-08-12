package org.jvnet.hudson.plugins.thinbackup.backup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.jvnet.hudson.plugins.thinbackup.TestHelper.newFolder;

import hudson.model.FreeStyleProject;
import java.io.File;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.plugins.thinbackup.TestHelper;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class TestBackupMultibranchJob {

    @TempDir
    private File tmpFolder;

    @Test
    void testFullBuildResultsBackup(JenkinsRule r) throws Exception {
        File backupDir = newFolder(tmpFolder, "junit");

        final ThinBackupPluginImpl thinBackupPlugin = ThinBackupPluginImpl.get();
        thinBackupPlugin.setBackupPath(backupDir.getAbsolutePath());
        thinBackupPlugin.setBackupBuildResults(true);
        final File rootDir = r.jenkins.getRootDir();
        final Date date = new Date();

        // create job
        FreeStyleProject project = r.createFreeStyleProject("test");
        project.getRootDir();

        TestHelper.addNewBuildToJob(project.getRootDir());

        TestHelper.addSingleMultibranchResult(project.getRootDir());

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

        assertTrue(new File(jobBackup, HudsonBackup.MULTIBRANCH_DIR_NAME).exists());
        assertTrue(new File(jobBackup, HudsonBackup.MULTIBRANCH_DIR_NAME + "/master").exists());
        assertTrue(new File(jobBackup, HudsonBackup.MULTIBRANCH_DIR_NAME + "/development").exists());
        assertTrue(new File(
                        jobBackup,
                        HudsonBackup.MULTIBRANCH_DIR_NAME + "/master/" + HudsonBackup.BUILDS_DIR_NAME + "/"
                                + TestHelper.CONCRETE_BUILD_DIRECTORY_NAME + "/build.xml")
                .exists());
        assertTrue(new File(
                        jobBackup,
                        HudsonBackup.MULTIBRANCH_DIR_NAME + "/development/" + HudsonBackup.BUILDS_DIR_NAME + "/"
                                + TestHelper.CONCRETE_BUILD_DIRECTORY_NAME + "/build.xml")
                .exists());

        assertTrue(new File(jobBackup, HudsonBackup.INDEXING_DIR_NAME).exists());
        assertTrue(new File(jobBackup, HudsonBackup.INDEXING_DIR_NAME + "/indexing.xml").exists());
        assertTrue(new File(jobBackup, HudsonBackup.INDEXING_DIR_NAME + "/indexing.log").exists());
    }
}
