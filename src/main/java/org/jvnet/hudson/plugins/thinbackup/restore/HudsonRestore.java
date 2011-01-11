package org.jvnet.hudson.plugins.thinbackup.restore;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;

public class HudsonRestore {
  private final String backupPath;
  private final String restoreBackupFrom;
  private final File hudsonHome;

  public HudsonRestore(final File hudsonConfigurationPath, final String backupPath, final String restoreBackupFrom) {
    this.hudsonHome = hudsonConfigurationPath;
    this.backupPath = backupPath;
    this.restoreBackupFrom = restoreBackupFrom;
  }

  public void restore() throws IOException {
    IOFileFilter suffixFilter = FileFilterUtils.suffixFileFilter(restoreBackupFrom);
    suffixFilter = FileFilterUtils.andFileFilter(suffixFilter, DirectoryFileFilter.DIRECTORY);

    if (!StringUtils.isEmpty(backupPath)) {
      final File[] candidates = new File(backupPath).listFiles((FileFilter) suffixFilter);
      if (candidates.length == 1) {
        final File toRestore = candidates[0];
        if (toRestore.getName().startsWith(BackupType.DIFF.toString())) {
          restore(getReferencedFullBackup(toRestore));
        }
        restore(toRestore);
      }
    }
  }

  private void restore(final File toRestore) throws IOException {
    FileUtils.copyDirectory(toRestore, this.hudsonHome);
  }

  private File getReferencedFullBackup(final File toRestore) {
    final IOFileFilter prefixFilter = FileFilterUtils.prefixFileFilter(BackupType.FULL.toString());
    final Collection<File> backups = Arrays.asList(new File(backupPath).listFiles((FilenameFilter) prefixFilter));

    if (backups.isEmpty()) {
      return null;
    }

    File referencedFullBackup = null;

    final Date curModifiedDate = new Date(toRestore.lastModified());
    Date closestPreviousBackupDate = new Date(0);
    for (final File fullBackupDir : backups) {
      final Date tmpModifiedDate = new Date(fullBackupDir.lastModified());
      if (tmpModifiedDate.before(curModifiedDate) && tmpModifiedDate.after(closestPreviousBackupDate)) {
        closestPreviousBackupDate = tmpModifiedDate;
        referencedFullBackup = fullBackupDir;
      }
    }

    return referencedFullBackup;
  }

  public void prepare() throws IOException {
    FileUtils.cleanDirectory(new File(hudsonHome, "jobs"));
    final IOFileFilter filter = FileFilterUtils.andFileFilter(FileFileFilter.FILE,
        FileFilterUtils.suffixFileFilter("xml"));
    @SuppressWarnings("unchecked")
    final Collection<File> listFiles = FileUtils.listFiles(hudsonHome, filter, null);
    for (final File file : listFiles) {
      file.delete();
    }
  }
}
