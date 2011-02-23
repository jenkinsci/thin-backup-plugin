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

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;

public class TestHudsonBackup {

  private File root;
  private File backupDir;

  @Before
  public void setUp() throws Exception {
    final File tempDir = new File(System.getProperty("java.io.tmpdir"));
    root = new File(tempDir, "RootDirForHudsonBackupTest");
    root.mkdir();
    backupDir = new File(tempDir, "BackupDirForHudsonBackupTest");
    backupDir.mkdir();

    new File(root, "config.xml").createNewFile();
    new File(root, "thinBackup.xml").createNewFile();
    new File(root, "secret.key").createNewFile();
    new File(root, "nodeMontitors.xml").createNewFile();
    new File(root, "hudson.model.UpdateCenter.xml").createNewFile();

    final File jobsDir = new File(root, "jobs");
    jobsDir.mkdir();
    final File testJob = new File(jobsDir, "test");
    testJob.mkdir();
    new File(testJob, "config.xml").createNewFile();
    new File(testJob, "nextBuildNumber").createNewFile();
    new File(testJob, "workspace").mkdir();
    new File(testJob, "modules").mkdir();
    final File builds = new File(testJob, "builds");
    builds.mkdir();
    final File build = new File(builds, "2011-01-08_22-26-40");
    build.mkdir();

    new File(build, "archive").mkdir();
    new File(build, "build.xml").createNewFile();
    new File(build, "changelog.xml").createNewFile();
    new File(build, "log").createNewFile();
    new File(build, "revision.txt").createNewFile();
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(root);
    FileUtils.deleteDirectory(backupDir);
  }

  @Test
  public void testBackup() throws Exception {
    new HudsonBackup(backupDir, root, BackupType.FULL, -1, false).backup();

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
  public void testHudsonDiffBackup() throws Exception {
    new HudsonBackup(backupDir, root, BackupType.FULL, -1, false).backup();

    // fake modification
    backupDir.listFiles((FileFilter) FileFilterUtils.prefixFileFilter(BackupType.FULL.toString()))[0]
        .setLastModified(System.currentTimeMillis() - 60000 * 60);

    for (final File globalConfigFile : root.listFiles()) {
      globalConfigFile.setLastModified(System.currentTimeMillis() - 60000 * 120);
    }

    new HudsonBackup(backupDir, root, BackupType.DIFF, -1, false).backup();
    final File lastDiffBackup = backupDir.listFiles((FileFilter) FileFilterUtils.prefixFileFilter(BackupType.DIFF
        .toString()))[0];
    Assert.assertEquals(1, lastDiffBackup.list().length);
  }

}
