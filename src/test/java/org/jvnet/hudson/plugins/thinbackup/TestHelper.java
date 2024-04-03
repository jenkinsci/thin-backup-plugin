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
    public static File createMaliciousMultiJob(File jenkinsHome, String jobName) {
        final File emptyJobDir = new File(new File(jenkinsHome, HudsonBackup.JOBS_DIR_NAME), "empty");
        emptyJobDir.mkdirs();
        final File jobsDirectory = new File(emptyJobDir, jobName);
        jobsDirectory.mkdir();

        return emptyJobDir;
    }

    public static File addNewBuildToJob(File job) throws IOException {
        final File builds = new File(job, HudsonBackup.BUILDS_DIR_NAME);
        builds.mkdirs();
        final File build = new File(builds, CONCRETE_BUILD_DIRECTORY_NAME);
        build.mkdirs();

        final File changelogDir = new File(build, HudsonBackup.CHANGELOG_HISTORY_PLUGIN_DIR_NAME);
        changelogDir.mkdirs();
        new File(changelogDir, "1.xml").createNewFile();
        new File(changelogDir, "2.xml").createNewFile();

        new File(build, "build.xml").createNewFile();
        new File(build, "changelog.xml").createNewFile();
        new File(build, "log").createNewFile();
        new File(build, "revision.txt").createNewFile();
        new File(build, "logfile.log").createNewFile();
        new File(build, "logfile.xlog").createNewFile();

        final File archiveDir = new File(build, HudsonBackup.ARCHIVE_DIR_NAME);
        archiveDir.mkdirs();
        new File(archiveDir, "someFile.log").createNewFile();

        return build;
    }

    public static void addSingleConfigurationResult(File job) throws IOException {
        File configurations = new File(job, HudsonBackup.CONFIGURATIONS_DIR_NAME);
        configurations.mkdir();
        File axis_x = new File(configurations, "axis-x");
        axis_x.mkdir();
        File xValueA = new File(axis_x, "a");
        xValueA.mkdir();
        File xValueB = new File(axis_x, "b");
        xValueB.mkdir();

        addNewBuildToJob(xValueA);
        addNewBuildToJob(xValueB);

        new File(xValueA, "config.xml").createNewFile();
        File nextBuildnumber = new File(xValueA, "nextBuildNumber");
        nextBuildnumber.createNewFile();
        addBuildNumber(nextBuildnumber);

        new File(xValueB, "config.xml").createNewFile();
        nextBuildnumber = new File(xValueB, "nextBuildNumber");
        nextBuildnumber.createNewFile();
        addBuildNumber(nextBuildnumber);
    }

    public static void addSinglePromotionResult(File job) throws IOException {
        File promotions = new File(job, HudsonBackup.PROMOTIONS_DIR_NAME);
        promotions.mkdir();
        File promotion_x = new File(promotions, "promotion-x");
        promotion_x.mkdir();

        addNewBuildToJob(promotion_x);

        new File(promotion_x, "config.xml").createNewFile();
        File nextBuildnumber = new File(promotion_x, "nextBuildNumber");
        nextBuildnumber.createNewFile();
        addBuildNumber(nextBuildnumber);
    }

    public static void addSingleMultibranchResult(File job) throws IOException, InterruptedException {
        File configurations = new File(job, HudsonBackup.MULTIBRANCH_DIR_NAME);
        configurations.mkdir();
        File branch1 = new File(configurations, "master");
        branch1.mkdir();
        File branch2 = new File(configurations, "development");
        branch2.mkdir();

        addNewBuildToJob(branch1);
        addNewBuildToJob(branch2);

        new File(branch1, "config.xml").createNewFile();
        File nextBuildnumber = new File(branch1, "nextBuildNumber");
        nextBuildnumber.createNewFile();
        addBuildNumber(nextBuildnumber);

        new File(branch2, "config.xml").createNewFile();
        nextBuildnumber = new File(branch2, "nextBuildNumber");
        nextBuildnumber.createNewFile();
        addBuildNumber(nextBuildnumber);

        File indexing = new File(job, HudsonBackup.INDEXING_DIR_NAME);
        indexing.mkdir();
        new File(indexing, "indexing.xml").createNewFile();
        new File(indexing, "indexing.log").createNewFile();
    }

    private static void addBuildNumber(final File nextBuildNumberFile) {
        try (Writer w = new FileWriter(nextBuildNumberFile)) {
            w.write("1234");
        } catch (final IOException e) {
            // catch me if you can!
        }
    }
}
