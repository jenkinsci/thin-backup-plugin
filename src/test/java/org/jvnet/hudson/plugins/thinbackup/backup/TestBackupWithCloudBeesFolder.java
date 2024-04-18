package org.jvnet.hudson.plugins.thinbackup.backup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;

public class TestBackupWithCloudBeesFolder {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testWithFolder() throws IOException {
        File backupDir = tmpFolder.newFolder();

        final ThinBackupPluginImpl thinBackupPlugin = ThinBackupPluginImpl.get();
        thinBackupPlugin.setBackupPath(backupDir.getAbsolutePath());
        thinBackupPlugin.setBackupBuildResults(true);
        final File rootDir = r.jenkins.getRootDir();
        final Date date = new Date();

        // create job
        r.createFreeStyleProject("freeStyleJob");

        // create folder
        final MockFolder folder1 = r.createFolder("folder1");

        // create job in folder1
        folder1.createProject(FreeStyleProject.class, "elements");

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

        assertTrue(new File(backupFolderName.toFile(), "jobs/folder1/jobs/elements/config.xml").exists());

        // check folder and job is there
        File jobBackup = new File(backupFolderName.toFile(), "jobs");
        final List<String> listedFolder = List.of(jobBackup.list());
        assertThat(listedFolder, containsInAnyOrder("folder1", "freeStyleJob"));

        // check folder for config and jobs folder
        File elementBackup = new File(jobBackup, "folder1");
        final List<String> listedElements = List.of(elementBackup.list());
        assertThat(listedElements, containsInAnyOrder("config.xml", "jobs"));

        // check job is in folder
        File folderJobDir = new File(elementBackup, "jobs/elements");
        final List<String> listedJobElements = List.of(folderJobDir.list());
        assertThat(listedJobElements, containsInAnyOrder("config.xml", "builds"));
    }
}
