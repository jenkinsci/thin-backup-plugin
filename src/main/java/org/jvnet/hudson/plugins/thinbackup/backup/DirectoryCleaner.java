package org.jvnet.hudson.plugins.thinbackup.backup;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.DirectoryWalker;

public class DirectoryCleaner extends DirectoryWalker {

  /**
   * Deletes all empty directories, including rootDir if it is empty at the end.
   * 
   * @param rootDir
   * @throws IOException
   */
  public void removeEmptyDirectories(final File rootDir) throws IOException {
    walk(rootDir, null);
  }

  @SuppressWarnings("unused")
  @Override
  protected void handleDirectoryEnd(final File directory, final int depth,
      @SuppressWarnings("rawtypes") final Collection results) throws IOException {
    if (directory.list().length == 0) {
      directory.delete();
    }
  }
}
