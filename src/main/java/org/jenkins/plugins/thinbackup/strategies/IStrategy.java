package org.jenkins.plugins.thinbackup.strategies;

import java.io.File;
import java.util.Collection;
import java.util.List;

public interface IStrategy {
  
  Collection<File> backup();
  
  void restore(List<File> toRestore);
}
