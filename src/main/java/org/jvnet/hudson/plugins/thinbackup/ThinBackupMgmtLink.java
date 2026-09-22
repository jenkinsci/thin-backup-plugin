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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jenkins.util.Timer;
import org.jvnet.hudson.plugins.thinbackup.restore.HudsonRestore;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.verb.POST;

/**
 * A backup solution for Hudson. Backs up configuration files from Hudson and its jobs.
 * <p>
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
        return "symbol-archive-outline plugin-ionicons-api";
    }

    @Override
    public String getUrlName() {
        return "thinBackup";
    }

    @Override
    public String getDescription() {
        return "Backup your global and job specific configuration.";
    }

    @POST
    public void doBackupManual(final StaplerRequest2 res, final StaplerResponse2 rsp) throws IOException {
        final Jenkins jenkins = Jenkins.get();
        jenkins.checkPermission(Jenkins.ADMINISTER);
        LOGGER.info("Starting manual backup.");

        final ThinBackupPeriodicWork manualBackupWorker = new ThinBackupPeriodicWork() {
            @Override
            protected void execute(final TaskListener arg0) {
                backupNow(BackupType.FULL);
            }
        };
        Timer.get().schedule(manualBackupWorker, 0, TimeUnit.SECONDS);

        rsp.sendRedirect(res.getContextPath() + THIN_BACKUP_SUBPATH);
    }

    @POST
    public void doRestore(
            final StaplerRequest2 res,
            final StaplerResponse2 rsp,
            @QueryParameter("restoreBackupFrom") final String restoreBackupFrom,
            @QueryParameter("restoreNextBuildNumber") final String restoreNextBuildNumber,
            @QueryParameter("restorePlugins") final String restorePlugins,
            @QueryParameter("restoreConfigHistory") final String restoreConfigHistory)
            throws IOException {
        LOGGER.info("Starting restore operation.");

        final Jenkins jenkins = Jenkins.get();
        jenkins.checkPermission(Jenkins.ADMINISTER);

        jenkins.doQuietDown();
        LOGGER.fine("Waiting until executors are idle to perform restore...");
        Utils.waitUntilIdle();

        try {
            final File jenkinsHome = jenkins.getRootDir();
            final Date restoreFromDate = new SimpleDateFormat(Utils.DISPLAY_DATE_FORMAT).parse(restoreBackupFrom);

            final HudsonRestore hudsonRestore = new HudsonRestore(
                    jenkinsHome,
                    ThinBackupPluginImpl.get().getExpandedBackupPath(),
                    restoreFromDate,
                    "on".equals(restoreNextBuildNumber),
                    "on".equals(restorePlugins),
                    "on".equals(restoreConfigHistory));
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

    public ThinBackupPluginImpl getConfiguration() {
        return ThinBackupPluginImpl.get();
    }

    public List<String> getAvailableBackups() {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.get();
        return Utils.getBackupsAsDates(new File(plugin.getExpandedBackupPath()));
    }

    @POST
    public ListBoxModel doFillBackupItems() {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        final ThinBackupPluginImpl plugin = ThinBackupPluginImpl.get();
        final List<String> backupsAsDates = Utils.getBackupsAsDates(new File(plugin.getExpandedBackupPath()));
        var model = new ListBoxModel();
        for (String entry : backupsAsDates) {
            model.add(new ListBoxModel.Option(entry));
        }
        return model;
    }

    /**
     * Name of the category for this management link. Exists so that plugins with core dependency pre-dating the version
     * when this was introduced can define a category.
     * @return name of the desired category, one of the enum values of Category, e.g. {@code STATUS}.
     * @since 2.226
     */
    @NonNull
    @Override
    public Category getCategory() {
        return Category.TOOLS;
    }
}
