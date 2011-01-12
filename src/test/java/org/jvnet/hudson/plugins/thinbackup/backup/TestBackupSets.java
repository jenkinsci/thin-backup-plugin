package org.jvnet.hudson.plugins.thinbackup.backup;

import static org.jvnet.hudson.plugins.thinbackup.utils.Utils.getFormattedDirectory;

import java.io.File;
import java.util.Calendar;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;

public class TestBackupSets {

  private File backupDir;

  private File full1;
  private File diff11;
  private File diff12;
  private File diff13;
  private File diff14;

  private File full2;
  private File diff21;

  private File full3;
  private File diff31;

  private File diff41;

  @Before
  public void setUp() throws Exception {
    final File tempDir = new File(System.getProperty("java.io.tmpdir"));
    backupDir = new File(tempDir, "BackupDirForHudsonBackupTest");
    backupDir.mkdir();

    final Calendar cal = Calendar.getInstance();
    cal.set(2011, 0, 1, 0, 0);
    full1 = getFormattedDirectory(backupDir, BackupType.FULL, cal.getTime());
    full1.mkdir();
    full1.setLastModified(cal.getTimeInMillis());
    cal.set(2011, 0, 1, 0, 1);
    diff11 = getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime());
    diff11.mkdir();
    diff11.setLastModified(cal.getTimeInMillis());
    cal.set(2011, 0, 1, 0, 2);
    diff12 = getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime());
    diff12.mkdir();
    diff12.setLastModified(cal.getTimeInMillis());
    cal.set(2011, 0, 1, 0, 3);
    diff13 = getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime());
    diff13.mkdir();
    diff13.setLastModified(cal.getTimeInMillis());
    cal.set(2011, 0, 1, 0, 4);
    diff14 = getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime());
    diff14.mkdir();
    diff14.setLastModified(cal.getTimeInMillis());

    cal.set(2011, 1, 1, 0, 0);
    full2 = getFormattedDirectory(backupDir, BackupType.FULL, cal.getTime());
    full2.mkdir();
    full2.setLastModified(cal.getTimeInMillis());
    cal.set(2011, 1, 1, 0, 1);
    diff21 = getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime());
    diff21.mkdir();
    diff21.setLastModified(cal.getTimeInMillis());

    cal.set(2011, 2, 1, 0, 0);
    full3 = getFormattedDirectory(backupDir, BackupType.FULL, cal.getTime());
    full3.mkdir();
    full3.setLastModified(cal.getTimeInMillis());
    cal.set(2011, 2, 1, 0, 1);
    diff31 = getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime());
    diff31.mkdir();
    diff31.setLastModified(cal.getTimeInMillis());

    cal.set(2010, 3, 1, 0, 1);
    diff41 = getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime());
    diff41.mkdir();
    diff41.setLastModified(cal.getTimeInMillis());
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(backupDir);
  }

  @Test
  public void testSimpleBackupSet() throws Exception {
    final BackupSet setFromFull = new BackupSet(full2);
    Assert.assertTrue(setFromFull.isValid());
    Assert.assertEquals(1, setFromFull.getDiffBackups().size());
    Assert.assertEquals(full2, setFromFull.getFullBackup());
    Assert.assertTrue(setFromFull.getDiffBackups().contains(diff21));

    final BackupSet setFromDiff = new BackupSet(diff21);
    Assert.assertTrue(setFromDiff.isValid());
    Assert.assertEquals(1, setFromDiff.getDiffBackups().size());
    Assert.assertEquals(full2, setFromDiff.getFullBackup());
    Assert.assertTrue(setFromDiff.getDiffBackups().contains(diff21));
  }

  @Test
  public void testDelete() throws Exception {
    final BackupSet setFromFull = new BackupSet(full1);
    Assert.assertTrue(setFromFull.isValid());
    Assert.assertEquals(4, setFromFull.getDiffBackups().size());
    Assert.assertEquals(full1, setFromFull.getFullBackup());
    Assert.assertTrue(setFromFull.getDiffBackups().contains(diff11));
    Assert.assertTrue(setFromFull.getDiffBackups().contains(diff12));
    Assert.assertTrue(setFromFull.getDiffBackups().contains(diff13));
    Assert.assertTrue(setFromFull.getDiffBackups().contains(diff14));

    Assert.assertEquals(10, backupDir.list().length);
    setFromFull.delete();
    Assert.assertEquals(5, backupDir.list().length);
  }

  @Test
  public void testInvalidSet() throws Exception {
    final BackupSet setFromFull = new BackupSet(diff41);
    Assert.assertFalse(setFromFull.isValid());
    Assert.assertNull(setFromFull.getFullBackup());
    Assert.assertEquals(1, setFromFull.getDiffBackups().size());

    Assert.assertEquals(10, backupDir.list().length);
    setFromFull.delete();
    Assert.assertEquals(9, backupDir.list().length);
  }

}
