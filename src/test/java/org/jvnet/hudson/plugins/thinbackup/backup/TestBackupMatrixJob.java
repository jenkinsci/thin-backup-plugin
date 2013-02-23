package org.jvnet.hudson.plugins.thinbackup.backup;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.ItemGroup;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.plugins.thinbackup.TestHelper;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class TestBackupMatrixJob {
  
  private File backupDir;
  private File jenkinsHome;

  @Before
  public void setup() throws IOException {
    File base = new File(System.getProperty("java.io.tmpdir"));
    backupDir = TestHelper.createBackupFolder(base);

    jenkinsHome = TestHelper.createBasicFolderStructure(base);
    File jobDir = TestHelper.createJob(jenkinsHome, TestHelper.TEST_JOB_NAME);
    TestHelper.addNewBuildToJob(jobDir);
    
    TestHelper.addSingleConfigurationResult(jobDir);
  }
  
  @After
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
    when(mockPlugin.isBackupNextBuildNumber()).thenReturn(false);
    when(mockPlugin.getExcludedFilesRegex()).thenReturn("");

    return mockPlugin;
  }
  
  @Test
  public void testFullBuildResultsBackup() throws IOException {
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE - 10));

    final ThinBackupPluginImpl mockPlugin = createMockPlugin();

    new HudsonBackup(mockPlugin, BackupType.FULL, cal.getTime(), mock(ItemGroup.class)).backup();
    
    String[] list = backupDir.list();
    Assert.assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    Assert.assertEquals(6, list.length);
    
    File jobBackup = new File(backup, "jobs/"+TestHelper.TEST_JOB_NAME);
    
    Assert.assertTrue(new File(jobBackup, HudsonBackup.CONFIGURATIONS_DIR_NAME).exists());
    Assert.assertTrue(new File(jobBackup, HudsonBackup.CONFIGURATIONS_DIR_NAME+"/axis-x/a").exists());
    Assert.assertTrue(new File(jobBackup, HudsonBackup.CONFIGURATIONS_DIR_NAME+"/axis-x/b").exists());
    Assert.assertTrue(new File(jobBackup, HudsonBackup.CONFIGURATIONS_DIR_NAME+"/axis-x/a/"+HudsonBackup.BUILDS_DIR_NAME+"/"+TestHelper.CONCRET_BUILD_DIRECTORY_NAME+"/build.xml").exists());
    Assert.assertTrue(new File(jobBackup, HudsonBackup.CONFIGURATIONS_DIR_NAME+"/axis-x/b/"+HudsonBackup.BUILDS_DIR_NAME+"/"+TestHelper.CONCRET_BUILD_DIRECTORY_NAME+"/build.xml").exists());
  }

}
