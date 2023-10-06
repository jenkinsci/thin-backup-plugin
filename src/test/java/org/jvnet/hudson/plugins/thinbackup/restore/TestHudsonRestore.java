package org.jvnet.hudson.plugins.thinbackup.restore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.plugins.thinbackup.TestHelper;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;
import org.jvnet.hudson.plugins.thinbackup.backup.BackupSet;
import org.jvnet.hudson.plugins.thinbackup.backup.HudsonBackup;
import org.jvnet.hudson.plugins.thinbackup.backup.TestHudsonBackup;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class TestHudsonRestore {

  @TempDir
  public Path tmpFolder;
  private File backupDir;
  private File jenkinsHome;
  private List<Path> originalFiles;

  @BeforeEach
  public void setup() throws IOException {
    backupDir = TestHelper.createBackupFolder(Files.createDirectory(tmpFolder.resolve("hudson-restore-backup")).toFile());
    jenkinsHome = TestHelper.createBasicFolderStructure(Files.createDirectory(tmpFolder.resolve("hudson-restore-jenkins")).toFile());
    File jobDir = TestHelper.createJob(jenkinsHome, TestHelper.TEST_JOB_NAME);
    TestHelper.addNewBuildToJob(jobDir);

    try (Stream<Path> walk = Files.walk(jenkinsHome.toPath())) {
      originalFiles = walk.filter(Files::isRegularFile)
              .collect(Collectors.toList());
    }
  }
  
  @AfterEach
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(jenkinsHome);
    FileUtils.deleteDirectory(backupDir);
    FileUtils.deleteDirectory(new File(Utils.THINBACKUP_TMP_DIR));
  }

  private ThinBackupPluginImpl createMockPlugin() {
    final ThinBackupPluginImpl mockPlugin = mock(ThinBackupPluginImpl.class);
  
    when(mockPlugin.getHudsonHome()).thenReturn(jenkinsHome);
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
    tester.backupDir = backupDir;
    tester.jenkinsHome = jenkinsHome;
    tester.setup();
    tester.performHudsonDiffBackup(createMockPluginNoNextBuildNumber());

    // now destroy the hudson directory and recreate the root dir
    FileUtils.deleteDirectory(jenkinsHome);
    jenkinsHome.mkdirs();

    final File[] files = backupDir.listFiles();
    assertEquals(2, files.length);
    final File tmpBackupDir = files[0];
    final BackupSet set = new BackupSet(tmpBackupDir);
    assertTrue(set.isValid());
    assertFalse(set.isInZipFile());

    final HudsonRestore restore = new HudsonRestore(jenkinsHome, backupDir.getAbsolutePath(),
        Utils.getDateFromBackupDirectory(tmpBackupDir), false, false);
    restore.restore();

    final List<Path> restoredFiles;
    try (Stream<Path> walk = Files.walk(jenkinsHome.toPath())) {
      restoredFiles = walk.filter(Files::isRegularFile)
              .collect(Collectors.toList());
    }
    final int nrRestored = restoredFiles.size();
    assertEquals(originalFiles.size(), nrRestored + 3); // + 3 because original has more files that were not
                                                               // backed up on purpose (next build number, secret.key, workspace/neverBackupme.txt)
  }

  @Test
  public void testRestoreFromZipFile() throws Exception {
    // create a backed up structure with DIFF back ups
    final TestHudsonBackup tester = new TestHudsonBackup();
    tester.backupDir = backupDir;
    tester.jenkinsHome = jenkinsHome;
    tester.setup();
    tester.performHudsonDiffBackup(createMockPluginNoNextBuildNumber());

    // remember the date to restore
    final File[] tmpFiles = backupDir.listFiles();
    assertEquals(2, tmpFiles.length);
    final Date backupDate = Utils.getDateFromBackupDirectory(tmpFiles[0]);

    // move backups to ZIP file
    Utils.moveOldBackupsToZipFile(backupDir, null);

    // now destroy the hudson directory and recreate the root dir
    FileUtils.deleteDirectory(jenkinsHome);
    jenkinsHome.mkdirs();

    final File[] files = backupDir.listFiles();
    assertEquals(1, files.length);
    final File zipFile = files[0];
    final BackupSet set = new BackupSet(zipFile);
    assertTrue(set.isValid());
    assertTrue(set.isInZipFile());

    final HudsonRestore restore = new HudsonRestore(jenkinsHome, backupDir.getAbsolutePath(), backupDate, false, false);
    restore.restore();


    final List<String> restoredFiles;
    try (Stream<Path> walk = Files.walk(jenkinsHome.toPath())) {
      restoredFiles = walk.filter(Files::isRegularFile)
              .map(Path::toString)
              .collect(Collectors.toList());
    }

    final int nrRestored = restoredFiles.size();
    assertEquals(originalFiles.size(), nrRestored + 3); // + 3 because original has more files that were not
                                                               // backed up on purpose (next build number, secret.key, workspace/neverBackupme.txt)
    assertThat(restoredFiles, not(hasItem(containsString(HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME))));
    assertThat(restoredFiles, not(hasItem(containsString("secret.key"))));
  }

  @Test
  public void testRestoreFromDirectoryWithNextBuildNumber() throws Exception {
    // create a backed up structure with DIFF back ups
    final TestHudsonBackup tester = new TestHudsonBackup();
    tester.backupDir = backupDir;
    tester.jenkinsHome = jenkinsHome;
    tester.setup();
    tester.performHudsonDiffBackup(createMockPluginBackupNextBuildNumber());

    // now destroy the hudson directory and recreate the root dir
    FileUtils.deleteDirectory(jenkinsHome);
    jenkinsHome.mkdirs();

    final File[] files = backupDir.listFiles();
    assertEquals(2, files.length);
    final File tmpBackupDir = files[0];
    final BackupSet set = new BackupSet(tmpBackupDir);
    assertTrue(set.isValid());
    assertFalse(set.isInZipFile());

    final HudsonRestore restore = new HudsonRestore(jenkinsHome, backupDir.getAbsolutePath(),
        Utils.getDateFromBackupDirectory(tmpBackupDir), true, false);
    restore.restore();

    final List<String> restoredFiles;
    try (Stream<Path> walk = Files.walk(jenkinsHome.toPath())) {
      restoredFiles = walk.filter(Files::isRegularFile)
              .map(Path::toString)
              .collect(Collectors.toList());
    }
    final int nrRestored = restoredFiles.size();
    assertEquals(originalFiles.size(), nrRestored + 2); // + 2 because original has more files that were not
                                                               // backed up on purpose (secret.key, workspace/neverBackupme.txt)
    assertThat(restoredFiles, hasItem(containsString(HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME)));
    assertThat(restoredFiles, not(hasItem(containsString("secret.key"))));
  }

  @Test
  public void testRestoreFromDirectoryBackupNextBuildNumberButDoNotRestore() throws Exception {
    // create a backed up structure with DIFF back ups
    final TestHudsonBackup tester = new TestHudsonBackup();
    tester.backupDir = backupDir;
    tester.jenkinsHome = jenkinsHome;
    tester.setup();
    tester.performHudsonDiffBackup(createMockPluginBackupNextBuildNumber());

    // now destroy the hudson directory and recreate the root dir
    FileUtils.deleteDirectory(jenkinsHome);
    jenkinsHome.mkdirs();

    final File[] files = backupDir.listFiles();
    assertEquals(2, files.length);
    final File tmpBackupDir = files[0];
    final BackupSet set = new BackupSet(tmpBackupDir);
    assertTrue(set.isValid());
    assertFalse(set.isInZipFile());

    final HudsonRestore restore = new HudsonRestore(jenkinsHome, backupDir.getAbsolutePath(),
        Utils.getDateFromBackupDirectory(tmpBackupDir), false, false);
    restore.restore();

    final List<String> restoredFiles;
    try (Stream<Path> walk = Files.walk(jenkinsHome.toPath())) {
      restoredFiles = walk.filter(Files::isRegularFile)
              .map(Path::toString)
              .collect(Collectors.toList());
    }
    final int nrRestored = restoredFiles.size();
    assertEquals(originalFiles.size(), nrRestored + 3); // + 3 because original has more files that were not
                                                               // backed up on purpose (next build number, secret.key, workspace/neverBackupme.txt)
    assertThat(restoredFiles, not(hasItem(containsString(HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME))));
    assertThat(restoredFiles, not(hasItem(containsString("secret.key"))));
  }

  @Test
  public void testRestoreFromZipFileWithNextBuildNumber() throws Exception {
    // create a backed up structure with DIFF back ups
    final TestHudsonBackup tester = new TestHudsonBackup();
    tester.backupDir = backupDir;
    tester.jenkinsHome = jenkinsHome;
    tester.setup();
    tester.performHudsonDiffBackup(createMockPluginBackupNextBuildNumber());

    // remember the date to restore
    final File[] tmpFiles = backupDir.listFiles();
    assertEquals(2, tmpFiles.length);
    final Date backupDate = Utils.getDateFromBackupDirectory(tmpFiles[0]);

    // move backups to ZIP file
    Utils.moveOldBackupsToZipFile(backupDir, null);

    // now destroy the hudson directory and recreate the root dir
    FileUtils.deleteDirectory(jenkinsHome);
    jenkinsHome.mkdirs();

    final File[] files = backupDir.listFiles();
    assertEquals(1, files.length);
    final File zipFile = files[0];
    final BackupSet set = new BackupSet(zipFile);
    assertTrue(set.isValid());
    assertTrue(set.isInZipFile());

    final HudsonRestore restore = new HudsonRestore(jenkinsHome, backupDir.getAbsolutePath(), backupDate, true, false);
    restore.restore();

    final List<String> restoredFiles;
    try (Stream<Path> walk = Files.walk(jenkinsHome.toPath())) {
      restoredFiles = walk.filter(Files::isRegularFile)
              .map(Path::toString)
              .collect(Collectors.toList());
    }
    final int nrRestored = restoredFiles.size();
    assertEquals(originalFiles.size(), nrRestored + 2); // + 3 because original has more files that were not
                                                               // backed up on purpose (secret.key, workspace/neverBackupme.txt)
    assertThat(restoredFiles, hasItem(containsString(HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME)));
    assertThat(restoredFiles, not(hasItem(containsString("secret.key"))));
  }

}
