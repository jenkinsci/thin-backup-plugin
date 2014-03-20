package org.jenkins.plugins.thinbackup;

import hudson.Extension;
import hudson.RelativePath;
import hudson.model.Hudson;
import hudson.util.FormValidation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import jenkins.model.Jenkins;

import org.jenkins.plugins.thinbackup.configuration.Configuration;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

@Extension
public final class Settings extends ThinBackupMenu {
  private static final String CONFIGURATION_FILENAME = "thinBackupConfiguration.xml";
  private Configuration configuration;

  public Settings() {
//    configuration = loadConfiguration();
  }

  private static Configuration loadConfiguration() {
    return (Configuration) Jenkins.XSTREAM2.fromXML(CONFIGURATION_FILENAME);
  }

  private static void saveConfiguration(Configuration configuration) {
//    try {
//      Jenkins.XSTREAM2.toXML(configuration, new FileWriter(CONFIGURATION_FILENAME));
//    } catch (IOException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
  }

  @Override
  public String getIconPath() {
    return "/plugin/thinBackup/images/settings.png";
  }

  @Override
  public String getDisplayName() {
    return "Settings";
  }

  @Override
  public String getDescription() {
    return "Configure ThinBackup to your needs.";
  }

  public synchronized void doSaveSettings(StaplerRequest req, StaplerResponse rsp) {
    Hudson.getInstance().checkPermission(Hudson.ADMINISTER);

    saveConfiguration(getConfiguration());
  }

  public FormValidation doCheckBackupPath(@RelativePath("configuration") @QueryParameter("backupPath") String value) {
    File backupPath = new File(value);
    if (backupPath.exists()) {
      return FormValidation.ok();
    } else {
      return FormValidation.warning("Directory does not exist. Will be created during next backup. Ensure write permissions at this location!");
    }
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }
}
