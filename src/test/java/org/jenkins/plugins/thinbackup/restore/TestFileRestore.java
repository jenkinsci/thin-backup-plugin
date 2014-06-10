package org.jenkins.plugins.thinbackup.restore;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jenkins.plugins.thinbackup.exceptions.RestoreException;
import org.jenkins.plugins.thinbackup.strategies.Strategy;
import org.jenkins.plugins.thinbackup.utils.TestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class TestFileRestore {
  private static File backupDir;
  private static File restoreDir;

  @BeforeClass
  public static void init() {
    backupDir = new File(TestUtils.createTempBackupDirectory());
    restoreDir = new File(TestUtils.createTempRestoredDirectory());
  }
  
  @After
  public void teardown() throws IOException {
    FileUtils.cleanDirectory(backupDir);
    FileUtils.cleanDirectory(restoreDir);
  }
  
  @AfterClass
  public static void cleanup() throws IOException {
    TestUtils.removeTempJenkinsHome(backupDir.getParentFile());
  }
  
  @Test
  public void findRegisteredStrategies() {
    List<? extends Strategy> list = new ArrayList<Strategy>();
    FileRestore restore = new FileRestore(restoreDir, list);
    
    assertEquals(list, restore.getRegisteredStrategies());
  }
  
  @Test
  public void restoreFile() throws Exception {
    File tempFile = new File(backupDir, "backuped.tmp");
    tempFile.createNewFile();
    
    Strategy mockStrategy = mock(Strategy.class);
    when(mockStrategy.getJenkinsHome()).thenReturn(restoreDir);

    List<Strategy> list = new ArrayList<Strategy>();
    list.add(mockStrategy);
    FileRestore restore = new FileRestore(backupDir, list);
    
    restore.restore();
    verify(mockStrategy).restore(Mockito.anyCollectionOf(File.class));
  }
  
  @Test
  public void restoreFiles() throws Exception {
    File temp1File = new File(backupDir, "restored1.tmp");
    temp1File.createNewFile();
    File temp2File = new File(backupDir, "restored2.tmp");
    temp2File.createNewFile();
    
    Strategy mockStrategy = mock(Strategy.class);
    when(mockStrategy.getJenkinsHome()).thenReturn(restoreDir);

    List<Strategy> list = new ArrayList<Strategy>();
    list.add(mockStrategy);
    FileRestore restore = new FileRestore(backupDir, list);
    
    restore.restore();
    verify(mockStrategy).restore(Arrays.asList(temp1File, temp2File));
  }
  
  @Test(expected = RestoreException.class)
  public void cannotRestore() throws Exception {
    Strategy mockStrategy = mock(Strategy.class);
    doThrow(new RestoreException("", new IOException())).when(mockStrategy).restore(Collections.<File>emptyList());

    List<Strategy> list = new ArrayList<Strategy>();
    list.add(mockStrategy);
    FileRestore restore = new FileRestore(restoreDir, list);
    
    restore.restore();
  }

}
