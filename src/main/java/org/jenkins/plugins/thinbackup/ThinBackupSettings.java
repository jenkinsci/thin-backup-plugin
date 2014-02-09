package org.jenkins.plugins.thinbackup;

import org.jenkins.plugins.thinbackup.configuration.Configuration;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

@Extension
public final class ThinBackupSettings extends ThinBackupMenu {
  private Configuration configuration;
  
  @DataBoundConstructor
  public ThinBackupSettings(Configuration c) {
    this.configuration = c;    
  }
  
  @Override
  public String getIconPath() {
    return "/plugin/thinBackup/images/settings.png";
  }

  @Override
  public String getDisplayName() {
    return "Settings";
  }

  @Override
  public String getDescription() {
    return "Configure ThinBackup to your needs.";
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }
}
