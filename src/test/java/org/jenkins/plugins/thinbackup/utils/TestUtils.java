package org.jenkins.plugins.thinbackup.utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class TestUtils {
  protected static final String UPDATECENTER_CONFIG = "hudson.model.UpdateCenter.xml";
  protected static final String NODE_CONFIG = "nodeMonitors.xml";
  protected static final String IDENTITY_KEY = "identity.key";
  protected static final String SECRET_KEY = "secret.key";
  
  public static void removeTempJenkinsHome(File tempJenkinsHome) throws IOException {
    FileUtils.deleteDirectory(tempJenkinsHome);
  }

  public static String createTempToBackupDirectory() {
    String systemTempDir = System.getProperty("java.io.tmpdir");
    File newTempDir = new File(systemTempDir, "thinBackupTests"+File.separator+"toBackup");
    newTempDir.mkdirs();
    return newTempDir.getPath();
  }
  
  public static String createTempRestoredDirectory() {
    String systemTempDir = System.getProperty("java.io.tmpdir");
    File newTempDir = new File(systemTempDir, "thinBackupTests"+File.separator+"restored");
    newTempDir.mkdirs();
    return newTempDir.getPath();
  }
  
  public static String createTempBackupDirectory() {
    String systemTempDir = System.getProperty("java.io.tmpdir");
    File newTempDir = new File(systemTempDir, "thinBackupTests"+File.separator+"backuped");
    newTempDir.mkdirs();
    return newTempDir.getPath();
  }
}
