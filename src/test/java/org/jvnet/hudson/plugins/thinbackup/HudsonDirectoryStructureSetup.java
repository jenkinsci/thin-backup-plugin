package org.jvnet.hudson.plugins.thinbackup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.mortbay.io.ByteArrayBuffer;

public class HudsonDirectoryStructureSetup {

  public static final String CONFIG_XML_CONTENTS = "FILLED WITH DATA... ";
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
    new File(root, "nodeMontitors.xml").createNewFile();
    new File(root, "hudson.model.UpdateCenter.xml").createNewFile();

    final File jobsDir = new File(root, "jobs");
    jobsDir.mkdir();
    final File testJob = new File(jobsDir, "test");
    testJob.mkdir();
    final File config = new File(testJob, "config.xml");
    config.createNewFile();
    final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(config));
    out.write(new ByteArrayBuffer(CONFIG_XML_CONTENTS).array());
    out.close();
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
    new File(build, "logfile.log").createNewFile();
    new File(build, "logfile.xlog").createNewFile();

    final FileCollector fc = new FileCollector();
    originalFiles = fc.getFilesAsString(root);
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(root);
    FileUtils.deleteDirectory(backupDir);
    FileUtils.deleteDirectory(new File(Utils.THINBACKUP_TMP_DIR));
  }

}