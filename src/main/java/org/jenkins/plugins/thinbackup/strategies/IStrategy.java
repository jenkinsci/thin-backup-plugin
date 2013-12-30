package org.jenkins.plugins.thinbackup.strategies;

import java.io.File;
import java.util.List;

public interface IStrategy {
  
  List<File> backup();
  
  void restore(List<File> toRestore);
}
