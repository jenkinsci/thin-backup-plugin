package org.jenkins.plugins.thinbackup.utils;

import java.io.File;

import org.hamcrest.CustomMatcher;

public class FileNameMatcher extends CustomMatcher<File> {

  private final File wanted;

  public FileNameMatcher(File wanted) {
    super(wanted.getName());
    this.wanted = wanted;
  }
  
  @Override
  public boolean matches(Object item) {
    if (item instanceof File) {
      return ((File) item).getName().equals(wanted.getName());
    }
    
    return false;
  }

}
