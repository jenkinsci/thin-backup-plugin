package org.jvnet.hudson.plugins.thinbackup.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.DirectoryWalker;

public class DirectoriesZipper extends DirectoryWalker implements Closeable {
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

  public static final int BUFFER_SIZE = 512 * 1024;

  private final ZipOutputStream zipStream;
  private final String rootPath;

  public DirectoriesZipper(final File zipFile) throws IOException {
    zipFile.createNewFile();
    zipStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
    this.rootPath = zipFile.getParent();
  }

  public void addToZip(final File directory) throws IOException {
    walk(directory, null);
  }

  @Override
  public void close() throws IOException {
    zipStream.close();
  }

  @Override
  protected void handleFile(final File file, final int depth, @SuppressWarnings("rawtypes") final Collection results) {
    try {
      final FileInputStream fi = new FileInputStream(file);
      final BufferedInputStream origin = new BufferedInputStream(fi);

      // make entry relative to the root directory
      String entryPath = file.getAbsolutePath();
      entryPath = entryPath.replace(rootPath + File.separator, "");
      final ZipEntry entry = new ZipEntry(entryPath);

      zipStream.putNextEntry(entry);
      int count;
      final byte[] buffer = new byte[BUFFER_SIZE];
      while ((count = origin.read(buffer)) != -1) {
        zipStream.write(buffer, 0, count);
      }
      origin.close();
    } catch (final IOException ioe) {
      LOGGER.log(Level.SEVERE, "Could not create ZIP entry", ioe);
    }
  }

}
