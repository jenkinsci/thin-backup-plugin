package org.jenkins.plugins.thinbackup;

import hudson.Extension;

@Extension
public final class BackupNow extends ThinBackupMenu {

  @Override
  public String getIconPath() {
    return "/plugin/thinBackup/images/backup.png";
  }

  @Override
  public String getDisplayName() {
    return "Backup Now";
  }

  @Override
  public String getDescription() {
    return "Manually trigger a immediate backup.";
  }
}
