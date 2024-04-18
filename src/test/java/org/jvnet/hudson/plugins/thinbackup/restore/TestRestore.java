package org.jvnet.hudson.plugins.thinbackup.restore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.jvnet.hudson.plugins.thinbackup.backup.HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME;
import static org.jvnet.hudson.test.LoggerRule.recorded;

import hudson.model.FreeStyleProject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;
import org.jvnet.hudson.plugins.thinbackup.backup.HudsonBackup;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;

public class TestRestore {

    @Rule
    public LoggerRule l = new LoggerRule();

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testRestoreFromFolder() throws IOException, InterruptedException {
        // create to test afterward
        File backupDir = tmpFolder.newFolder();

        final ThinBackupPluginImpl thinBackupPlugin = ThinBackupPluginImpl.get();
        thinBackupPlugin.setBackupPath(backupDir.getAbsolutePath());
        thinBackupPlugin.setBackupBuildResults(true);
        thinBackupPlugin.setBackupNextBuildNumber(true);
        final File rootDir = r.jenkins.getRootDir();
        final Date date = new Date();

        // create 2 jobs
        final FreeStyleProject test = r.createFreeStyleProject("test");
        final FreeStyleProject test2 = r.createFreeStyleProject("test2");

        // add build info to one
        final File test2rootDir = test2.getRootDir();
        final File nextNumber = new File(test2rootDir, NEXT_BUILD_NUMBER_FILE_NAME);
        Files.write(nextNumber.toPath(), "42".getBytes(), StandardOpenOption.CREATE_NEW);
        final File buildsDir = new File(test2rootDir, "builds/1");
        buildsDir.mkdirs();
        // add some log files
        new File(buildsDir, "log").createNewFile(); // should be copied
        new File(buildsDir, "log.txt").createNewFile(); // should be copied
        new File(buildsDir, "logfile.log").createNewFile(); // should NOT be copied
        new File(buildsDir, "logfile.xlog").createNewFile(); // should be copied

        // run backup
        new HudsonBackup(thinBackupPlugin, ThinBackupPeriodicWork.BackupType.FULL, date, r.jenkins).backup();

        // delete jobs
        test.delete();
        test2.delete();

        // check that files are gone
        final File jobs = new File(rootDir, "jobs");
        String[] jobList = jobs.list();
        assertEquals(jobList.length, 0);

        // now do the restore without build number
        HudsonRestore hudsonRestore = new HudsonRestore(rootDir, backupDir.getAbsolutePath(), date, false, false);
        hudsonRestore.restore();

        // verify jobs are back
        jobList = jobs.list();
        assertEquals(jobList.length, 2);

        // verify NextBuildNumber is missing
        assertFalse(new File(test2rootDir, "nextBuildNumber").exists());

        // restore from backup INCLUDING build number
        hudsonRestore = new HudsonRestore(rootDir, backupDir.getAbsolutePath(), date, true, false);
        hudsonRestore.restore();

        assertTrue(new File(test2rootDir, "nextBuildNumber").exists());
    }

    @Test
    public void testRestoreFromZip() throws IOException, InterruptedException, ParseException {
        // create to test afterward
        File backupDir = tmpFolder.newFolder();

        final ThinBackupPluginImpl thinBackupPlugin = ThinBackupPluginImpl.get();
        thinBackupPlugin.setBackupPath(backupDir.getAbsolutePath());
        thinBackupPlugin.setBackupBuildResults(true);
        thinBackupPlugin.setBackupNextBuildNumber(true);
        final File rootDir = r.jenkins.getRootDir();
        Date date = new Date();

        // create 2 jobs
        final FreeStyleProject test = r.createFreeStyleProject("test");
        final FreeStyleProject test2 = r.createFreeStyleProject("test2");

        // add build info to one
        final File test2rootDir = test2.getRootDir();
        final File nextNumber = new File(test2rootDir, NEXT_BUILD_NUMBER_FILE_NAME);
        Files.write(nextNumber.toPath(), "42".getBytes(), StandardOpenOption.CREATE_NEW);
        final File buildsDir = new File(test2rootDir, "builds/1");
        buildsDir.mkdirs();
        // add some log files
        new File(buildsDir, "log").createNewFile(); // should be copied
        new File(buildsDir, "log.txt").createNewFile(); // should be copied
        new File(buildsDir, "logfile.log").createNewFile(); // should NOT be copied
        new File(buildsDir, "logfile.xlog").createNewFile(); // should be copied

        // run backup
        new HudsonBackup(thinBackupPlugin, ThinBackupPeriodicWork.BackupType.FULL, date, r.jenkins).backup();

        List<String> backupsAsDates = Utils.getBackupsAsDates(backupDir);
        Assertions.assertEquals(1, backupsAsDates.size());

        // create zips with older backups
        Utils.moveOldBackupsToZipFile(backupDir, null);

        // check that backupset is present
        File[] files = backupDir.listFiles();
        Assertions.assertEquals(1, files.length);

        // delete jobs
        test.delete();
        test2.delete();

        // check that files are gone
        final File jobs = new File(rootDir, "jobs");
        String[] jobList = jobs.list();
        assertEquals(0, jobList.length);

        // check that backup is available
        backupsAsDates = Utils.getBackupsAsDates(backupDir);
        assertEquals(1, backupsAsDates.size());

        final Date restoreFromDate = new SimpleDateFormat(Utils.DISPLAY_DATE_FORMAT).parse(backupsAsDates.get(0));

        // now do the restore without build number
        HudsonRestore hudsonRestore =
                new HudsonRestore(rootDir, backupDir.getAbsolutePath(), restoreFromDate, false, false);
        hudsonRestore.restore();

        // verify jobs are back
        jobList = jobs.list();
        assertEquals(2, jobList.length);

        // verify NextBuildNumber is missing
        assertFalse(new File(test2rootDir, "nextBuildNumber").exists());

        // restore from backup INCLUDING build number
        hudsonRestore = new HudsonRestore(rootDir, backupDir.getAbsolutePath(), restoreFromDate, true, false);
        hudsonRestore.restore();

        assertTrue(new File(test2rootDir, "nextBuildNumber").exists());
    }

    @Test
    public void testLogsForRestoringWithoutBackupPath() {
        l.capture(3).record("hudson.plugins.thinbackup", Level.SEVERE);
        final HudsonRestore hudsonRestore = new HudsonRestore(null, null, null, false, false);
        hudsonRestore.restore();
        assertThat(l, recorded(Level.SEVERE, containsString("Backup path not specified for restoration. Aborting.")));
    }

    @Test
    public void testLogsForRestoringWithoutRestoreFromDate() {
        l.capture(3).record("hudson.plugins.thinbackup", Level.SEVERE);
        final HudsonRestore hudsonRestore = new HudsonRestore(null, "/var/backup", null, false, false);
        hudsonRestore.restore();
        assertThat(
                l, recorded(Level.SEVERE, containsString("Backup date to restore from was not specified. Aborting.")));
    }
}
