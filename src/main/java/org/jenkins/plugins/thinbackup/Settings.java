package org.jenkins.plugins.thinbackup;

import hudson.BulkChange;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.Descriptor;
import hudson.model.Hudson;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

@Extension
public class Settings extends ThinBackupMenu implements Saveable {
  public static final String CONFIGURATION_FILE = "thinBackup_Config.xml";
  private static final Logger LOGGER = Logger.getLogger("thinbackup");
  
  private List<Configuration> configurations = Arrays.asList(new Configuration("place your backup path here"));
  
  public Settings(){
  }
    
  @DataBoundConstructor
  public Settings(List<Configuration> configurations) {
    this.configurations = configurations;
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
  
  public synchronized void doSaveSettings(StaplerRequest req, StaplerResponse rsp,
      @QueryParameter("backupPath") String backupPath) throws IOException {
    Hudson.getInstance().checkPermission(Hudson.ADMINISTER);

    save();
  }
  
  protected void load() {
    XmlFile xml = configurationFile();
    if (xml.exists()) {
      try {
        xml.unmarshal(this);
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Cannot read Configuration File.", e);
      }
    }
  }

  public void save() {
    if (BulkChange.contains(this)) {
      return;
    }
    try {
      configurationFile().write(this);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Cannot write Configuration File.", e);
    }
  }

  protected XmlFile configurationFile() {
    return new XmlFile(Hudson.XSTREAM, new File(Hudson.getInstance().getRootDir(), CONFIGURATION_FILE));
  }

  public List<Configuration> getConfigurations() {
    return configurations;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<ThinBackupMenu> {
    @Override
    public String getDisplayName() {
      return "";
    }
  }
}
