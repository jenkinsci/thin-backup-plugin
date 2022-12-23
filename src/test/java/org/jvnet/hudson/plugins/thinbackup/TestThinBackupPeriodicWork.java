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


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestThinBackupPeriodicWork {

  @Test
  public void testGetNextScheduledBackup() {
    ThinBackupPeriodicWork thinBackupPeriodicWork = new ThinBackupPeriodicWork();
    final long currentTime = System.currentTimeMillis();
    final String fullCron = "* * * * *";
    final String diffCron = "* * * * *";
    final ThinBackupPeriodicWork.BackupType backupType = thinBackupPeriodicWork.getNextScheduledBackupType(currentTime, fullCron, diffCron);
    assertEquals(ThinBackupPeriodicWork.BackupType.FULL, backupType);
  }
}
