package org.jvnet.hudson.plugins.thinbackup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.jvnet.hudson.plugins.thinbackup.backup.HudsonBackup;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.mortbay.io.ByteArrayBuffer;

public class HudsonDirectoryStructureSetup {

  public static final String CONFIG_XML_CONTENTS = "FILLED WITH DATA... ";
  public static final String TEST_JOB_NAME = "test";
  public static final String BACKUP_DIRECTORY_NAME = "2011-01-08_22-26-40";

  protected File root;
  protected File backupDir;

  protected List<String> originalFiles;

  public HudsonDirectoryStructureSetup() {
    super();
  }

  @Before
  public void setup() throws Exception {
    final File tempDir = new File(System.getProperty("java.io.tmpdir"));
    root = new File(tempDir, "RootDirForHudsonBackupTest");
    root.mkdir();
    backupDir = new File(tempDir, "BackupDirForHudsonBackupTest");
    backupDir.mkdir();

    new File(root, "config.xml").createNewFile();
    new File(root, "thinBackup.xml").createNewFile();
    new File(root, "secret.key").createNewFile();
    new File(root, "nodeMonitors.xml").createNewFile();
    new File(root, "hudson.model.UpdateCenter.xml").createNewFile();

    final File jobsDir = new File(root, HudsonBackup.JOBS_DIR_NAME);
    jobsDir.mkdir();
    final File testJob = new File(jobsDir, TEST_JOB_NAME);
    testJob.mkdir();
    final File config = new File(testJob, "config.xml");
    config.createNewFile();
    final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(config));
    out.write(new ByteArrayBuffer(CONFIG_XML_CONTENTS).array());
    out.close();
    new File(testJob, HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME).createNewFile();
    new File(testJob, "workspace").mkdir();
    new File(testJob, "modules").mkdir();
    final File builds = new File(testJob, HudsonBackup.BUILDS_DIR_NAME);
    builds.mkdir();
    final File build = new File(builds, BACKUP_DIRECTORY_NAME);
    build.mkdir();

    final File archiveDir = new File(build, HudsonBackup.ARCHIVE_DIR_NAME);
    archiveDir.mkdir();
    new File(build, "build.xml").createNewFile();
    new File(build, "changelog.xml").createNewFile();
    new File(build, "log").createNewFile();
    new File(build, "revision.txt").createNewFile();
    new File(build, "logfile.log").createNewFile();
    new File(build, "logfile.xlog").createNewFile();

    new File(archiveDir, "someFile.log").createNewFile();

    final FileCollector fc = new FileCollector();
    originalFiles = fc.getFilesAsString(root);
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(root);
    FileUtils.deleteDirectory(backupDir);
    FileUtils.deleteDirectory(new File(Utils.THINBACKUP_TMP_DIR));
  }

  protected boolean containsStringEndingWith(final List<String> strings, final String pattern) {
    boolean contains = false;

    for (final String string : strings) {
      if (string.endsWith(pattern)) {
        contains = true;
        break;
      }
    }

    return contains;
  }

}