package org.jvnet.hudson.plugins.thinbackup.backup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.Assert;

import org.junit.Test;
import org.jvnet.hudson.plugins.thinbackup.HudsonDirectoryStructureSetup;

public class TestBackupZipping extends HudsonDirectoryStructureSetup {

  @Test
  public void testThinBackupZipper() throws Exception {
    // create a backed up structure with DIFF back ups
    final TestHudsonBackup tester = new TestHudsonBackup();
    tester.setup();
    tester.testHudsonDiffBackup();

    File[] files = backupDir.listFiles();
    Assert.assertEquals(2, files.length);

    final BackupSet backupSetFromDirectory = new BackupSet(files[0]);
    Assert.assertTrue(backupSetFromDirectory.isValid());
    Assert.assertFalse(backupSetFromDirectory.isInZipFile());
    Assert.assertEquals(backupSetFromDirectory, backupSetFromDirectory.unzip());

    final File zippedBackupSet = backupSetFromDirectory.zipTo(backupDir);
    Assert.assertNotNull(zippedBackupSet);

    files = backupDir.listFiles();
    Assert.assertEquals(3, files.length);

    final ZipFile zipFile = new ZipFile(zippedBackupSet);
    final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
    int entryCount = 0;
    while (zipEntries.hasMoreElements()) {
      zipEntries.nextElement();
      ++entryCount;
    }
    Assert.assertEquals(23, entryCount);
    zipFile.close();

    final BackupSet backupSetFromZip = new BackupSet(zippedBackupSet);
    Assert.assertTrue(backupSetFromZip.isValid());
    Assert.assertTrue(backupSetFromZip.isInZipFile());

    Assert.assertEquals(backupSetFromDirectory.getFullBackupName(), backupSetFromZip.getFullBackupName());
    Assert.assertEquals(backupSetFromDirectory.getDiffBackupsNames().size(), backupSetFromZip.getDiffBackupsNames()
        .size());

    final BackupSet backupSetFromUnzippedZip = backupSetFromZip.unzip();
    Assert.assertTrue(backupSetFromUnzippedZip.isValid());
    Assert.assertFalse(backupSetFromUnzippedZip.isInZipFile());
    Assert.assertNotNull(backupSetFromUnzippedZip.getFullBackup());
    Assert.assertTrue(backupSetFromUnzippedZip.getFullBackup().exists());
    Assert.assertNotNull(backupSetFromUnzippedZip.getDiffBackups());
    for (final File diffBackup : backupSetFromUnzippedZip.getDiffBackups()) {
      Assert.assertTrue(diffBackup.exists());
    }

    final File f1 = new File(backupSetFromUnzippedZip.getFullBackup(), "jobs");
    final File f2 = new File(f1, "test");
    final File configXml = new File(f2, "config.xml");

    Assert.assertEquals(20, configXml.length());
    final byte[] data = new byte[20];
    final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(configXml));
    bis.read(data);
    bis.close();
    final String configXmlContents = new String(data);
    Assert.assertEquals(CONFIG_XML_CONTENTS, configXmlContents);

    backupSetFromZip.deleteUnzipDir();
    Assert.assertFalse(backupSetFromZip.getUnzipDir().exists());
    Assert.assertFalse(backupSetFromUnzippedZip.getFullBackup().exists());
  }

}
