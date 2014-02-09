package org.jenkins.plugins.thinbackup;

import hudson.ExtensionPoint;
import hudson.model.Describable;

import java.util.List;

import jenkins.model.Jenkins;

public abstract class ThinBackupMenu implements ExtensionPoint, Describable<ThinBackupMenu> {

  public static List<ThinBackupMenu> all() {
    return Jenkins.getInstance().getExtensionList(ThinBackupMenu.class);
  }

  public Object getUrlName() {
    return getClass().getSimpleName();
  }

  public ThinBackupMenuDescriptor getDescriptor() {
    return (ThinBackupMenuDescriptor) Jenkins.getInstance().getDescriptorOrDie(getClass());
  }
  
  public abstract String getIconPath();
  
  public abstract String getDisplayName();
  
  public abstract String getDescription();
}
