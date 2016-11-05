/**
 *  Copyright (C) 2011  Matthias Steinkogler, Thomas Fürer
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses.
 */
package org.jvnet.hudson.plugins.thinbackup.backup;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.DirectoryWalker;

public class DirectoryCleaner extends DirectoryWalker<Object> {

  /**
   * Deletes all empty directories, including rootDir if it is empty at the end.
   * 
   * @param rootDir  the directory to start from, not null
   * @throws IOException if an I/O Error occurs
   */
  public void removeEmptyDirectories(final File rootDir) throws IOException {
    walk(rootDir, Collections.emptyList());
  }

  @Override
  protected void handleDirectoryEnd(final File directory, final int depth,
      @SuppressWarnings("rawtypes") final Collection results) {
    if (directory.list().length == 0) {
      directory.delete();
    }
  }

}
