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

public class TestUserContentBackup extends AbstractBackupTestUtils {

  private UserContent userContent;

  @Before
  public void setup() throws IOException {
    File userContentRoot = new File(toBackupTempDir, UserContent.ROOTFOLDER_NAME);
    userContentRoot.mkdir();
    
    new File(userContentRoot, "readme.txt").createNewFile();
    
    userContent = new UserContent();
    userContent.setJenkinsHome(new File(toBackupTempDir));
  }
  
  @After
  public void tearDown() throws IOException {
    FileUtils.cleanDirectory(new File(toBackupTempDir, UserContent.ROOTFOLDER_NAME));
  }
  
  @Test
  public void backupUserContent() {
    Collection<File> backup = userContent.backup();
    
    assertThat(backup, Matchers.contains(
        new FileNameMatcher(UserContent.ROOTFOLDER_NAME),
        new FileNameMatcher(UserContent.ROOTFOLDER_NAME+File.separator+"readme.txt")));
  }
  
  @Test
  public void doNotBackupNonUserContentFiles() throws IOException {
    new File(toBackupTempDir, "shouldNotBeBackuped.xml").createNewFile();
    Collection<File> backup = userContent.backup();
    
    assertThat(backup, Matchers.hasItem(Matchers.not(
        new FileNameMatcher("shouldNotBeBackuped.xml"))));
  }

}
