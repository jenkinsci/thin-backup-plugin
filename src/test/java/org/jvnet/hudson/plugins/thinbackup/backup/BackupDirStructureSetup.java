package org.jvnet.hudson.plugins.thinbackup.backup;

import static org.jvnet.hudson.plugins.thinbackup.TestHelper.newFolder;
import static org.jvnet.hudson.plugins.thinbackup.utils.Utils.getFormattedDirectory;

import java.io.File;
import java.nio.file.Path;
import java.util.Calendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;

public class BackupDirStructureSetup {

    @TempDir
    protected Path tmpFolder;

    protected File backupDir;

    protected File full1;
    protected File diff11;
    protected File diff12;
    protected File diff13;
    protected File diff14;

    protected File full2;
    protected File diff21;

    protected File full3;
    protected File diff31;

    protected File diff41;

    @BeforeEach
    void setup() throws Exception {
        backupDir = tmpFolder.toFile();

        final Calendar cal = Calendar.getInstance();
        cal.set(2011, Calendar.JANUARY, 1, 0, 0);
        full1 = newFolder(getFormattedDirectory(backupDir, BackupType.FULL, cal.getTime()));
        full1.setLastModified(cal.getTimeInMillis());
        cal.set(2011, Calendar.JANUARY, 1, 0, 1);
        diff11 = newFolder(getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime()));
        diff11.setLastModified(cal.getTimeInMillis());
        cal.set(2011, Calendar.JANUARY, 1, 0, 2);
        diff12 = newFolder(getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime()));
        diff12.setLastModified(cal.getTimeInMillis());
        cal.set(2011, Calendar.JANUARY, 1, 0, 3);
        diff13 = newFolder(getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime()));
        diff13.setLastModified(cal.getTimeInMillis());
        cal.set(2011, Calendar.JANUARY, 1, 0, 4);
        diff14 = newFolder(getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime()));
        diff14.setLastModified(cal.getTimeInMillis());

        cal.set(2011, Calendar.FEBRUARY, 1, 0, 0);
        full2 = newFolder(getFormattedDirectory(backupDir, BackupType.FULL, cal.getTime()));
        full2.setLastModified(cal.getTimeInMillis());
        cal.set(2011, Calendar.FEBRUARY, 1, 0, 1);
        diff21 = newFolder(getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime()));
        diff21.setLastModified(cal.getTimeInMillis());

        cal.set(2011, Calendar.MARCH, 1, 0, 0);
        full3 = newFolder(getFormattedDirectory(backupDir, BackupType.FULL, cal.getTime()));
        full3.setLastModified(cal.getTimeInMillis());
        cal.set(2011, Calendar.MARCH, 1, 0, 1);
        diff31 = newFolder(getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime()));
        diff31.setLastModified(cal.getTimeInMillis());

        cal.set(2010, Calendar.APRIL, 1, 0, 1);
        diff41 = newFolder(getFormattedDirectory(backupDir, BackupType.DIFF, cal.getTime()));
        diff41.setLastModified(cal.getTimeInMillis());
    }
}
