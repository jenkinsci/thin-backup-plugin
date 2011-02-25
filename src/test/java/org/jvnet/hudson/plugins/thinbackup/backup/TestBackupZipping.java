package org.jvnet.hudson.plugins.thinbackup.backup;

import java.io.File;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.Assert;

import org.junit.Test;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;

public class TestBackupZipping extends HudsonDirectoryStructureSetup {

  @Test
  public void testThinBackupZipper() throws Exception {
    new HudsonBackup(backupDir, root, BackupType.FULL, -1, false, false).backup();

    // create a backed up structure
    final TestHudsonBackup tester = new TestHudsonBackup();
    tester.setUp();
    tester.testHudsonDiffBackup();

    final ThinBackupZipper zipper = new ThinBackupZipper();
    zipper.moveOldBackupsToZipFile(backupDir, null);

    final File[] files = backupDir.listFiles();
    Assert.assertEquals(1, files.length);

    final ZipFile zipFile = new ZipFile(files[0]);
    final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
    int entryCount = 0;
    while (zipEntries.hasMoreElements()) {
      zipEntries.nextElement();
      ++entryCount;
    }
    Assert.assertEquals(15, entryCount);
    zipFile.close();
  }

}
