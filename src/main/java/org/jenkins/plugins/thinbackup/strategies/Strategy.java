package org.jenkins.plugins.thinbackup.strategies;

import hudson.ExtensionList;
import hudson.ExtensionPoint;

import java.io.File;
import java.util.Collection;

import org.jenkins.plugins.thinbackup.exceptions.RestoreException;

import jenkins.model.Jenkins;

public abstract class Strategy implements ExtensionPoint {

  private File jenkinsHome;

  public abstract Collection<File> backup();

  public abstract void restore(Collection<File> toRestore) throws RestoreException;

  public static ExtensionList<Strategy> all(Jenkins jenkins) {
    return jenkins != null ? jenkins.getExtensionList(Strategy.class) : new ExtensionList<Strategy>(jenkins, Strategy.class) {};
  }

  public File getJenkinsHome() {
    return jenkinsHome;
  }

  public void setJenkinsHome(File jenkinsHome) {
    this.jenkinsHome = jenkinsHome;
  }
}
