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
package org.jvnet.hudson.plugins.thinbackup.restore;

import hudson.PluginManager;
import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import hudson.model.UpdateSite.Plugin;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import jenkins.model.Jenkins;
import org.springframework.security.core.Authentication;

public class PluginRestoreUpdateCenter extends UpdateCenter {
    public class PluginRestoreJob extends DownloadJob {

        private Plugin plugin;
        private String version;

        private final PluginManager pm;

        public PluginRestoreJob(UpdateSite site, Authentication auth, Plugin plugin, String version) {
            super(site, auth);
            this.plugin = plugin;
            this.version = version;

            Jenkins jenkins = Jenkins.getInstanceOrNull();
            if (jenkins == null) {
                throw new RuntimeException("Setup the Jenkins environment failed.");
            }
            this.pm = jenkins.getPluginManager();
        }

        @Override
        protected URL getURL() throws MalformedURLException {
            String latestVersion = plugin.version;
            String newUrl = plugin.url.replace(latestVersion, version);
            return new URL(newUrl);
        }

        @Override
        protected File getDestination() {
            return new File(pm.rootDir, plugin.name + ".hpi");
        }

        @Override
        public String getName() {
            return plugin.getDisplayName();
        }

        @Override
        protected void onSuccess() {
            pm.pluginUploaded = true;
        }

        @Override
        public String toString() {
            return super.toString() + "[plugin=" + plugin.title + "]";
        }

        @Override
        protected void _run() throws IOException, InstallationStatus {
            super._run();
        }
    }

    private Set<UpdateSite> knownUpdateSites = new HashSet<>();

    synchronized Future<UpdateCenterJob> addNewJob(UpdateCenterJob job) {
        if (knownUpdateSites.add(job.site)) {
            new ConnectionCheckJob(job.site).submit();
        }
        return job.submit();
    }
}
