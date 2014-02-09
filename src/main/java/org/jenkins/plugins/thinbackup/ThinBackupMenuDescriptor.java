package org.jenkins.plugins.thinbackup;

import hudson.model.Descriptor;

public abstract class ThinBackupMenuDescriptor extends Descriptor<ThinBackupMenu> {
  @Override
  public String getDisplayName() {
      return clazz.getSimpleName();
  }
}
