package org.jvnet.hudson.plugins.thinbackup.utils;

@SuppressWarnings("serial")
public class EnvironmentVariableNotDefinedException extends IllegalArgumentException {

  public EnvironmentVariableNotDefinedException() {
    super();
  }

  public EnvironmentVariableNotDefinedException(final String message) {
    super(message);
  }

}
