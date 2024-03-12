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

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class TestBackupSet extends BackupDirStructureSetup {

    @Test
    public void testSimpleBackupSet() {
        final BackupSet setFromFull = new BackupSet(full2);
        assertTrue(setFromFull.isValid());
        assertEquals(1, setFromFull.getDiffBackups().size());
        assertEquals(full2, setFromFull.getFullBackup());
        assertTrue(setFromFull.getDiffBackups().contains(diff21));

        final BackupSet setFromDiff = new BackupSet(diff21);
        assertTrue(setFromDiff.isValid());
        assertEquals(1, setFromDiff.getDiffBackups().size());
        assertEquals(full2, setFromDiff.getFullBackup());
        assertTrue(setFromDiff.getDiffBackups().contains(diff21));
    }

    @Test
    public void testDelete() throws Exception {
        final BackupSet setFromFull = new BackupSet(full1);
        assertTrue(setFromFull.isValid());
        assertEquals(4, setFromFull.getDiffBackups().size());
        assertEquals(full1, setFromFull.getFullBackup());
        assertTrue(setFromFull.getDiffBackups().contains(diff11));
        assertTrue(setFromFull.getDiffBackups().contains(diff12));
        assertTrue(setFromFull.getDiffBackups().contains(diff13));
        assertTrue(setFromFull.getDiffBackups().contains(diff14));

        assertEquals(10, backupDir.list().length);
        setFromFull.delete();
        assertEquals(5, backupDir.list().length);
    }

    @Test
    public void testInvalidSet() throws Exception {
        final BackupSet setFromDiff = new BackupSet(diff41);
        assertFalse(setFromDiff.isValid());
        assertFalse(setFromDiff.isValid());
        assertNull(setFromDiff.getFullBackup());
        assertNull(setFromDiff.getDiffBackups());

        assertEquals(10, backupDir.list().length);
        setFromDiff.delete();
        assertEquals(10, backupDir.list().length);
    }

    @Test
    public void testBackupSetCompare() {
        final BackupSet backupSet1 = new BackupSet(full1);
        final BackupSet backupSet2 = new BackupSet(full2);
        final BackupSet invalidBackupSet = new BackupSet(diff41);

        assertEquals(0, backupSet1.compareTo(backupSet1));
        assertEquals(-1, backupSet1.compareTo(backupSet2));
        assertEquals(1, backupSet2.compareTo(backupSet1));
        assertEquals(1, backupSet1.compareTo(invalidBackupSet));
        assertEquals(1, backupSet2.compareTo(invalidBackupSet));
        assertEquals(-1, invalidBackupSet.compareTo(backupSet1));
    }

    @Test
    public void testBackupSetContainsDirectory() throws IOException {
        final BackupSet backupSet1 = new BackupSet(full1);

        assertTrue(backupSet1.containsDirectory(full1));
        assertTrue(backupSet1.containsDirectory(new File(full1.getAbsolutePath())));
        assertTrue(backupSet1.containsDirectory(diff11));
        assertTrue(backupSet1.containsDirectory(diff12));
        assertTrue(backupSet1.containsDirectory(diff13));
        assertTrue(backupSet1.containsDirectory(diff14));
        assertFalse(backupSet1.containsDirectory(null));
        assertFalse(backupSet1.containsDirectory(full2));
        assertFalse(backupSet1.containsDirectory(diff21));
        assertFalse(backupSet1.containsDirectory(new File(diff21.getAbsolutePath())));

        final BackupSet invalidBackupSet = new BackupSet(diff41);
        assertFalse(invalidBackupSet.containsDirectory(diff41));

        final File tempDir = new File(System.getProperty("java.io.tmpdir"));
        backupDir = new File(tempDir, "BackupDirForHudsonBackupTest");
        final File testFile = tmpFolder.resolve("tempFile.nxt").toFile();
        testFile.createNewFile();
        assertFalse(backupSet1.containsDirectory(testFile));
        assertFalse(backupSet1.containsDirectory(new File(testFile.getAbsolutePath())));
    }
}
