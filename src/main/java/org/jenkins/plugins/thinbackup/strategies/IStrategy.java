package org.jenkins.plugins.thinbackup.strategies;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.jenkins.plugins.thinbackup.exceptions.RestoreException;

public interface IStrategy {
  
  Collection<File> backup();
  
  void restore(List<File> toRestore) throws RestoreException;
}
