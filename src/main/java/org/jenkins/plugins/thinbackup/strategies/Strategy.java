package org.jenkins.plugins.thinbackup.strategies;

import hudson.ExtensionList;
import hudson.ExtensionPoint;

import java.io.File;
import java.util.Collection;

import jenkins.model.Jenkins;

import org.jenkins.plugins.thinbackup.exceptions.RestoreException;

public abstract class Strategy implements ExtensionPoint {

  protected final File jenkinsHome;

  public Strategy(File jenkinsHome) {
    this.jenkinsHome = jenkinsHome;
  }

  public abstract Collection<File> backup();

  public abstract void restore(Collection<File> toRestore) throws RestoreException;

  public File getJenkinsHome() {
    return jenkinsHome;
  }

  public static ExtensionList<Strategy> all(Jenkins jenkins) {
    return jenkins != null ? jenkins.getExtensionList(Strategy.class) : new ExtensionList<Strategy>(jenkins, Strategy.class) {};
  }
}
