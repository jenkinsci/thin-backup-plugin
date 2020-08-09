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

import java.util.Calendar;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import antlr.ANTLRException;
import hudson.scheduler.CronTab;

public class TestThinBackupPeriodicWork {

  private ThinBackupPeriodicWork thinBackupPeriodicWork;

  @Before
  public void setup() {
    thinBackupPeriodicWork = new ThinBackupPeriodicWork();
  }

  @Test
  public void testGetNextScheduledBackup() {
    final long currentTime = System.currentTimeMillis();
    final String fullCron = "* * * * *";
    final String diffCron = "* * * * *";
    thinBackupPeriodicWork.getNextScheduledBackupType(currentTime, fullCron, diffCron);
  }

  @Test
  @Ignore("Test is ignored as it testing for a known issue in J/H <= 1.395")
  public void testGetWeekendScheduledBackup() {
    final Calendar cal = Calendar.getInstance();
    cal.set(2011, 0, 16, 0, 0, 0);
    final long testTime = cal.getTimeInMillis();
    final String fullCron = "0 23 * * 0";
    final String diffCron = "0 23 * * 1-5";
    thinBackupPeriodicWork.getNextScheduledBackupType(testTime, fullCron, diffCron);
  }

  @Test
  @Ignore("Test is ignored as it testing for a known issue in J/H <= 1.395")
  public void testHudsonCeil() throws ANTLRException {
    final Calendar cal = Calendar.getInstance();
    cal.set(2011, 0, 16, 0, 0, 0); // Sunday, Jan 16th 2011, 00:00
    final String cronStr = "0 23 * * 1-5"; // execute on weekdays @23:00

    final CronTab cron = new CronTab(cronStr);
    final Calendar next = cron.ceil(cal);

    final Calendar expectedDate = Calendar.getInstance();
    expectedDate.set(2011, 0, 17, 23, 0, 0); // Expected next: Monday, Jan 17th 2011, 23:00
    Assert.assertEquals(expectedDate.get(Calendar.HOUR), next.get(Calendar.HOUR));
    Assert.assertEquals(expectedDate.get(Calendar.MINUTE), next.get(Calendar.MINUTE));
    Assert.assertEquals(expectedDate.get(Calendar.YEAR), next.get(Calendar.YEAR));
    Assert.assertEquals(expectedDate.get(Calendar.MONTH), next.get(Calendar.MONTH));
    Assert.assertEquals(expectedDate.get(Calendar.DAY_OF_MONTH), next.get(Calendar.DAY_OF_MONTH)); // FAILS: is Monday,
                                                                                                   // Jan 10th, 23:00
  }

}
