package org.jenkins.plugins.thinbackup.strategies;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

final class GlobalConfiguration extends AbstractStrategy implements IStrategy {

  public GlobalConfiguration(File jenkinsHome) {
    super(jenkinsHome);
  }

  @Override
  public List<File> backup() {
    // formatter:off
    IOFileFilter suffixFilter = FileFilterUtils.or(
        FileFilterUtils.suffixFileFilter(".xml"), 
        FileFilterUtils.suffixFileFilter(".key"));
    // formatter:on
    FileFilter filter = FileFilterUtils.and(FileFileFilter.FILE, suffixFilter);
    
    return Arrays.asList(getJenkinsHome().listFiles(filter));
  }

  @Override
  public void restore(List<File> toRestore) {
    // TODO Auto-generated method stub
    
  }

}
