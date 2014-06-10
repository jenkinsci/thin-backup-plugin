package org.jenkins.plugins.thinbackup;

import java.io.File;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class Configuration extends AbstractDescribableImpl<Configuration> {
  public String backupPath;
  
  @DataBoundConstructor
  public Configuration(String backupPath) {
    this.backupPath = backupPath;
  }
  
  @Extension
  public static class DescriptorImpl extends Descriptor<Configuration> {
    @Override
    public String getDisplayName() {
      return "";
    }
    
    public FormValidation doCheckBackupPath(@QueryParameter("backupPath") String value) {
      File backupPath = new File(value);
      if (backupPath.exists()) {
        return FormValidation.ok();
      } else {
        return FormValidation.warning("Directory does not exist. Will be created during next backup. Ensure write permissions at this location!");
      }
    }
  }
}
