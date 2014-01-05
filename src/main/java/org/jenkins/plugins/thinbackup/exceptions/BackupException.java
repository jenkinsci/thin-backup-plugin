package org.jenkins.plugins.thinbackup.exceptions;


public class BackupException extends Exception {
  private static final long serialVersionUID = 1L;

  public BackupException(String message, Exception e) {
    super(message, e);
  }

}
