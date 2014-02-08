package org.jenkins.plugins.thinbackup.strategies;

import hudson.Extension;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.jenkins.plugins.thinbackup.exceptions.RestoreException;

@Extension
public final class UserContent extends Strategy {
  static final String ROOTFOLDER_NAME = "userContent";

  @Override
  public Collection<File> backup() {
    File userContent = new File(getJenkinsHome(), ROOTFOLDER_NAME);
    return FileUtils.listFilesAndDirs(userContent, FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
  }

  @Override
  public void restore(Collection<File> toRestore) throws RestoreException {
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
