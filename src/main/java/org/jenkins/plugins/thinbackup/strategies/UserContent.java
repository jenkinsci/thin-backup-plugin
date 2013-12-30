package org.jenkins.plugins.thinbackup.strategies;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

final class UserContent extends AbstractStrategy implements IStrategy {
  static final String ROOTFOLDER_NAME = "userContent";
  
  public UserContent(File jenkinsHome) {
    super(jenkinsHome);
  }

  @Override
  public Collection<File> backup() {
    File userContent = new File(getJenkinsHome(), ROOTFOLDER_NAME);
    return FileUtils.listFiles(userContent, null, true);
  }

  @Override
  public void restore(List<File> toRestore) {
    // TODO Auto-generated method stub
    
  }

}
