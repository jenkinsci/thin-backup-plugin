package org.jenkins.plugins.thinbackup.strategies;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;


public abstract class AbstractBackupTestUtils {

  protected static String toBackupTempDir;

  public static void removeTempJenkinsHome(File tempJenkinsHome) throws IOException {
    FileUtils.deleteDirectory(tempJenkinsHome);
  }

  protected static String createTempToBackupDirectory() {
    String systemTempDir = System.getProperty("java.io.tmpdir");
    File newTempDir = new File(systemTempDir, "thinBackupTests/toBackup");
    newTempDir.mkdirs();
    return newTempDir.getPath();
  }

  @BeforeClass
  public static void init() {
    toBackupTempDir = AbstractBackupTestUtils.createTempToBackupDirectory();
  }

  @AfterClass
  public static void cleanup() throws IOException {
    AbstractBackupTestUtils.removeTempJenkinsHome(new File(toBackupTempDir).getParentFile());
  }

}
