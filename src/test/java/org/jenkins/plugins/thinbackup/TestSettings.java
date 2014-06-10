package org.jenkins.plugins.thinbackup;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;

import hudson.XmlFile;
import hudson.model.Hudson;

import org.jenkins.plugins.thinbackup.utils.FileContentMatcher;
import org.jenkins.plugins.thinbackup.utils.FileNameMatcher;
import org.jenkins.plugins.thinbackup.utils.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSettings {

  private File configFile;

  @Before
  public void setup() {
    String tempDir = System.getProperty("java.io.tmpdir");
    configFile = new File(tempDir, Settings.CONFIGURATION_FILE);
  }
  
  @After
  public void cleanup() {
    configFile.delete();
  }
  
//  @Test
//  public void saveConfiguration() {
//    Settings settings = new Settings("path/to/backupDir") {
//      protected XmlFile configurationFile() {
//        return new XmlFile(Hudson.XSTREAM, configFile);
//      }
//    };
//    
//    settings.save();
//    
//    assertTrue(configFile.exists());
//    assertThat(configFile, new FileContentMatcher(""));
//  }

}
