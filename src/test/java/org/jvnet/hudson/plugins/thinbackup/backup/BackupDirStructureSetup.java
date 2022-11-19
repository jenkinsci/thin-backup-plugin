package org.jvnet.hudson.plugins.thinbackup.backup;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;

import java.io.File;
import java.util.Calendar;

import static org.jvnet.hudson.plugins.thinbackup.utils.Utils.getFormattedDirectory;

public class BackupDirStructureSetup {

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder(new File(System.getProperty("java.io.tmpdir")));

  protected File backupDir;

  protected File full1;
  protected File diff11;
  protected File diff12;
  protected File diff13;
  protected File diff14;

  protected File full2;
  protected File diff21;

  protected File full3;
  protected File diff31;

  protected File diff41;

  @Before
  public void setup() throws Exception {
    backupDir = tmpFolder.newFolder("thin-backup");

    final Calendar cal = Calendar.getInstance();
    cal.set(2011, Calendar.JANUARY, 1, 0, 0);
    full1 = getFormattedDirectory(backupDir, BackupType.FULL, cal.getTime());
    full1.mkdir();
    full1.setLastModified(cal.getTimeInMillis());
    cal.set(2011, Calendar.JANUARY, 1, 0, 1);
    diff11 = getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime());
    diff11.mkdir();
    diff11.setLastModified(cal.getTimeInMillis());
    cal.set(2011, Calendar.JANUARY, 1, 0, 2);
    diff12 = getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime());
    diff12.mkdir();
    diff12.setLastModified(cal.getTimeInMillis());
    cal.set(2011, Calendar.JANUARY, 1, 0, 3);
    diff13 = getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime());
    diff13.mkdir();
    diff13.setLastModified(cal.getTimeInMillis());
    cal.set(2011, Calendar.JANUARY, 1, 0, 4);
    diff14 = getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime());
    diff14.mkdir();
    diff14.setLastModified(cal.getTimeInMillis());

    cal.set(2011, Calendar.FEBRUARY, 1, 0, 0);
    full2 = getFormattedDirectory(backupDir, BackupType.FULL, cal.getTime());
    full2.mkdir();
    full2.setLastModified(cal.getTimeInMillis());
    cal.set(2011, Calendar.FEBRUARY, 1, 0, 1);
    diff21 = getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime());
    diff21.mkdir();
    diff21.setLastModified(cal.getTimeInMillis());

    cal.set(2011, Calendar.MARCH, 1, 0, 0);
    full3 = getFormattedDirectory(backupDir, BackupType.FULL, cal.getTime());
    full3.mkdir();
    full3.setLastModified(cal.getTimeInMillis());
    cal.set(2011, Calendar.MARCH, 1, 0, 1);
    diff31 = getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime());
    diff31.mkdir();
    diff31.setLastModified(cal.getTimeInMillis());

    cal.set(2010, Calendar.APRIL, 1, 0, 1);
    diff41 = getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime());
    diff41.mkdir();
    diff41.setLastModified(cal.getTimeInMillis());
  }
}
