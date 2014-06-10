package org.jenkins.plugins.thinbackup.strategies;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jenkins.plugins.thinbackup.utils.TestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class AbstractRestoreTestUtils extends TestUtils {

  protected static String backupedTempDir;
  protected static String restoredTempDir;

  protected List<File> backuped;

  @BeforeClass
  public static void initTemporaryDirectories() {
    backupedTempDir = createTempToBackupDirectory();
    restoredTempDir = createTempRestoredDirectory();
  }
  

  @Before
  public void setupCommonBackupFileList() throws IOException {
    backuped = Arrays.asList( 
        new File(backupedTempDir, UPDATECENTER_CONFIG), 
        new File(backupedTempDir, NODE_CONFIG),
        new File(backupedTempDir, IDENTITY_KEY), 
        new File(backupedTempDir, SECRET_KEY) 
    );
    
    for (File file : backuped) {
      file.createNewFile();
    }
  }
  
  @After
  public void cleanTemporaryDirectories() throws IOException {
    FileUtils.cleanDirectory(new File(restoredTempDir));
    FileUtils.cleanDirectory(new File(backupedTempDir));
  }

  @AfterClass
  public static void removeTemporaryEnvironment() throws IOException {
    FileUtils.deleteDirectory(new File(restoredTempDir).getParentFile());
  }

}
