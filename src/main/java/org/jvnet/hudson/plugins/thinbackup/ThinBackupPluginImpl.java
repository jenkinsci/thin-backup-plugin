/*
 * The MIT License
 *
 * Copyright (c) 2011, Borland (a Micro Focus Company), Matthias Steinkogler, Thomas Fuerer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jvnet.hudson.plugins.thinbackup;

import hudson.Plugin;

import java.util.logging.Logger;

public class ThinBackupPluginImpl extends Plugin {
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");
  private static ThinBackupPluginImpl instance = null;

  private String fullBackupSchedule;
  private String diffBackupSchedule;
  private String backupPath;
  private String nrMaxStoredFull;
  private boolean cleanupDiff;

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

  public void setBackupPath(final String backupPath) {
    this.backupPath = backupPath;
  }

  public String getBackupPath() {
    return backupPath;
  }

  public void setFullBackupSchedule(final String fullBackupSchedule) {
    this.fullBackupSchedule = fullBackupSchedule;
  }

  public String getFullBackupSchedule() {
    return fullBackupSchedule;
  }

  public void setDiffBackupSchedule(final String diffBackupSchedule) {
    this.diffBackupSchedule = diffBackupSchedule;
  }

  public String getDiffBackupSchedule() {
    return diffBackupSchedule;
  }

  public void setCleanupDiff(final boolean cleanupDiff) {
    this.cleanupDiff = cleanupDiff;
  }

  public boolean isCleanupDiff() {
    return cleanupDiff;
  }

  public void setNrMaxStoredFull(final String nrMaxStoredFull) {
    this.nrMaxStoredFull = nrMaxStoredFull;
  }

  public String getNrMaxStoredFull() {
    return nrMaxStoredFull;
  }

}
