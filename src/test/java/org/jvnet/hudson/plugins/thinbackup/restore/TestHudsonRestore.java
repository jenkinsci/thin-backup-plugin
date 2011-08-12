package org.jvnet.hudson.plugins.thinbackup.restore;

import java.io.File;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.jvnet.hudson.plugins.thinbackup.FileCollector;
import org.jvnet.hudson.plugins.thinbackup.HudsonDirectoryStructureSetup;
import org.jvnet.hudson.plugins.thinbackup.backup.BackupSet;
import org.jvnet.hudson.plugins.thinbackup.backup.TestHudsonBackup;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class TestHudsonRestore extends HudsonDirectoryStructureSetup {

  @Test
  public void testRestoreFromDirectory() throws Exception {
    // create a backed up structure with DIFF back ups
    final TestHudsonBackup tester = new TestHudsonBackup();
    tester.setup();
    tester.testHudsonDiffBackup();

    // now destroy the hudson directory and recreate the root dir
    FileUtils.deleteDirectory(root);
    root.mkdirs();

    final File[] files = backupDir.listFiles();
    Assert.assertEquals(2, files.length);
    final File tmpBackupDir = files[0];
    final BackupSet set = new BackupSet(tmpBackupDir);
    Assert.assertTrue(set.isValid());
    Assert.assertFalse(set.isInZipFile());

    final HudsonRestore restore = new HudsonRestore(root, backupDir.getAbsolutePath(),
        Utils.getDateFromBackupDirectory(tmpBackupDir));
    restore.restore();

    final FileCollector fc = new FileCollector();
    final List<String> restoredFiles = fc.getFilesAsString(root);
    final int nrRestored = restoredFiles.size();
    Assert.assertEquals(originalFiles.size(), nrRestored + 2); // + 2 because original has more files that were not
                                                               // backed up on purpose
  }

  @Test
  public void testRestoreFromZipFile() throws Exception {
    // create a backed up structure with DIFF back ups
    final TestHudsonBackup tester = new TestHudsonBackup();
    tester.setup();
    tester.testHudsonDiffBackup();

    // remember the date to restore
    final File[] tmpFiles = backupDir.listFiles();
    Assert.assertEquals(2, tmpFiles.length);
    final Date backupDate = Utils.getDateFromBackupDirectory(tmpFiles[0]);

    // move backups to ZIP file
    Utils.moveOldBackupsToZipFile(backupDir, null);

    // now destroy the hudson directory and recreate the root dir
    FileUtils.deleteDirectory(root);
    root.mkdirs();

    final File[] files = backupDir.listFiles();
    Assert.assertEquals(1, files.length);
    final File zipFile = files[0];
    final BackupSet set = new BackupSet(zipFile);
    Assert.assertTrue(set.isValid());
    Assert.assertTrue(set.isInZipFile());

    final HudsonRestore restore = new HudsonRestore(root, backupDir.getAbsolutePath(), backupDate);
    restore.restore();

    final FileCollector fc = new FileCollector();
    final List<String> restoredFiles = fc.getFilesAsString(root);
    final int nrRestored = restoredFiles.size();
    Assert.assertEquals(originalFiles.size(), nrRestored + 2); // + 2 because original has more files that were not
                                                               // backed up on purpose
  }

}
