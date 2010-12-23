package org.jvnet.hudson.plugins.thinbackup;

import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.model.Hudson;
import hudson.scheduler.CronTab;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import antlr.ANTLRException;

@Extension
public class ThinBackupMgmtLink extends ManagementLink {
  private static final Logger LOGGER = Logger
      .getLogger("hudson.plugins.thinbackup");

  public String getDisplayName() {
    return "ThinBackup";
  }

  @Override
  public String getIconFileName() {
    return "package.gif";
  }

  @Override
  public String getUrlName() {
    return "thinBackup";
  }

  public void doBackupManual(final StaplerRequest res, final StaplerResponse rsp)
      throws IOException {
    Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
    final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.getInstance();
    plugin.runBackup();
    LOGGER.info("Manual backup finished");
    rsp.sendRedirect(res.getContextPath() + "/thinBackup");
  }

  public void doSaveSettings(final StaplerRequest res,
      final StaplerResponse rsp,
      @QueryParameter("backupPath") final String backupPath,
      @QueryParameter("backupTime") final String backupTime) throws IOException {
    Hudson.getInstance().checkPermission(Hudson.ADMINISTER);

    final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.getInstance();
    plugin.setBackupPath(backupPath);
    plugin.setBackupTime(backupTime);
    plugin.save();
    LOGGER.info("Save backup settings done.");
    rsp.sendRedirect(res.getContextPath() + "/thinBackup");
  }

  public FormValidation doCheckBackupPath(final StaplerRequest res,
      final StaplerResponse rsp,
      @QueryParameter("backupPath") final String backupPath) {
    if ((backupPath == null) || backupPath.isEmpty()) {
      return FormValidation.error("'Backup Path' is not mandatory.");
    }

    final File backupdir = new File(backupPath);
    if (!backupdir.exists()) {
      return FormValidation
          .warning("The given directory does not exist, it will be created during the first run.");
    }
    if (!backupdir.isDirectory()) {
      return FormValidation.error("A file with this name already exists.");
    }
    return FormValidation.ok();
  }

  public FormValidation doCheckBackupTime(final StaplerRequest res,
      final StaplerResponse rsp,
      @QueryParameter("backupTime") final String backupTime) {
    String message;
    if ((backupTime != null) && !backupTime.isEmpty()) {
      try {
        message = new CronTab(backupTime).checkSanity();
      } catch (final ANTLRException e) {
        return FormValidation.error(e.getMessage());
      }
      if (message != null) {
        return FormValidation.warning(message);
      } else {
        return FormValidation.ok();
      }
    } else {
      return FormValidation.ok();
    }
  }

  public ThinBackupPluginImpl getConfiguration() {
    return ThinBackupPluginImpl.getInstance();
  }

}
