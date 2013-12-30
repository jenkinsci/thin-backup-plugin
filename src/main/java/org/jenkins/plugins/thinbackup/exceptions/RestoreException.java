package org.jenkins.plugins.thinbackup.exceptions;

import java.io.IOException;

public class RestoreException extends Exception {
  private static final long serialVersionUID = 1L;

  public RestoreException(String message, IOException e) {
    super(message, e);
  }
}
