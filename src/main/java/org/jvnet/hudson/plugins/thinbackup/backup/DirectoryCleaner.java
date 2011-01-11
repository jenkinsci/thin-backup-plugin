package org.jvnet.hudson.plugins.thinbackup.backup;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.DirectoryWalker;

public class DirectoryCleaner extends DirectoryWalker {
  public void clean(final File rootDir) throws IOException {
    walk(rootDir, null);
  }

  @Override
  protected void handleDirectoryEnd(final File directory, final int depth,
      @SuppressWarnings("rawtypes") final Collection results) throws IOException {
    if (directory.list().length == 0) {
      directory.delete();
    }
  }
}
