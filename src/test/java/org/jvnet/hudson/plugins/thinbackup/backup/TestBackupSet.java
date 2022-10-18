/*
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

import org.junit.Assert;
import org.junit.Test;

public class TestBackupSet extends BackupDirStructureSetup {

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
    final BackupSet setFromDiff = new BackupSet(diff41);
    Assert.assertFalse(setFromDiff.isValid());
    Assert.assertFalse(setFromDiff.isValid());
    Assert.assertNull(setFromDiff.getFullBackup());
    Assert.assertNull(setFromDiff.getDiffBackups());

    Assert.assertEquals(10, backupDir.list().length);
    setFromDiff.delete();
    Assert.assertEquals(10, backupDir.list().length);
  }

  @Test
  public void testBackupSetCompare() {
    final BackupSet backupSet1 = new BackupSet(full1);
    final BackupSet backupSet2 = new BackupSet(full2);
    final BackupSet invalidBackupSet = new BackupSet(diff41);

    Assert.assertEquals(0, backupSet1.compareTo(backupSet1));
    Assert.assertEquals(-1, backupSet1.compareTo(backupSet2));
    Assert.assertEquals(1, backupSet2.compareTo(backupSet1));
    Assert.assertEquals(1, backupSet1.compareTo(invalidBackupSet));
    Assert.assertEquals(1, backupSet2.compareTo(invalidBackupSet));
    Assert.assertEquals(-1, invalidBackupSet.compareTo(backupSet1));
  }

  @Test
  public void testBackupSetContainsDirectory() throws IOException {
    final BackupSet backupSet1 = new BackupSet(full1);

    Assert.assertTrue(backupSet1.containsDirectory(full1));
    Assert.assertTrue(backupSet1.containsDirectory(new File(full1.getAbsolutePath())));
    Assert.assertTrue(backupSet1.containsDirectory(diff11));
    Assert.assertTrue(backupSet1.containsDirectory(diff12));
    Assert.assertTrue(backupSet1.containsDirectory(diff13));
    Assert.assertTrue(backupSet1.containsDirectory(diff14));
    Assert.assertFalse(backupSet1.containsDirectory(null));
    Assert.assertFalse(backupSet1.containsDirectory(full2));
    Assert.assertFalse(backupSet1.containsDirectory(diff21));
    Assert.assertFalse(backupSet1.containsDirectory(new File(diff21.getAbsolutePath())));

    final BackupSet invalidBackupSet = new BackupSet(diff41);
    Assert.assertFalse(invalidBackupSet.containsDirectory(diff41));

    final File tempDir = new File(System.getProperty("java.io.tmpdir"));
    backupDir = new File(tempDir, "BackupDirForHudsonBackupTest");
    final File testFile = tmpFolder.newFile("tempFile.nxt");
    testFile.createNewFile();
    Assert.assertFalse(backupSet1.containsDirectory(testFile));
    Assert.assertFalse(backupSet1.containsDirectory(new File(testFile.getAbsolutePath())));
  }

}
