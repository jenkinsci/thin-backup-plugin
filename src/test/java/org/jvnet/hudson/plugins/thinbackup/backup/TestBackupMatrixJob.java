package org.jvnet.hudson.plugins.thinbackup.backup;

import hudson.model.ItemGroup;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.plugins.thinbackup.TestHelper;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBackupMatrixJob {

  @TempDir
  public Path tmpFolder;

  private File backupDir;
  private File jenkinsHome;

  @BeforeEach
  public void setUp() throws IOException, InterruptedException {
    backupDir = TestHelper.createBackupFolder(Files.createDirectory(tmpFolder.resolve("thin-backup-matrix-job")).toFile());
    jenkinsHome = TestHelper.createBasicFolderStructure(Files.createDirectory(tmpFolder.resolve("jenkins-matrix-job")).toFile());

    File jobDir = TestHelper.createJob(jenkinsHome, TestHelper.TEST_JOB_NAME);
    TestHelper.addNewBuildToJob(jobDir);

    TestHelper.addSingleConfigurationResult(jobDir);
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
    Assert.assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    Assert.assertEquals(6, list.length);
    
    File jobBackup = new File(backup, "jobs/"+TestHelper.TEST_JOB_NAME);
    
    Assert.assertTrue(new File(jobBackup, HudsonBackup.CONFIGURATIONS_DIR_NAME).exists());
    Assert.assertTrue(new File(jobBackup, HudsonBackup.CONFIGURATIONS_DIR_NAME+"/axis-x/a").exists());
    Assert.assertTrue(new File(jobBackup, HudsonBackup.CONFIGURATIONS_DIR_NAME+"/axis-x/b").exists());
    Assert.assertTrue(new File(jobBackup, HudsonBackup.CONFIGURATIONS_DIR_NAME+"/axis-x/a/"+HudsonBackup.BUILDS_DIR_NAME+"/"+TestHelper.CONCRETE_BUILD_DIRECTORY_NAME +"/build.xml").exists());
    Assert.assertTrue(new File(jobBackup, HudsonBackup.CONFIGURATIONS_DIR_NAME+"/axis-x/b/"+HudsonBackup.BUILDS_DIR_NAME+"/"+TestHelper.CONCRETE_BUILD_DIRECTORY_NAME +"/build.xml").exists());
  }

}
