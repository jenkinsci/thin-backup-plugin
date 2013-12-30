package org.jenkins.plugins.thinbackup.strategies;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.jenkins.plugins.thinbackup.utils.FileNameMatcher;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGlobalConfigurationBackup {
  private static final String UPDATECENTER_CONFIG = "hudson.model.UpdateCenter.xml";
  private static final String NODE_CONFIG = "nodeMonitors.xml";
  private static final String IDENTITY_KEY = "identity.key";
  private static final String SECRET_KEY = "secret.key";

  private static String toBackupTempDir;
  private GlobalConfiguration globalConfiguration;

  @BeforeClass
  public static void init() {
    String systemTempDir = System.getProperty("java.io.tmpdir");
    File newTempDir = new File(systemTempDir, "thinBackupTests/toBackup");
    newTempDir.mkdirs();
    toBackupTempDir = newTempDir.getPath();
  }

  @Before
  public void setup() {
    // @formatter:off
    File[] toBackup = new File[] { 
        new File(toBackupTempDir, UPDATECENTER_CONFIG), 
        new File(toBackupTempDir, NODE_CONFIG),
        new File(toBackupTempDir, IDENTITY_KEY), 
        new File(toBackupTempDir, SECRET_KEY), 
    };
    // @formatter:on

    for (File file : toBackup) {
      try {
        file.createNewFile();
      } catch (IOException e) {
        // continue quietly
      }
    }

    globalConfiguration = new GlobalConfiguration(new File(toBackupTempDir));
  }

  @After
  public void tearDown() throws IOException {
    globalConfiguration = null;
    FileUtils.cleanDirectory(new File(toBackupTempDir));
  }

  @AfterClass
  public static void cleanup() throws IOException {
    FileUtils.deleteDirectory(new File(toBackupTempDir).getParentFile());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void backupAvailableXMLConfiguration() {
    List<File> backupedFiles = globalConfiguration.backup();

    // @formatter:off
    assertThat(backupedFiles, Matchers.containsInAnyOrder(
        new FileNameMatcher(new File(UPDATECENTER_CONFIG)), 
        new FileNameMatcher(new File(NODE_CONFIG)),
        new FileNameMatcher(new File(SECRET_KEY)), 
        new FileNameMatcher(new File(IDENTITY_KEY))));
    // @formatter:on
  }
  
  @Test
  public void backupOnlyConfiguration() throws IOException {
    new File(toBackupTempDir, "someThingNotToBackup").createNewFile();
    
    List<File> backupedFiles = globalConfiguration.backup();

    assertThat(backupedFiles, Matchers.hasItem(Matchers.not(new FileNameMatcher(new File("someThingNotToBackup")))));
  }
  
  @Test
  public void doNotBackupFolders() throws IOException {
    new File(toBackupTempDir, "jobs").mkdir();
    
    List<File> backupedFiles = globalConfiguration.backup();

    assertThat(backupedFiles, Matchers.hasItem(Matchers.not(new FileNameMatcher(new File("jobs")))));
  }
  
  @Test
  public void backupUnknownConfiguration() throws IOException {
    new File(toBackupTempDir, "thinBackup.xml").createNewFile();
    
    List<File> backupedFiles = globalConfiguration.backup();

    assertThat(backupedFiles, Matchers.hasItem(new FileNameMatcher(new File("thinBackup.xml"))));
  }

}
