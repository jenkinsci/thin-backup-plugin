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
import java.io.FileFilter;
import java.util.Calendar;

import junit.framework.Assert;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.Test;
import org.jvnet.hudson.plugins.thinbackup.HudsonDirectoryStructureSetup;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;

public class TestHudsonBackup extends HudsonDirectoryStructureSetup {

  @Test
  public void testBackup() throws Exception {
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE - 10));

    new HudsonBackup(backupDir, root, BackupType.FULL, -1, false, false, true, cal.getTime()).backup();

    String[] list = backupDir.list();
    Assert.assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    Assert.assertEquals(6, list.length);

    final File job = new File(new File(backup, "jobs"), "test");
    list = job.list();
    Assert.assertEquals(2, list.length);

    final File build = new File(new File(job, "builds"), "2011-01-08_22-26-40");
    list = build.list();
    Assert.assertEquals(4, list.length);
  }

  @Test
  public void testBackupWithoutBuildResults() throws Exception {
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE - 10));

    new HudsonBackup(backupDir, root, BackupType.FULL, -1, false, false, false, cal.getTime()).backup();

    String[] list = backupDir.list();
    Assert.assertEquals(1, list.length);
    final File backup = new File(backupDir, list[0]);
    list = backup.list();
    Assert.assertEquals(6, list.length);

    final File job = new File(new File(backup, "jobs"), "test");
    list = job.list();
    Assert.assertEquals(1, list.length);
    Assert.assertEquals("config.xml", list[0]);
  }

  @Test
  public void testHudsonDiffBackup() throws Exception {
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE - 10));

    new HudsonBackup(backupDir, root, BackupType.FULL, -1, false, false, true, cal.getTime()).backup();

    // fake modification
    backupDir.listFiles((FileFilter) FileFilterUtils.prefixFileFilter(BackupType.FULL.toString()))[0]
        .setLastModified(System.currentTimeMillis() - 60000 * 60);

    for (final File globalConfigFile : root.listFiles()) {
      globalConfigFile.setLastModified(System.currentTimeMillis() - 60000 * 120);
    }

    new HudsonBackup(backupDir, root, BackupType.DIFF, -1, false, false, true).backup();
    final File lastDiffBackup = backupDir.listFiles((FileFilter) FileFilterUtils.prefixFileFilter(BackupType.DIFF
        .toString()))[0];
    Assert.assertEquals(1, lastDiffBackup.list().length);
  }

}
