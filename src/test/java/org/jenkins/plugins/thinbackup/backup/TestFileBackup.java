package org.jenkins.plugins.thinbackup.backup;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.jenkins.plugins.thinbackup.exceptions.BackupException;
import org.jenkins.plugins.thinbackup.strategies.AbstractTestUtils;
import org.jenkins.plugins.thinbackup.strategies.Strategy;
import org.jenkins.plugins.thinbackup.utils.FileNameMatcher;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFileBackup {

  private static File toBackupDir;
  private static File backupDir;

  @BeforeClass
  public static void init() {
    toBackupDir = new File(AbstractTestUtils.createTempToBackupDirectory());
    backupDir = new File(AbstractTestUtils.createTempBackupDirectory());
  }
  
  @After
  public void teardown() throws IOException {
    FileUtils.cleanDirectory(toBackupDir);
    FileUtils.cleanDirectory(backupDir);
  }
  
  @AfterClass
  public static void cleanup() throws IOException {
    AbstractTestUtils.removeTempJenkinsHome(backupDir.getParentFile());
  }
  
  @Test
  public void findRegisteredStrategies() {
    List<? extends Strategy> list = new ArrayList<Strategy>();
    FileBackup backup = new FileBackup(backupDir, list);
    
    assertEquals(list, backup.getRegisteredStrategies());
  }
  
  @Test
  public void backupFile() throws Exception {
    File tempFile = File.createTempFile("toBackup", ".tmp"); 
    
    Strategy mockStrategy = mock(Strategy.class);
    when(mockStrategy.backup()).thenReturn(Arrays.asList(tempFile));

    List<Strategy> list = new ArrayList<Strategy>();
    list.add(mockStrategy);
    FileBackup backup = new FileBackup(backupDir, list);
    
    backup.backup();
    
    assertThat(backupDir.listFiles(), Matchers.arrayContainingInAnyOrder(new FileNameMatcher(tempFile.getName())));
  }
  
  @Test
  public void backupFiles() throws Exception {
    File temp1File = new File(toBackupDir, "toBackup1.tmp");
    temp1File.createNewFile();
    File temp2File = new File(toBackupDir, "toBackup2.tmp");
    temp2File.createNewFile();
    
    Strategy mockStrategy = mock(Strategy.class);
    when(mockStrategy.backup()).thenReturn(Arrays.asList(toBackupDir, temp1File, temp2File));

    List<Strategy> list = new ArrayList<Strategy>();
    list.add(mockStrategy);
    FileBackup backup = new FileBackup(backupDir, list);
    
    backup.backup();
    
    assertThat(backupDir.listFiles(), Matchers.arrayContainingInAnyOrder(
        new FileNameMatcher(temp1File.getName()),
        new FileNameMatcher(temp2File.getName()),
        new FileNameMatcher(toBackupDir.getName())));
  }
  
  @Test(expected = BackupException.class)
  public void cannotBackup() throws Exception {
    File temp1File = new File(toBackupDir, "toBackup1.tmp");
    
    Strategy mockStrategy = mock(Strategy.class);
    when(mockStrategy.backup()).thenReturn(Arrays.asList(temp1File));

    List<Strategy> list = new ArrayList<Strategy>();
    list.add(mockStrategy);
    FileBackup backup = new FileBackup(backupDir, list);
    
    backup.backup();
  }

}
