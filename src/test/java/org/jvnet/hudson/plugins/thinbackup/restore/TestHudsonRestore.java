package org.jvnet.hudson.plugins.thinbackup.restore;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.jvnet.hudson.plugins.thinbackup.FileCollector;
import org.jvnet.hudson.plugins.thinbackup.HudsonDirectoryStructureSetup;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;
import org.jvnet.hudson.plugins.thinbackup.backup.BackupSet;
import org.jvnet.hudson.plugins.thinbackup.backup.HudsonBackup;
import org.jvnet.hudson.plugins.thinbackup.backup.TestHudsonBackup;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class TestHudsonRestore extends HudsonDirectoryStructureSetup {

  private ThinBackupPluginImpl createMockPlugin() {
    final ThinBackupPluginImpl mockPlugin = mock(ThinBackupPluginImpl.class);

    when(mockPlugin.getHudsonHome()).thenReturn(root);
    when(mockPlugin.getFullBackupSchedule()).thenReturn("");
    when(mockPlugin.getDiffBackupSchedule()).thenReturn("");
    when(mockPlugin.getExpandedBackupPath()).thenReturn(backupDir.getAbsolutePath());
    when(mockPlugin.getNrMaxStoredFull()).thenReturn(-1);
    when(mockPlugin.isCleanupDiff()).thenReturn(false);
    when(mockPlugin.isMoveOldBackupsToZipFile()).thenReturn(false);
    when(mockPlugin.isBackupBuildResults()).thenReturn(true);
    when(mockPlugin.isBackupBuildArchive()).thenReturn(false);
    // when(mockPlugin.isBackupNextBuildNumber()).thenReturn(false);
    when(mockPlugin.getExcludedFilesRegex()).thenReturn("");

    return mockPlugin;
  }

  private ThinBackupPluginImpl createMockPluginNoNextBuildNumber() {
    final ThinBackupPluginImpl mockPlugin = createMockPlugin();

    when(mockPlugin.isBackupNextBuildNumber()).thenReturn(false);

    return mockPlugin;
  }

  private ThinBackupPluginImpl createMockPluginBackupNextBuildNumber() {
    final ThinBackupPluginImpl mockPlugin = createMockPlugin();

    when(mockPlugin.isBackupNextBuildNumber()).thenReturn(true);

    return mockPlugin;
  }

  @Test
  public void testRestoreFromDirectory() throws Exception {
    // create a backed up structure with DIFF back ups
    final TestHudsonBackup tester = new TestHudsonBackup();
    tester.setup();
    tester.performHudsonDiffBackup(createMockPluginNoNextBuildNumber());

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
        Utils.getDateFromBackupDirectory(tmpBackupDir), false);
    restore.restore();

    final FileCollector fc = new FileCollector();
    final List<String> restoredFiles = fc.getFilesAsString(root);
    final int nrRestored = restoredFiles.size();
    Assert.assertEquals(originalFiles.size(), nrRestored + 2); // + 2 because original has more files that were not
                                                               // backed up on purpose (next build number, secret.key)
  }

  @Test
  public void testRestoreFromZipFile() throws Exception {
    // create a backed up structure with DIFF back ups
    final TestHudsonBackup tester = new TestHudsonBackup();
    tester.setup();
    tester.performHudsonDiffBackup(createMockPluginNoNextBuildNumber());

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

    final HudsonRestore restore = new HudsonRestore(root, backupDir.getAbsolutePath(), backupDate, false);
    restore.restore();

    final FileCollector fc = new FileCollector();
    final List<String> restoredFiles = fc.getFilesAsString(root);
    final int nrRestored = restoredFiles.size();
    Assert.assertEquals(originalFiles.size(), nrRestored + 2); // + 2 because original has more files that were not
                                                               // backed up on purpose (next build number, secret.key)
    Assert.assertFalse(containsStringEndingWith(restoredFiles, HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME));
    Assert.assertFalse(containsStringEndingWith(restoredFiles, "secret.key"));
  }

  @Test
  public void testRestoreFromDirectoryWithNextBuildNumber() throws Exception {
    // create a backed up structure with DIFF back ups
    final TestHudsonBackup tester = new TestHudsonBackup();
    tester.setup();
    tester.performHudsonDiffBackup(createMockPluginBackupNextBuildNumber());

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
        Utils.getDateFromBackupDirectory(tmpBackupDir), true);
    restore.restore();

    final FileCollector fc = new FileCollector();
    final List<String> restoredFiles = fc.getFilesAsString(root);
    final int nrRestored = restoredFiles.size();
    Assert.assertEquals(originalFiles.size(), nrRestored + 1); // + 2 because original has more files that were not
                                                               // backed up on purpose (next build number, secret.key)
    Assert.assertTrue(containsStringEndingWith(restoredFiles, HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME));
    Assert.assertFalse(containsStringEndingWith(restoredFiles, "secret.key"));
  }

  @Test
  public void testRestoreFromDirectoryBackupNextBuildNumberButDoNotRestore() throws Exception {
    // create a backed up structure with DIFF back ups
    final TestHudsonBackup tester = new TestHudsonBackup();
    tester.setup();
    tester.performHudsonDiffBackup(createMockPluginBackupNextBuildNumber());

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
        Utils.getDateFromBackupDirectory(tmpBackupDir), false);
    restore.restore();

    final FileCollector fc = new FileCollector();
    final List<String> restoredFiles = fc.getFilesAsString(root);
    final int nrRestored = restoredFiles.size();
    Assert.assertEquals(originalFiles.size(), nrRestored + 2); // + 2 because original has more files that were not
                                                               // backed up on purpose (next build number, secret.key)
    Assert.assertFalse(containsStringEndingWith(restoredFiles, HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME));
    Assert.assertFalse(containsStringEndingWith(restoredFiles, "secret.key"));
  }

  @Test
  public void testRestoreFromZipFileWithNextBuildNumber() throws Exception {
    // create a backed up structure with DIFF back ups
    final TestHudsonBackup tester = new TestHudsonBackup();
    tester.setup();
    tester.performHudsonDiffBackup(createMockPluginBackupNextBuildNumber());

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

    final HudsonRestore restore = new HudsonRestore(root, backupDir.getAbsolutePath(), backupDate, true);
    restore.restore();

    final FileCollector fc = new FileCollector();
    final List<String> restoredFiles = fc.getFilesAsString(root);
    final int nrRestored = restoredFiles.size();
    Assert.assertEquals(originalFiles.size(), nrRestored + 1);// + 2 because original has more files that were not
                                                              // backed up on purpose (secret.key)
    Assert.assertTrue(containsStringEndingWith(restoredFiles, HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME));
    Assert.assertFalse(containsStringEndingWith(restoredFiles, "secret.key"));
  }

}
