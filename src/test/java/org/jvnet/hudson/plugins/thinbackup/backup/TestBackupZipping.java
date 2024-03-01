package org.jvnet.hudson.plugins.thinbackup.backup;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.plugins.thinbackup.TestHelper;

public class TestBackupZipping {

    @TempDir
    public Path tmpFolder;

    private File backupDir;
    private File jenkinsHome;

    @BeforeEach
    public void setup() throws IOException {
        backupDir = TestHelper.createBackupFolder(
                Files.createDirectory(tmpFolder.resolve("thin-backup-zipping")).toFile());
        jenkinsHome = TestHelper.createBasicFolderStructure(
                Files.createDirectory(tmpFolder.resolve("backup-zipping")).toFile());

        File jobDir = TestHelper.createJob(jenkinsHome, TestHelper.TEST_JOB_NAME);
        TestHelper.addNewBuildToJob(jobDir);
    }

    @Test
    public void testThinBackupZipper() throws Exception {
        // create a backed up structure with DIFF back ups
        final TestHudsonBackup tester = new TestHudsonBackup();
        tester.backupDir = backupDir;
        tester.jenkinsHome = jenkinsHome;
        tester.setup();
        tester.testHudsonDiffBackup();

        File[] files = backupDir.listFiles();
        assertEquals(2, files.length);

        final BackupSet backupSetFromDirectory = new BackupSet(files[0]);
        assertTrue(backupSetFromDirectory.isValid());
        assertFalse(backupSetFromDirectory.isInZipFile());
        assertEquals(backupSetFromDirectory, backupSetFromDirectory.unzip());

        final File zippedBackupSet = backupSetFromDirectory.zipTo(backupDir);
        assertNotNull(zippedBackupSet);

        files = backupDir.listFiles();
        assertEquals(3, files.length);

        final ZipFile zipFile = new ZipFile(zippedBackupSet);
        final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        int entryCount = 0;
        while (zipEntries.hasMoreElements()) {
            zipEntries.nextElement();
            ++entryCount;
        }
        assertEquals(23, entryCount);
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

        final File f1 = new File(backupSetFromUnzippedZip.getFullBackup(), "jobs");
        final File f2 = new File(f1, "test");
        final File configXml = new File(f2, "config.xml");

        assertEquals(20, configXml.length());
        final byte[] data = new byte[20];
        final BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(configXml.toPath()));
        bis.read(data);
        bis.close();
        final String configXmlContents = new String(data);
        assertEquals(TestHelper.CONFIG_XML_CONTENTS, configXmlContents);

        backupSetFromZip.deleteUnzipDir();
        assertFalse(backupSetFromZip.getUnzipDir().exists());
        assertFalse(backupSetFromUnzippedZip.getFullBackup().exists());
    }
}
