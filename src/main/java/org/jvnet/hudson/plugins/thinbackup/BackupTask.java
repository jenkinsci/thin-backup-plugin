package org.jvnet.hudson.plugins.thinbackup;

import java.util.TimerTask;

public class BackupTask extends TimerTask {
  public ThinBackupPluginImpl plugin;

  public BackupTask(final ThinBackupPluginImpl plugin) {
    this.plugin = plugin;
  }

  @Override
  public void run() {
//    plugin.runBackup();
//    plugin.scheduleNextRun();
  }

}