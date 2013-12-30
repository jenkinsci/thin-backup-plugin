/**
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
package org.jenkins.plugins.thinbackup;

import hudson.Plugin;
import hudson.model.Hudson;

import java.io.File;
import java.util.logging.Logger;

public class ThinBackupPluginImpl extends Plugin {
  private static final Logger LOGGER = Logger.getLogger("org.jenkins.plugins.thinbackup");
  private static ThinBackupPluginImpl instance = null;

  public ThinBackupPluginImpl() {
    instance = this;
  }

  @Override
  public void start() throws Exception {
    super.start();
    load();
    LOGGER.fine("'thinBackup' plugin initialized.");
  }

  public static ThinBackupPluginImpl getInstance() {
    return instance;
  }

  public File getHudsonHome() {
    return Hudson.getInstance().getRootDir();
  }
}
