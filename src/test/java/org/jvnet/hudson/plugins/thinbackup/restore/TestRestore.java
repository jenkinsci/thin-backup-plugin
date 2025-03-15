package org.jvnet.hudson.plugins.thinbackup.restore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.jvnet.hudson.plugins.thinbackup.TestHelper.newFile;
import static org.jvnet.hudson.plugins.thinbackup.TestHelper.newFolder;
import static org.jvnet.hudson.plugins.thinbackup.backup.HudsonBackup.NEXT_BUILD_NUMBER_FILE_NAME;
import static org.jvnet.hudson.test.LogRecorder.recorded;

import hudson.model.FreeStyleProject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;
import org.jvnet.hudson.plugins.thinbackup.backup.HudsonBackup;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LogRecorder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class TestRestore {

    @TempDir
    private File tmpFolder;

    @Test
    void testRestoreFromFolder(JenkinsRule r) throws Exception {
        // create to test afterward
        File backupDir = newFolder(tmpFolder, "junit");

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
        final File buildsDir = newFolder(test2rootDir, "builds", "1");
        // add some log files
        newFile(buildsDir, "log"); // should be copied
        newFile(buildsDir, "log.txt"); // should be copied
        newFile(buildsDir, "logfile.log"); // should NOT be copied
        newFile(buildsDir, "logfile.xlog"); // should be copied

        // run backup
        new HudsonBackup(thinBackupPlugin, ThinBackupPeriodicWork.BackupType.FULL, date, r.jenkins).backup();

        // delete jobs
        test.delete();
        test2.delete();

        // check that files are gone
        final File jobs = new File(rootDir, "jobs");
        String[] jobList = jobs.list();
        assertEquals(0, jobList.length);

        // now do the restore without build number
        HudsonRestore hudsonRestore = new HudsonRestore(rootDir, backupDir.getAbsolutePath(), date, false, false);
        hudsonRestore.restore();

        // verify jobs are back
        jobList = jobs.list();
        assertEquals(2, jobList.length);

        // verify NextBuildNumber is missing
        assertFalse(new File(test2rootDir, "nextBuildNumber").exists());

        // restore from backup INCLUDING build number
        hudsonRestore = new HudsonRestore(rootDir, backupDir.getAbsolutePath(), date, true, false);
        hudsonRestore.restore();

        assertTrue(new File(test2rootDir, "nextBuildNumber").exists());
    }

    @Test
    void testRestoreFromZip(JenkinsRule r) throws Exception {
        // create to test afterward
        File backupDir = newFolder(tmpFolder, "junit");

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
        final File buildsDir = newFolder(test2rootDir, "builds", "1");
        // add some log files
        newFile(buildsDir, "log"); // should be copied
        newFile(buildsDir, "log.txt"); // should be copied
        newFile(buildsDir, "logfile.log"); // should NOT be copied
        newFile(buildsDir, "logfile.xlog"); // should be copied

        // run backup
        new HudsonBackup(thinBackupPlugin, ThinBackupPeriodicWork.BackupType.FULL, date, r.jenkins).backup();

        List<String> backupsAsDates = Utils.getBackupsAsDates(backupDir);
        assertEquals(1, backupsAsDates.size());

        // create zips with older backups
        Utils.moveOldBackupsToZipFile(backupDir, null);

        // check that backupset is present
        File[] files = backupDir.listFiles();
        assertEquals(1, files.length);

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
    void testLogsForRestoringWithoutBackupPath(JenkinsRule r) {
        try (LogRecorder l = new LogRecorder().capture(3).record("hudson.plugins.thinbackup", Level.SEVERE)) {
            final HudsonRestore hudsonRestore = new HudsonRestore(null, null, null, false, false);
            hudsonRestore.restore();
            assertThat(
                    l, recorded(Level.SEVERE, containsString("Backup path not specified for restoration. Aborting.")));
        }
    }

    @Test
    void testLogsForRestoringWithoutRestoreFromDate(JenkinsRule r) {
        try (LogRecorder l = new LogRecorder().capture(3).record("hudson.plugins.thinbackup", Level.SEVERE)) {
            final HudsonRestore hudsonRestore = new HudsonRestore(null, "/var/backup", null, false, false);
            hudsonRestore.restore();
            assertThat(
                    l,
                    recorded(Level.SEVERE, containsString("Backup date to restore from was not specified. Aborting.")));
        }
    }
}
