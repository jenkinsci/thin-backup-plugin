package org.jvnet.hudson.plugins.thinbackup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.DirectoryWalker;

public class FileCollector extends DirectoryWalker<String> {

	public FileCollector() {
		super(pathname -> {
            String name = pathname.getName();
            return !(name.equals("lastSuccessful") || name.equals("lastStable"));
        }, -1);
	}

	/**
   * Recursively gets all files from the given directory.
   * 
   * @param rootDir
   * @return a list of all files found recursively
   * @throws IOException
   */
  public List<String> getFilesAsString(final File rootDir) throws IOException {
    final ArrayList<String> result = new ArrayList<>();
    walk(rootDir, result);
    return result;
  }

  @Override
  @SuppressWarnings({"unchecked" })
  protected void handleFile(final File file, final int depth, final Collection results) {
    results.add(file.getAbsolutePath());
  }

}
