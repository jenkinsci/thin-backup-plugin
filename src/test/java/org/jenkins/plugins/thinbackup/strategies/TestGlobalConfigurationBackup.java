package org.jenkins.plugins.thinbackup.strategies;

import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.jenkins.plugins.thinbackup.utils.FileNameMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestGlobalConfigurationBackup extends AbstractBackupTestUtils {
  private static final String UPDATECENTER_CONFIG = "hudson.model.UpdateCenter.xml";
  private static final String NODE_CONFIG = "nodeMonitors.xml";
  private static final String IDENTITY_KEY = "identity.key";
  private static final String SECRET_KEY = "secret.key";

  private GlobalConfiguration globalConfiguration;

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

    globalConfiguration = new GlobalConfiguration();
    globalConfiguration.setJenkinsHome(new File(toBackupTempDir));
  }

  @After
  public void tearDown() throws IOException {
    globalConfiguration = null;
    FileUtils.cleanDirectory(new File(toBackupTempDir));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void backupAvailableXMLConfiguration() {
    Collection<File> backupedFiles = globalConfiguration.backup();

    // @formatter:off
    assertThat(backupedFiles, Matchers.containsInAnyOrder(
        new FileNameMatcher(UPDATECENTER_CONFIG), 
        new FileNameMatcher(NODE_CONFIG),
        new FileNameMatcher(SECRET_KEY), 
        new FileNameMatcher(IDENTITY_KEY)));
    // @formatter:on
  }
  
  @Test
  public void backupOnlyConfiguration() throws IOException {
    new File(toBackupTempDir, "someThingNotToBackup").createNewFile();
    
    Collection<File> backupedFiles = globalConfiguration.backup();

    assertThat(backupedFiles, Matchers.hasItem(Matchers.not(new FileNameMatcher("someThingNotToBackup"))));
  }
  
  @Test
  public void doNotBackupFolders() throws IOException {
    new File(toBackupTempDir, "jobs").mkdir();
    
    Collection<File> backupedFiles = globalConfiguration.backup();

    assertThat(backupedFiles, Matchers.hasItem(Matchers.not(new FileNameMatcher("jobs"))));
  }
  
  @Test
  public void backupUnknownConfiguration() throws IOException {
    new File(toBackupTempDir, "thinBackup.xml").createNewFile();
    
    Collection<File> backupedFiles = globalConfiguration.backup();

    assertThat(backupedFiles, Matchers.hasItem(new FileNameMatcher("thinBackup.xml")));
  }

}
