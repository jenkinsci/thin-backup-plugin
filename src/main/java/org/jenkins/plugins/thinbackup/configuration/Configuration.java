package org.jenkins.plugins.thinbackup.configuration;

import hudson.model.ModelObject;

public class Configuration implements ModelObject {
  private final String backupPath;

  public Configuration(String backupPath) {
    this.backupPath = backupPath;
  }

  @Override
  public String getDisplayName() {
    return "ThinBackupConfiguration";
  }

  public String getBackupPath() {
    return backupPath;
  }
}
