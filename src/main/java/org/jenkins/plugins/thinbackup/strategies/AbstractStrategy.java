package org.jenkins.plugins.thinbackup.strategies;

import java.io.File;

abstract class AbstractStrategy {

  private final File jenkinsHome;

  public AbstractStrategy(File jenkinsHome) {
    this.jenkinsHome = jenkinsHome;
  }

  public File getJenkinsHome() {
    return jenkinsHome;
  }
}
