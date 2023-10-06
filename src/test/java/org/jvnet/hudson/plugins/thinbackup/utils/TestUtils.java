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
package org.jvnet.hudson.plugins.thinbackup.utils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.backup.BackupDirStructureSetup;
import org.jvnet.hudson.plugins.thinbackup.backup.BackupSet;

import static org.junit.jupiter.api.Assertions.*;

public class TestUtils extends BackupDirStructureSetup {

  private File tmpDir;

  @BeforeEach
  public void init() {
    tmpDir = new File(System.getenv("tmp"), "test");
    tmpDir.mkdir();
  }
  
  @AfterEach
  public void cleanup() throws IOException {
    FileUtils.deleteDirectory(tmpDir);
  }
  
  @Test
  public void testConvertToDirectoryNameDateFormat() throws ParseException {
    final String displayDate = "2011-02-13 10:48";
    final String fileDate = Utils.convertToDirectoryNameDateFormat(displayDate);
    assertEquals("2011-02-13_10-48", fileDate);
  }

  @Test
  public void testBadFormatConvertToDirectoryNameDateFormat() {
    assertThrows(ParseException.class,
            () -> Utils.convertToDirectoryNameDateFormat("2011-02-13-10:48")
    );
  }

  @Test
  public void testWrongFormatConvertToDirectoryNameDateFormat() {
    assertThrows(ParseException.class,
            () -> Utils.convertToDirectoryNameDateFormat("FULL-2011-02-13_10-48")
    );
  }

  @Test
  public void testEmptyDateConvertToDirectoryNameDateFormat() {
    assertThrows(ParseException.class,
            () -> Utils.convertToDirectoryNameDateFormat("")
    );
  }

  @Test
  public void testGetDateFromValidBackupDir() {
    final Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.set(2011, Calendar.FEBRUARY, 13, 10, 48);
    final Date expected = cal.getTime();

    String displayDate = "FULL-2011-02-13_10-48";
    Date tmp = Utils.getDateFromBackupDirectoryName(displayDate);
    assertNotNull(tmp);
    assertEquals(expected, tmp);

    displayDate = "DIFF-2011-02-13_10-48";
    tmp = Utils.getDateFromBackupDirectoryName(displayDate);
    assertNotNull(tmp);
    assertEquals(expected, tmp);
  }

  @Test
  public void testGetDateFromInvalidBackupDir() {
    final String displayDate = "DWDWD-2011-02-13_10-48";
    final Date tmp = Utils.getDateFromBackupDirectoryName(displayDate);
    assertNull(tmp);
  }

  @Test
  public void testGetBackupTypeDirectories() {
    final List<File> fullBackupDirs = Utils.getBackupTypeDirectories(backupDir, BackupType.FULL);
    assertEquals(3, fullBackupDirs.size());

    final List<File> diffBackupDirs = Utils.getBackupTypeDirectories(backupDir, BackupType.DIFF);
    assertEquals(7, diffBackupDirs.size());
  }

  @Test
  public void testGetReferencedFullBackup() {
    File fullBackup = Utils.getReferencedFullBackup(diff11);
    assertEquals(full1, fullBackup);

    fullBackup = Utils.getReferencedFullBackup(diff12);
    assertEquals(full1, fullBackup);

    fullBackup = Utils.getReferencedFullBackup(diff13);
    assertEquals(full1, fullBackup);

    fullBackup = Utils.getReferencedFullBackup(diff14);
    assertEquals(full1, fullBackup);

    fullBackup = Utils.getReferencedFullBackup(full1);
    assertEquals(full1, fullBackup);

    fullBackup = Utils.getReferencedFullBackup(diff41);
    assertNull(fullBackup);
  }

  @Test
  public void testGetReferencingDiffBackups() {
    List<File> diffBackups = Utils.getReferencingDiffBackups(full1);
    assertEquals(4, diffBackups.size());
    assertTrue(diffBackups.contains(diff11));
    assertTrue(diffBackups.contains(diff12));
    assertTrue(diffBackups.contains(diff13));
    assertTrue(diffBackups.contains(diff14));

    diffBackups = Utils.getReferencingDiffBackups(diff41);
    assertEquals(0, diffBackups.size());
  }

  @Test
  public void testGetBackups() {
    final List<String> backups = Utils.getBackupsAsDates(backupDir.getAbsoluteFile());
    assertEquals(9, backups.size());
  }

  @Test
  public void testGetValidBackupSets() {
    final List<BackupSet> validBackupSets = Utils.getValidBackupSetsFromDirectories(backupDir);
    assertEquals(3, validBackupSets.size());
  }

  @Test
  public void testExpandEnvironmentVariables() {
    final Map<String, String> map = new HashMap<>();
    map.put("TEST_VAR", "REPLACEMENT");
    String path = "${TEST_VAR}";
    assertEquals("REPLACEMENT", Utils.internalExpandEnvironmentVariables(path, map));
    path = "1${TEST_VAR}2";
    assertEquals("1REPLACEMENT2", Utils.internalExpandEnvironmentVariables(path, map));
    path = "1${TEST_VAR2";
    assertEquals("1${TEST_VAR2", Utils.internalExpandEnvironmentVariables(path, map));
    path = "1${TEST_VAR}2${3";
    assertEquals("1REPLACEMENT2${3", Utils.internalExpandEnvironmentVariables(path, map));
    path = "1${TEST_VAR}2${TEST_VAR}3";
    assertEquals("1REPLACEMENT2REPLACEMENT3", Utils.internalExpandEnvironmentVariables(path, map));
    path = "1${NO_VALUE_DEFINED}";
    try {
      Utils.internalExpandEnvironmentVariables(path, map);
      fail("Expected an exception.");
    } catch (final EnvironmentVariableNotDefinedException evnde) {
      // if an exception is caught, everything is AOK.
    }
  }
}
