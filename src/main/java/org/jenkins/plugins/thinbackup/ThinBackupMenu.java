package org.jenkins.plugins.thinbackup;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;

import java.util.List;

import jenkins.model.Jenkins;

public abstract class ThinBackupMenu extends AbstractDescribableImpl<ThinBackupMenu> implements ExtensionPoint {

  public static List<ThinBackupMenu> all() {
    return Jenkins.getInstance().getExtensionList(ThinBackupMenu.class);
  }

  public Object getUrlName() {
    return getClass().getSimpleName();
  }
  
  public abstract String getIconPath();
  
  public abstract String getDisplayName();
  
  public abstract String getDescription();
}
