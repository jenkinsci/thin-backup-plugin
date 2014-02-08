package org.jenkins.plugins.thinbackup.strategies;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.jenkins.plugins.thinbackup.utils.FileNameMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestFreeStyleJobsBackup extends AbstractBackupTestUtils {

  private Jobs jobs;

  @Before
  public void setup() throws IOException {
    File jobsRoot = new File(toBackupTempDir, Jobs.ROOTFOLDER_NAME);
    jobsRoot.mkdir();
    
    File freeStyleJob = new File(jobsRoot, "freeStyleJob");
    freeStyleJob.mkdir();
    
    new File(freeStyleJob, "config.xml").createNewFile();
    new File(freeStyleJob, "nextBuildNumber").createNewFile();
    
    new File(freeStyleJob, "builds").mkdir();
    new File(freeStyleJob, "workspace").mkdir();
    
    jobs = new Jobs();
    jobs.setJenkinsHome(new File(toBackupTempDir));
  }
  
  @After
  public void tearDown() throws IOException {
    FileUtils.cleanDirectory(new File(toBackupTempDir, Jobs.ROOTFOLDER_NAME));
  }
  
  @Test
  public void backupJob() {
    Collection<File> backup = jobs.backup();
    
    assertThat(backup, Matchers.containsInAnyOrder(
        new FileNameMatcher(Jobs.ROOTFOLDER_NAME+File.separator+"freeStyleJob"+File.separator+"config.xml"),
        new FileNameMatcher(Jobs.ROOTFOLDER_NAME+File.separator+"freeStyleJob"+File.separator+"nextBuildNumber"),
        new FileNameMatcher(Jobs.ROOTFOLDER_NAME+File.separator+"freeStyleJob"+File.separator+"builds"),
        new FileNameMatcher(Jobs.ROOTFOLDER_NAME+File.separator+"freeStyleJob"+File.separator+"workspace"),
        new FileNameMatcher(Jobs.ROOTFOLDER_NAME+File.separator+"freeStyleJob"),
        new FileNameMatcher(Jobs.ROOTFOLDER_NAME)));
  }

}
