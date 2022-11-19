package org.jvnet.hudson.plugins.thinbackup.backup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.plugins.thinbackup.TestHelper;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class TestBackupZipping {

  private File backupDir;
  private File jenkinsHome;

  @Before
  public void setup() throws IOException, InterruptedException {
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
    final BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(configXml.toPath()));
    bis.read(data);
    bis.close();
    final String configXmlContents = new String(data);
    Assert.assertEquals(TestHelper.CONFIG_XML_CONTENTS, configXmlContents);

    backupSetFromZip.deleteUnzipDir();
    Assert.assertFalse(backupSetFromZip.getUnzipDir().exists());
    Assert.assertFalse(backupSetFromUnzippedZip.getFullBackup().exists());
  }

}
