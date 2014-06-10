package org.jenkins.plugins.thinbackup.utils;

import java.io.File;

import org.hamcrest.CustomMatcher;

public class FileContentMatcher extends CustomMatcher<File> {

  public FileContentMatcher(String value) {
    super(value);
    // TODO Auto-generated constructor stub
  }

  @Override
  public boolean matches(Object item) {
    // TODO Auto-generated method stub
    return false;
  }


}
