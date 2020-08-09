/*
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
package org.jvnet.hudson.plugins.thinbackup;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.jvnet.hudson.plugins.thinbackup.restore.HudsonRestore;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkins.util.Timer;

/**
 * A backup solution for Hudson. Backs up configuration files from Hudson and its jobs.
 *
 * Originally based on the Backup plugin by Vincent Sellier, Manufacture Fran�aise des Pneumatiques Michelin, Romain
 * Seguy, et.al. Subsequently heavily modified.
 */
@Extension
public class ThinBackupMgmtLink extends ManagementLink {
  private static final String THIN_BACKUP_SUBPATH = "/thinBackup";
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

  @Override
  public String getDisplayName() {
    return "ThinBackup";
  }

  @Override
  public String getIconFileName() {
    return "package.png";
  }

  @Override
  public String getUrlName() {
    return "thinBackup";
  }

  @Override
  public String getDescription() {
    return "Backup your global and job specific configuration.";
  }

  public void doBackupManual(final StaplerRequest res, final StaplerResponse rsp) throws IOException {
    LOGGER.info("Starting manual backup.");

    final Jenkins jenkins = Jenkins.getInstance();
    if (jenkins == null) {
      return;
    }
    jenkins.checkPermission(Jenkins.ADMINISTER);

    final ThinBackupPeriodicWork manualBackupWorker = new ThinBackupPeriodicWork() {
      @Override
      protected void execute(final TaskListener arg0) {
        backupNow(BackupType.FULL);
      }
    };
    Timer.get().schedule(manualBackupWorker, 0, TimeUnit.SECONDS);

    rsp.sendRedirect(res.getContextPath() + THIN_BACKUP_SUBPATH);
  }

  public void doRestore(final StaplerRequest res, final StaplerResponse rsp,
      @QueryParameter("restoreBackupFrom") final String restoreBackupFrom,
      @QueryParameter("restoreNextBuildNumber") final String restoreNextBuildNumber,
      @QueryParameter("restorePlugins") final String restorePlugins) throws IOException {
    LOGGER.info("Starting restore operation.");

    final Jenkins jenkins = Jenkins.getInstance();
    if (jenkins == null) {
      return;
    }
    jenkins.checkPermission(Jenkins.ADMINISTER);

    jenkins.doQuietDown();
    LOGGER.fine("Waiting until executors are idle to perform restore...");
    Utils.waitUntilIdle();

    try {
      final File hudsonHome = jenkins.getRootDir();
      final Date restoreFromDate = new SimpleDateFormat(Utils.DISPLAY_DATE_FORMAT).parse(restoreBackupFrom);

      final HudsonRestore hudsonRestore = new HudsonRestore(hudsonHome, ThinBackupPluginImpl.getInstance()
          .getExpandedBackupPath(), restoreFromDate, "on".equals(restoreNextBuildNumber), "on".equals(restorePlugins));
      hudsonRestore.restore();

      LOGGER.info("Restore finished.");
    } catch (ParseException e) {
      LOGGER.severe("Cannot parse restore option. Aborting.");
    } catch (final Exception ise) {
      LOGGER.severe("Could not restore. Aborting.");
    } finally {
      jenkins.doCancelQuietDown();
      rsp.sendRedirect(res.getContextPath() + THIN_BACKUP_SUBPATH);
    }
  }

  public void doSaveSettings(final StaplerRequest res, final StaplerResponse rsp,
      @QueryParameter("backupPath") final String backupPath,
      @QueryParameter("fullBackupSchedule") final String fullBackupSchedule,
      @QueryParameter("diffBackupSchedule") final String diffBackupSchedule,
      @QueryParameter("nrMaxStoredFull") final String nrMaxStoredFull,
      @QueryParameter("excludedFilesRegex") final String excludedFilesRegex,
      @QueryParameter("moveOldBackupsToZipFile") final boolean moveOldBackupsToZipFile,
      @QueryParameter("cleanupDiff") final boolean cleanupDiff,
      @QueryParameter("backupBuildResults") final boolean backupBuildResults,
      @QueryParameter("backupBuildArchive") final boolean backupBuildArchive,
      @QueryParameter("backupBuildsToKeepOnly") final boolean backupBuildsToKeepOnly,
      @QueryParameter("backupUserContents") final boolean backupUserContents,
      @QueryParameter("backupNextBuildNumber") final boolean backupNextBuildNumber,
      @QueryParameter("backupPluginArchives") final boolean backupPluginArchives,
      @QueryParameter("backupAdditionalFiles") final boolean backupAdditionalFiles,
      @QueryParameter("backupAdditionalFilesRegex") final String backupAdditionalFilesRegex,
      @QueryParameter("waitForIdle") final boolean waitForIdle,
      @QueryParameter("forceQuietModeTimeout") final String forceQuietModeTimeout) throws IOException {
    Jenkins jenkins = Jenkins.getInstance();
    if (jenkins == null) {
      return;
    }
    jenkins.checkPermission(Jenkins.ADMINISTER);

    final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.getInstance();
    plugin.setBackupPath(backupPath);
    plugin.setFullBackupSchedule(fullBackupSchedule);
    plugin.setDiffBackupSchedule(diffBackupSchedule);
    plugin.setNrMaxStoredFullAsString(nrMaxStoredFull);
    plugin.setExcludedFilesRegex(excludedFilesRegex);
    plugin.setCleanupDiff(cleanupDiff);
    plugin.setMoveOldBackupsToZipFile(moveOldBackupsToZipFile);
    plugin.setBackupBuildResults(backupBuildResults);
    plugin.setBackupBuildArchive(backupBuildArchive);
    plugin.setBackupBuildsToKeepOnly(backupBuildsToKeepOnly);
    plugin.setBackupUserContents(backupUserContents);
    plugin.setBackupNextBuildNumber(backupNextBuildNumber);
    plugin.setBackupPluginArchives(backupPluginArchives);
    plugin.setBackupAdditionalFiles(backupAdditionalFiles);
    plugin.setBackupAdditionalFilesRegex(backupAdditionalFilesRegex);
    plugin.setWaitForIdle(waitForIdle);
    plugin.setForceQuietModeTimeout(Integer.parseInt(forceQuietModeTimeout));
    plugin.save();
    LOGGER.finest("Saving backup settings done.");
    rsp.sendRedirect(res.getContextPath() + THIN_BACKUP_SUBPATH);
  }

  public ThinBackupPluginImpl getConfiguration() {
    return ThinBackupPluginImpl.getInstance();
  }

  public List<String> getAvailableBackups() {
    final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.getInstance();
    return Utils.getBackupsAsDates(new File(plugin.getExpandedBackupPath()));
  }

}
