package org.jvnet.hudson.plugins.thinbackup.backup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.jvnet.hudson.plugins.thinbackup.TestHelper.newFolder;

import hudson.model.FreeStyleProject;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class TestBackupZipping {

    @TempDir
    private File tmpFolder;

    @Test
    void testThinBackupZipper(JenkinsRule r) throws Exception {
        File backupDir = newFolder(tmpFolder, "junit");

        final ThinBackupPluginImpl thinBackupPlugin = ThinBackupPluginImpl.get();
        thinBackupPlugin.setBackupPath(backupDir.getAbsolutePath());
        thinBackupPlugin.setBackupBuildResults(true);

        Instant now = Instant.now(); // current date
        Instant before = now.minus(Duration.ofDays(7));
        Date dateBefore = Date.from(before);

        final File rootDir = r.jenkins.getRootDir();
        final Date date = Date.from(now);

        // create job
        r.createFreeStyleProject("freeStyleJob");

        // create folder
        final MockFolder folder1 = r.createFolder("folder1");

        // create job in folder1
        folder1.createProject(FreeStyleProject.class, "elements");

        // run backup full
        new HudsonBackup(thinBackupPlugin, ThinBackupPeriodicWork.BackupType.FULL, dateBefore, r.jenkins).backup();

        // create another job
        folder1.createProject(FreeStyleProject.class, "elements2");

        // run backup diff
        new HudsonBackup(thinBackupPlugin, ThinBackupPeriodicWork.BackupType.DIFF, date, r.jenkins).backup();

        final String[] listedBackupDirs = backupDir.list();
        assertEquals(2, listedBackupDirs.length);

        Path backupFolderName = Utils.getFormattedDirectory(
                        backupDir, ThinBackupPeriodicWork.BackupType.FULL, dateBefore)
                .toPath();
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

        // create BackupSet
        final File[] listedFiles = backupDir.listFiles();
        assertEquals(2, listedFiles.length);
        final BackupSet backupSetFromDirectory = new BackupSet(listedFiles[0]);

        assertTrue(backupSetFromDirectory.isValid());
        assertFalse(backupSetFromDirectory.isInZipFile());
        assertEquals(backupSetFromDirectory, backupSetFromDirectory.unzip());

        // zip files
        final File zippedBackupSet = backupSetFromDirectory.zipTo(backupDir);
        assertNotNull(zippedBackupSet);

        File[] files = backupDir.listFiles();
        assertEquals(3, files.length);

        final ZipFile zipFile = new ZipFile(zippedBackupSet);
        final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        int entryCount = 0;
        while (zipEntries.hasMoreElements()) {
            zipEntries.nextElement();
            ++entryCount;
        }
        assertThat(entryCount, lessThan(30));
        zipFile.close();

        final BackupSet backupSetFromZip = new BackupSet(zippedBackupSet);
        assertTrue(backupSetFromZip.isValid());
        assertTrue(backupSetFromZip.isInZipFile());

        assertEquals(backupSetFromDirectory.getFullBackupName(), backupSetFromZip.getFullBackupName());
        assertEquals(
                backupSetFromDirectory.getDiffBackupsNames().size(),
                backupSetFromZip.getDiffBackupsNames().size());

        final BackupSet backupSetFromUnzippedZip = backupSetFromZip.unzip();
        assertTrue(backupSetFromUnzippedZip.isValid());
        assertFalse(backupSetFromUnzippedZip.isInZipFile());
        assertNotNull(backupSetFromUnzippedZip.getFullBackup());
        assertTrue(backupSetFromUnzippedZip.getFullBackup().exists());
        assertNotNull(backupSetFromUnzippedZip.getDiffBackups());
        for (final File diffBackup : backupSetFromUnzippedZip.getDiffBackups()) {
            assertTrue(diffBackup.exists());
        }
    }
}
