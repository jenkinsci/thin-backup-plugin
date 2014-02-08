package org.jenkins.plugins.thinbackup.strategies;

import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.jenkins.plugins.thinbackup.exceptions.RestoreException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestUserContentRestore extends AbstractRestoreTestUtils {
  private UserContent userContent;

  @Before
  public void setup() throws IOException {
    userContent = new UserContent();
    userContent.setJenkinsHome(new File(restoredTempDir));
  }

  @After
  public void tearDown() throws IOException {
    userContent = null;
  }
  
  @Test
  public void nothingShouledBeRestored() throws RestoreException {
    userContent.restore(backuped);
    
    List<String> restored = Arrays.asList(new File(restoredTempDir).list());
    
    assertThat(restored, Matchers.hasSize(0));
  }
  
  @Test
  public void restoreUserContent() throws RestoreException, IOException {
    new File(backupedTempDir, UserContent.ROOTFOLDER_NAME).mkdir();
    File readme = new File(backupedTempDir+File.separator+UserContent.ROOTFOLDER_NAME, "readme.txt");
    readme.createNewFile();
    backuped = new ArrayList<File>(backuped);
    backuped.add(readme);
    
    userContent.restore(backuped);
    
    List<String> restored = Arrays.asList(new File(restoredTempDir).list());
    assertThat(restored, Matchers.contains(UserContent.ROOTFOLDER_NAME));
    
    restored = Arrays.asList(new File(restoredTempDir, UserContent.ROOTFOLDER_NAME).list());
    assertThat(restored, Matchers.contains("readme.txt"));
  }
  
  @Test(expected = RestoreException.class)
  public void cannotRestore() throws IOException, RestoreException {
    userContent = new UserContent();
    userContent.setJenkinsHome(new File("c:\fileDoNotExist"));
    
    File root = new File(backupedTempDir, UserContent.ROOTFOLDER_NAME);
    root.mkdir();
    File backuped = new File(root, "readme.txt");
    backuped.createNewFile();
    userContent.restore(Arrays.asList(backuped));
  }

}
