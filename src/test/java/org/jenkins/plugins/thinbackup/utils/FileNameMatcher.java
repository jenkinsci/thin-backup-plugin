package org.jenkins.plugins.thinbackup.utils;

import java.io.File;

import org.hamcrest.CustomMatcher;

public class FileNameMatcher extends CustomMatcher<File> {

  private final String wanted;

  public FileNameMatcher(File wanted) {
    super(wanted.getName());
    this.wanted = wanted.getName();
  }
  
  public FileNameMatcher(String wanted) {
    super(wanted);
    this.wanted = wanted;
  }
  
  @Override
  public boolean matches(Object item) {
    if (item instanceof File) {
      return ((File) item).getAbsolutePath().endsWith(wanted);
    }
    
    return false;
  }

}
