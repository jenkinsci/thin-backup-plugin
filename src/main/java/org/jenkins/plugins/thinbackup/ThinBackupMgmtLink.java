package org.jenkins.plugins.thinbackup;

import hudson.Extension;
import hudson.model.ManagementLink;

import java.util.List;

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

  public ThinBackupMenu getDynamic(String name) {
    for (ThinBackupMenu menu : getAll())
      if (menu.getUrlName().equals(name))
        return menu;
    return null;
  }

  public List<ThinBackupMenu> getAll() {
    return ThinBackupMenu.all();
  }

}
