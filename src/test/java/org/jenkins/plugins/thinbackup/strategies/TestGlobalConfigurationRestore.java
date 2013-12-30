package org.jenkins.plugins.thinbackup.strategies;

import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestGlobalConfigurationRestore {
  private static final String UPDATECENTER_CONFIG = "hudson.model.UpdateCenter.xml";
  private static final String NODE_CONFIG = "nodeMonitors.xml";
  private static final String IDENTITY_KEY = "identity.key";
  private static final String SECRET_KEY = "secret.key";

  private static String backupedTempDir;
  private static String restoredTempDir;
  private GlobalConfiguration globalConfiguration;
  private List<File> backuped;

  @BeforeClass
  public static void init() {
    String systemTempDir = System.getProperty("java.io.tmpdir");
    
    File newTempDir = new File(systemTempDir, "thinBackupTests/backuped");
    newTempDir.mkdirs();
    backupedTempDir = newTempDir.getPath();
    
    newTempDir = new File(systemTempDir, "thinBackupTests/restored");
    newTempDir.mkdirs();
    restoredTempDir = newTempDir.getPath();
  }
  
  @Before
  public void setup() throws IOException {
    backuped = Arrays.asList( 
        new File(backupedTempDir, UPDATECENTER_CONFIG), 
        new File(backupedTempDir, NODE_CONFIG),
        new File(backupedTempDir, IDENTITY_KEY), 
        new File(backupedTempDir, SECRET_KEY) 
    );
    
    for (File file : backuped) {
      file.createNewFile();
    }

    globalConfiguration = new GlobalConfiguration(new File(restoredTempDir));
  }

  @After
  public void tearDown() throws IOException {
    globalConfiguration = null;
    FileUtils.cleanDirectory(new File(restoredTempDir));
    FileUtils.cleanDirectory(new File(backupedTempDir));
  }
  
  @AfterClass
  public static void cleanup() throws IOException {
    FileUtils.deleteDirectory(new File(restoredTempDir).getParentFile());
  }

  @Test
  public void restoreAvailableXMLConfiguration() {
    globalConfiguration.restore(backuped);

    String[] restored = new File(restoredTempDir).list();
    
    // @formatter:off
    assertThat(restored, Matchers.arrayContainingInAnyOrder(
        UPDATECENTER_CONFIG, 
        NODE_CONFIG,
        SECRET_KEY, 
        IDENTITY_KEY));
    // @formatter:on
  }
  
  @Test
  public void doNotRestoreFolders() {
    backuped = new ArrayList<File>(backuped);
    backuped.add(new File("jobs", "job1"));
    globalConfiguration.restore(backuped);

    List<String> restored = Arrays.asList(new File(restoredTempDir).list());
    
    assertThat(restored, Matchers.hasItem(Matchers.not("jobs")));
  }
  
  @Test
  public void overrideConfigurationsAllreadyExists() throws IOException {
    new File(restoredTempDir, UPDATECENTER_CONFIG).createNewFile();
    
    globalConfiguration.restore(backuped);    

    List<String> restored = Arrays.asList(new File(restoredTempDir).list());
    
    assertThat(restored, Matchers.hasItem(Matchers.not("jobs")));
  }
  
  @Test @Ignore
  public void missingFileWritePermission() {
    // TODO: find out how to simulate a missing file permission
  }
}
