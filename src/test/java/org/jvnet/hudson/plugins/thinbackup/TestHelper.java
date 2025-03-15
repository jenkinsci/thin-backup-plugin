package org.jvnet.hudson.plugins.thinbackup;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import org.jvnet.hudson.plugins.thinbackup.backup.HudsonBackup;

public class TestHelper {
    public static final String CONFIG_XML_CONTENTS = "FILLED WITH DATA... ";
    public static final String CONCRETE_BUILD_DIRECTORY_NAME = "42";
    public static final String TEST_JOB_NAME = "test";

    /**
     * When deleting multibranch jobs / folders or removing them we saw leftover directories in the Jenkins
     * filesystem. They do not contain a config.xml nor any other file. We simulate a structure like that here:
     * <pre>JENKINS_HOME/jobs/jobName
     * '- jobs</pre>
     * @param jenkinsHome
     * @param jobName
     * @return
     */
    public static File createMaliciousMultiJob(File jenkinsHome, String jobName) throws Exception {
        final File emptyJobDir = newFolder(jenkinsHome, HudsonBackup.JOBS_DIR_NAME, "empty");
        newFolder(emptyJobDir, jobName);

        return emptyJobDir;
    }

    public static File addNewBuildToJob(File job) throws Exception {
        final File builds = newFolder(job, HudsonBackup.BUILDS_DIR_NAME);
        final File build = newFolder(builds, CONCRETE_BUILD_DIRECTORY_NAME);

        final File changelogDir = newFolder(build, HudsonBackup.CHANGELOG_HISTORY_PLUGIN_DIR_NAME);
        newFile(changelogDir, "1.xml");
        newFile(changelogDir, "2.xml");

        newFile(build, "build.xml");
        newFile(build, "changelog.xml");
        newFile(build, "log");
        newFile(build, "revision.txt");
        newFile(build, "logfile.log");
        newFile(build, "logfile.xlog");

        final File archiveDir = newFolder(build, HudsonBackup.ARCHIVE_DIR_NAME);
        newFile(archiveDir, "someFile.log");

        return build;
    }

    public static void addSingleConfigurationResult(File job) throws Exception {
        File configurations = newFolder(job, HudsonBackup.CONFIGURATIONS_DIR_NAME);
        File axis_x = newFolder(configurations, "axis-x");
        File xValueA = newFolder(axis_x, "a");
        File xValueB = newFolder(axis_x, "b");

        addNewBuildToJob(xValueA);
        addNewBuildToJob(xValueB);

        newFile(xValueA, "config.xml");
        File nextBuildnumber = newFile(xValueA, "nextBuildNumber");
        addBuildNumber(nextBuildnumber);

        newFile(xValueB, "config.xml");
        nextBuildnumber = newFile(xValueB, "nextBuildNumber");
        addBuildNumber(nextBuildnumber);
    }

    public static void addSinglePromotionResult(File job) throws Exception {
        File promotions = newFolder(job, HudsonBackup.PROMOTIONS_DIR_NAME);
        File promotion_x = newFolder(promotions, "promotion-x");

        addNewBuildToJob(promotion_x);

        newFile(promotion_x, "config.xml");
        File nextBuildnumber = newFile(promotion_x, "nextBuildNumber");
        addBuildNumber(nextBuildnumber);
    }

    public static void addSingleMultibranchResult(File job) throws Exception {
        File configurations = newFolder(job, HudsonBackup.MULTIBRANCH_DIR_NAME);
        File branch1 = newFolder(configurations, "master");
        File branch2 = newFolder(configurations, "development");

        addNewBuildToJob(branch1);
        addNewBuildToJob(branch2);

        newFile(branch1, "config.xml");
        File nextBuildnumber = newFile(branch1, "nextBuildNumber");
        addBuildNumber(nextBuildnumber);

        newFile(branch2, "config.xml");
        nextBuildnumber = newFile(branch2, "nextBuildNumber");
        addBuildNumber(nextBuildnumber);

        File indexing = newFolder(job, HudsonBackup.INDEXING_DIR_NAME);
        newFile(indexing, "indexing.xml");
        newFile(indexing, "indexing.log");
    }

    private static void addBuildNumber(final File nextBuildNumberFile) {
        try (Writer w = new FileWriter(nextBuildNumberFile)) {
            w.write("1234");
        } catch (final IOException e) {
            // catch me if you can!
        }
    }

    public static File newFolder(File root, String... subDirs) throws Exception {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.exists() && !result.mkdirs()) {
            throw new IOException("Couldn't create folders " + result.getAbsolutePath());
        }
        return result;
    }

    public static File newFile(File root, String filename) throws Exception {
        File result = new File(root, filename);
        if (!result.exists() && !result.createNewFile()) {
            throw new IOException("Couldn't create file " + result.getAbsolutePath());
        }
        return result;
    }
}
