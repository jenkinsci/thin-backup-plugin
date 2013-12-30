package org.jenkins.plugins.thinbackup.strategies;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;


public abstract class AbstractBackupTestUtils extends AbstractTestUtils {

  protected static String toBackupTempDir;

  @BeforeClass
  public static void init() {
    toBackupTempDir = AbstractBackupTestUtils.createTempToBackupDirectory();
  }

  @AfterClass
  public static void cleanup() throws IOException {
    AbstractBackupTestUtils.removeTempJenkinsHome(new File(toBackupTempDir).getParentFile());
  }

}
