package org.jvnet.hudson.plugins.thinbackup;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.Util;
import hudson.util.StreamTaskListener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.jvnet.hudson.plugins.thinbackup.backup.HudsonBackup;

public class TestHelper {
  public static final String CONFIG_XML_CONTENTS = "FILLED WITH DATA... ";
  public static final String CONCRETE_BUILD_DIRECTORY_NAME = "2011-01-08_22-26-40";
  public static final String TEST_JOB_NAME = "test";
  public static final String TEST_NODE_NAME = "node1";

  public static File createBasicFolderStructure(File base) throws IOException {
    File root = new File(base, "RootDirForHudsonBackupTest");
    root.mkdir();

    new File(root, "config.xml").createNewFile();
    new File(root, "thinBackup.xml").createNewFile();
    new File(root, "secret.key").createNewFile();
    new File(root, "nodeMonitors.xml").createNewFile();
    new File(root, "hudson.model.UpdateCenter.xml").createNewFile();
    new File(root, HudsonBackup.JOBS_DIR_NAME).mkdir();
    new File(root, HudsonBackup.USERS_DIR_NAME).mkdir();
    new File(root, HudsonBackup.NODES_DIR_NAME).mkdir();
    new File(root, HudsonBackup.USERSCONTENTS_DIR_NAME).mkdir();
    new File(root, "plugins").mkdir();
    
    return root;
  }
  
  public static File createBackupFolder(File base) {
    File backupDir = new File(base, "BackupDirForHudsonBackupTest");
    backupDir.mkdir();
    
    return backupDir;
  }
  
  public static File createCloudBeesFolder(File jenkinsHome, String folderName) throws FileNotFoundException, IOException {
    final File folderDir = new File(new File(jenkinsHome, HudsonBackup.JOBS_DIR_NAME), folderName);
    folderDir.mkdirs();
    
    final File config = new File(folderDir, "config.xml");
    config.createNewFile();
    final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(config));
    out.write(CONFIG_XML_CONTENTS.getBytes());
    out.close();
    
