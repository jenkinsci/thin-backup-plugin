package org.jenkins.plugins.thinbackup;

import hudson.Extension;
import hudson.model.ManagementLink;

@Extension
public class ThinBackupMgmtLink extends ManagementLink {

  @Override
  public String getDisplayName() {
    return "ThinBackup";
  }

  @Override
  public String getIconFileName() {
    return "package.gif";
  }

  @Override
  public String getUrlName() {
    return "thinBackup";
  }
  
  @Override
  public String getDescription() {
    return "Backup your global and job specific configuration.";
  }

}
