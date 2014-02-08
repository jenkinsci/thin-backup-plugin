package org.jenkins.plugins.thinbackup.strategies;

import hudson.Extension;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jenkins.plugins.thinbackup.exceptions.RestoreException;

@Extension
public final class GlobalConfiguration extends Strategy {

  @Override
  public Collection<File> backup() {
    // formatter:off
    IOFileFilter suffixFilter = FileFilterUtils.or(
        FileFilterUtils.suffixFileFilter(".xml"), 
        FileFilterUtils.suffixFileFilter(".key"));
    // formatter:on
    FileFilter filter = FileFilterUtils.and(FileFileFilter.FILE, suffixFilter);
    
    return Arrays.asList(getJenkinsHome().listFiles(filter));
  }

  @Override
  public void restore(Collection<File> toRestore) throws RestoreException {
    for (File file : toRestore) {
      try {
        if (file.isFile())
          FileUtils.copyFileToDirectory(file, getJenkinsHome());
      } catch (IOException e) {
        throw new RestoreException("Cannot restore global configuration file.", e);
      }
    }
  }

}