    return folderDir;
  }
  
  public static File createJob(File jenkinsHome, String jobName) throws IOException {
    final File testJob = createJobsFolderWithConfiguration(jenkinsHome, jobName);
    final File nextBuildNumberFile = new File(testJob, HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME);
    nextBuildNumberFile.createNewFile();
    addBuildNumber(nextBuildNumberFile);
    File workspace = new File(testJob, "workspace");
    workspace.mkdir();
    new File(workspace, "neverBackupMe.txt").createNewFile();
    
    return testJob;
  }
  
  /**
   * When deleting multibranch jobs / folders or removing them we saw leftover directories in the Jenkins
   * filesystem. They do not contain a config.xml nor any other file. We simulate a structure like that here:
   * <pre>JENKINS_HOME/jobs/jobName
   * '- jobs</pre>
   * @param jenkinsHome
   * @param jobName
   * @return
   * @throws IOException
   */
  public static File createMaliciousMultiJob(File jenkinsHome, String jobName) throws IOException {
	    final File emptyJobDir = new File(new File(jenkinsHome, HudsonBackup.JOBS_DIR_NAME), "empty");
	    emptyJobDir.mkdirs();
	    final File jobsDirectory = new File(emptyJobDir, HudsonBackup.JOBS_DIR_NAME);
	    jobsDirectory.mkdir();
	    
	    return emptyJobDir;
  }

  private static File createJobsFolderWithConfiguration(File jenkinsHome, String jobName) throws IOException, FileNotFoundException {
    final File testJob = new File(new File(jenkinsHome, HudsonBackup.JOBS_DIR_NAME), jobName);
    testJob.mkdirs();
    final File config = new File(testJob, "config.xml");
    config.createNewFile();
    final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(config));
    out.write(CONFIG_XML_CONTENTS.getBytes());
    out.close();
    return testJob;
  }
  
  public static File addNewBuildToJob(File job) throws IOException, InterruptedException {
    final File builds = new File(job, HudsonBackup.BUILDS_DIR_NAME);
    builds.mkdir();
    final File build = new File(builds, CONCRETE_BUILD_DIRECTORY_NAME);
    build.mkdir();
    
    final File changelogDir = new File(build, HudsonBackup.CHANGELOG_HISTORY_PLUGIN_DIR_NAME);
    changelogDir.mkdir();
    new File(changelogDir, "1.xml").createNewFile();
    new File(changelogDir, "2.xml").createNewFile();

    new File(build, "build.xml").createNewFile();
    new File(build, "changelog.xml").createNewFile();
    new File(build, "log").createNewFile();
    new File(build, "revision.txt").createNewFile();
    new File(build, "logfile.log").createNewFile();
    new File(build, "logfile.xlog").createNewFile();

    final File archiveDir = new File(build, HudsonBackup.ARCHIVE_DIR_NAME);
    archiveDir.mkdir();
    new File(archiveDir, "someFile.log").createNewFile();
    
    Util.createSymlink(job, HudsonBackup.BUILDS_DIR_NAME+"/"+ CONCRETE_BUILD_DIRECTORY_NAME, "lastSuccessful", new StreamTaskListener(new StringWriter()));
    Util.createSymlink(job, HudsonBackup.BUILDS_DIR_NAME+"/"+ CONCRETE_BUILD_DIRECTORY_NAME, "lastStable", new StreamTaskListener(new StringWriter()));

    return build;
  }
  
  public static void addSingleConfigurationResult(File job) throws IOException, InterruptedException  {
    File configurations = new File(job, HudsonBackup.CONFIGURATIONS_DIR_NAME);
    configurations.mkdir();
    File axis_x = new File(configurations, "axis-x");
    axis_x.mkdir();
    File xValueA = new File(axis_x, "a");
    xValueA.mkdir();
    File xValueB = new File(axis_x, "b");
    xValueB.mkdir();
    
    addNewBuildToJob(xValueA);
    addNewBuildToJob(xValueB);
    
    new File(xValueA, "config.xml").createNewFile();
    File nextBuildnumber = new File(xValueA, "nextBuildNumber");
    nextBuildnumber.createNewFile();
    addBuildNumber(nextBuildnumber);
    
    new File(xValueB, "config.xml").createNewFile();
    nextBuildnumber = new File(xValueB, "nextBuildNumber");
    nextBuildnumber.createNewFile();
    addBuildNumber(nextBuildnumber);
  }
  
  public static void addSinglePromotionResult(File job) throws IOException, InterruptedException  {
    File promotions = new File(job, HudsonBackup.PROMOTIONS_DIR_NAME);
    promotions.mkdir();
    File promotion_x = new File(promotions, "promotion-x");
    promotion_x.mkdir();

    addNewBuildToJob(promotion_x);

    new File(promotion_x, "config.xml").createNewFile();
    File nextBuildnumber = new File(promotion_x, "nextBuildNumber");
    nextBuildnumber.createNewFile();
    addBuildNumber(nextBuildnumber);
  }
  
  public static void addSingleMultibranchResult(File job) throws IOException, InterruptedException  {
    File configurations = new File(job, HudsonBackup.MULTIBRANCH_DIR_NAME);
    configurations.mkdir();
    File branch1 = new File(configurations, "master");
    branch1.mkdir();
    File branch2 = new File(configurations, "development");
    branch2.mkdir();
    
    addNewBuildToJob(branch1);
    addNewBuildToJob(branch2);
    
    new File(branch1, "config.xml").createNewFile();
    File nextBuildnumber = new File(branch1, "nextBuildNumber");
    nextBuildnumber.createNewFile();
    addBuildNumber(nextBuildnumber);
    
    new File(branch2, "config.xml").createNewFile();
    nextBuildnumber = new File(branch2, "nextBuildNumber");
    nextBuildnumber.createNewFile();
    addBuildNumber(nextBuildnumber);

    File indexing = new File(job, HudsonBackup.INDEXING_DIR_NAME);
    indexing.mkdir();
    new File(indexing, "indexing.xml").createNewFile();
    new File(indexing, "indexing.log").createNewFile();
  }

  public static boolean containsStringEndingWith(final List<String> strings, final String pattern) {
    boolean contains = false;

    for (final String string : strings) {
      if (string.endsWith(pattern)) {
        contains = true;
        break;
      }
    }

    return contains;
  }
  
  private static void addBuildNumber(final File nextBuildNumberFile) {
    try (Writer w = new FileWriter(nextBuildNumberFile)) {
      w.write("1234");
    } catch (final IOException e) {
      // catch me if you can!
    }
  }

  public static ThinBackupPluginImpl createMockPlugin(File jenkinsHome, File backupDir) {
    final ThinBackupPluginImpl mockPlugin = mock(ThinBackupPluginImpl.class);
  
    when(mockPlugin.getHudsonHome()).thenReturn(jenkinsHome);
    when(mockPlugin.getFullBackupSchedule()).thenReturn("");
    when(mockPlugin.getDiffBackupSchedule()).thenReturn("");
    when(mockPlugin.getExpandedBackupPath()).thenReturn(backupDir.getAbsolutePath());
    when(mockPlugin.getNrMaxStoredFull()).thenReturn(-1);
    when(mockPlugin.isCleanupDiff()).thenReturn(false);
    when(mockPlugin.isMoveOldBackupsToZipFile()).thenReturn(false);
    when(mockPlugin.isBackupBuildResults()).thenReturn(true);
    when(mockPlugin.isBackupBuildArchive()).thenReturn(false);
    when(mockPlugin.isBackupNextBuildNumber()).thenReturn(false);
    when(mockPlugin.getExcludedFilesRegex()).thenReturn("");
  
    return mockPlugin;
  }
  
  public static File createNode(File jenkinsHome, String nodeName) throws IOException {
    final File testNode = new File(new File(jenkinsHome, HudsonBackup.NODES_DIR_NAME), nodeName);
    testNode.mkdirs();
    final File config = new File(testNode, "config.xml");
    config.createNewFile();
    final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(config));
    out.write(CONFIG_XML_CONTENTS.getBytes());
    out.close();
    return testNode;
  }
}
