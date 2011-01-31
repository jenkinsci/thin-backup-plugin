package org.jvnet.hudson.plugins.thinbackup;

import hudson.scheduler.CronTab;

import java.util.Calendar;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import antlr.ANTLRException;

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
    thinBackupPeriodicWork.getNextScheduledBackup(currentTime, fullCron, diffCron);
  }

  @Test
  public void testGetWeekendScheduledBackup() {
    final Calendar cal = Calendar.getInstance();
    cal.set(2011, 0, 16, 0, 0, 0);
    final long testTime = cal.getTimeInMillis();
    final String fullCron = "0 23 * * 0";
    final String diffCron = "0 23 * * 1-5";
    thinBackupPeriodicWork.getNextScheduledBackup(testTime, fullCron, diffCron);
  }

  @Test
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
    Assert.assertEquals(expectedDate.get(Calendar.DAY_OF_MONTH), next.get(Calendar.DAY_OF_MONTH)); // FAILS: is Monday, Jan 10th, 23:00
  }

}
