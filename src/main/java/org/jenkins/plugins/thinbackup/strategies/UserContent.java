package org.jenkins.plugins.thinbackup.strategies;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jenkins.plugins.thinbackup.exceptions.RestoreException;

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
  public void restore(List<File> toRestore) throws RestoreException {
    for (File file : toRestore) {
      try {
        if (file.getAbsolutePath().contains(File.separator+UserContent.ROOTFOLDER_NAME+File.separator))
          FileUtils.copyFileToDirectory(file, new File(getJenkinsHome(), UserContent.ROOTFOLDER_NAME));
      } catch (IOException e) {
        throw new RestoreException("Cannot restore user content.", e);
      }
    }
  }

}
