package org.jvnet.hudson.plugins.thinbackup.backup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import hudson.model.ItemGroup;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.plugins.thinbackup.TestHelper;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class TestBackupWithCloudBeesFolder {
  private static final String TEST_FOLDER = "testFolder";
  private File backupDir;
  private File jenkinsHome;
  private File cloudBeesFolder;

  @BeforeEach
  public void setup() throws IOException, InterruptedException {
    File base = new File(System.getProperty("java.io.tmpdir"));
    backupDir = TestHelper.createBackupFolder(base);

    jenkinsHome = TestHelper.createBasicFolderStructure(base);
    cloudBeesFolder = TestHelper.createCloudBeesFolder(jenkinsHome, TEST_FOLDER);
    File jobDir = TestHelper.createJob(cloudBeesFolder, TestHelper.TEST_JOB_NAME);
    TestHelper.addNewBuildToJob(jobDir);
    
  }
  
  @AfterEach
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(jenkinsHome);
    FileUtils.deleteDirectory(backupDir);
    FileUtils.deleteDirectory(new File(Utils.THINBACKUP_TMP_DIR));
  }
  
  @Test
  public void testCloudBeesFolderBackup() throws Exception {
    final ThinBackupPluginImpl mockPlugin = TestHelper.createMockPlugin(jenkinsHome, backupDir);
    new HudsonBackup(mockPlugin, BackupType.FULL, new Date(), mock(ItemGroup.class)).backup();
    
    final File backup = new File(backupDir, backupDir.list()[0]);
    File rootJobsFolder = new File(backup, HudsonBackup.JOBS_DIR_NAME);
    String[] list = rootJobsFolder.list();
    assertThat(list, Matchers.arrayContainingInAnyOrder(TEST_FOLDER));

    File cloudBeesFolder = new File(rootJobsFolder, list[0]);
    list = cloudBeesFolder.list();
    assertThat(list, Matchers.arrayContainingInAnyOrder(HudsonBackup.JOBS_DIR_NAME, HudsonBackup.CONFIG_XML));
    
    File childJobsFolder = new File(cloudBeesFolder, HudsonBackup.JOBS_DIR_NAME);
    list = childJobsFolder.list();
    assertThat(list, Matchers.arrayContainingInAnyOrder(TestHelper.TEST_JOB_NAME));
    
    File jobFolder = new File(childJobsFolder, TestHelper.TEST_JOB_NAME);
    list = jobFolder.list();
    assertThat(list, Matchers.arrayContainingInAnyOrder(HudsonBackup.BUILDS_DIR_NAME, HudsonBackup.CONFIG_XML));
  }
  
  @Test
  public void testRecursiveFolderBackup() throws Exception {
    File subFolderDirectory = TestHelper.createCloudBeesFolder(cloudBeesFolder, "subFolder");
    TestHelper.createJob(subFolderDirectory, "folderJob");
    
    final ThinBackupPluginImpl mockPlugin = TestHelper.createMockPlugin(jenkinsHome, backupDir);
    new HudsonBackup(mockPlugin, BackupType.FULL, new Date(), mock(ItemGroup.class)).backup();
    
    File subFolderJobsBackupDirectory = new File(backupDir, backupDir.list()[0]+"/jobs/"+TEST_FOLDER+"/jobs/subFolder/jobs");
    String[] list = subFolderJobsBackupDirectory.list();
    assertThat(list , Matchers.arrayContainingInAnyOrder("folderJob"));
    
    File jobFolder = new File(subFolderJobsBackupDirectory, "folderJob");
    list = jobFolder.list();
    assertThat(list, Matchers.arrayContainingInAnyOrder(HudsonBackup.CONFIG_XML));
  }

}
