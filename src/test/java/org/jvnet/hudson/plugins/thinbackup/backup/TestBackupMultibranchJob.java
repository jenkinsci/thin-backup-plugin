package org.jvnet.hudson.plugins.thinbackup.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.ItemGroup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.plugins.thinbackup.TestHelper;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;

public class TestBackupMultibranchJob {

  @TempDir
  static Path tempDir;

  private File backupDir;
  private File jenkinsHome;

  @BeforeEach
  public void setup() throws IOException, InterruptedException {
    File base = tempDir.toFile();
    backupDir = TestHelper.createBackupFolder(base);

    jenkinsHome = TestHelper.createBasicFolderStructure(base);
    File jobDir = TestHelper.createJob(jenkinsHome, TestHelper.TEST_JOB_NAME);
    TestHelper.addNewBuildToJob(jobDir);
    
    TestHelper.addSingleMultibranchResult(jobDir);
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
    when(mockPlugin.isBackupNextBuildNumber()).thenReturn(false);
    when(mockPlugin.getExcludedFilesRegex()).thenReturn("");

    return mockPlugin;
  }
  
  @Test
  public void testFullBuildResultsBackup() throws IOException {
    final ThinBackupPluginImpl mockPlugin = createMockPlugin();

    new HudsonBackup(mockPlugin, BackupType.FULL, new Date(), mock(ItemGroup.class)).backup();
    
    String[] list = backupDir.list();
    assertNotNull(list);
    assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    assertNotNull(list);
    assertEquals(6, list.length);
    
    File jobBackup = new File(backup, "jobs/"+TestHelper.TEST_JOB_NAME);
    
    assertTrue(new File(jobBackup, HudsonBackup.MULTIBRANCH_DIR_NAME).exists());
    assertTrue(new File(jobBackup, HudsonBackup.MULTIBRANCH_DIR_NAME+"/master").exists());
    assertTrue(new File(jobBackup, HudsonBackup.MULTIBRANCH_DIR_NAME+"/development").exists());
    assertTrue(new File(jobBackup, HudsonBackup.MULTIBRANCH_DIR_NAME+"/master/"+HudsonBackup.BUILDS_DIR_NAME+"/"+TestHelper.CONCRETE_BUILD_DIRECTORY_NAME +"/build.xml").exists());
    assertTrue(new File(jobBackup, HudsonBackup.MULTIBRANCH_DIR_NAME+"/development/"+HudsonBackup.BUILDS_DIR_NAME+"/"+TestHelper.CONCRETE_BUILD_DIRECTORY_NAME +"/build.xml").exists());

    assertTrue(new File(jobBackup, HudsonBackup.INDEXING_DIR_NAME).exists());
    assertTrue(new File(jobBackup, HudsonBackup.INDEXING_DIR_NAME+"/indexing.xml").exists());
    assertTrue(new File(jobBackup, HudsonBackup.INDEXING_DIR_NAME+"/indexing.log").exists());
  }
}
