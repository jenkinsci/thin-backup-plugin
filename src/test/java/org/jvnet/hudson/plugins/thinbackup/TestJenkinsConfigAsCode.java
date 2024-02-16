package org.jvnet.hudson.plugins.thinbackup;


import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestJenkinsConfigAsCode {

    @Rule
    public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void should_support_configuration_as_code() {
        ThinBackupPluginImpl thinBackupPluginConfig = ThinBackupPluginImpl.get();
        final String backupPath = thinBackupPluginConfig.getBackupPath();
        // test strings
        assertEquals("c:\\temp\\thin-backup", backupPath);
        assertEquals("0 12 * * 1-5", thinBackupPluginConfig.getDiffBackupSchedule());
        assertEquals("0 12 * * 1", thinBackupPluginConfig.getFullBackupSchedule());
        assertEquals("^.*\\.(log)$", thinBackupPluginConfig.getExcludedFilesRegex());
        assertEquals("^.*\\.(txt)$", thinBackupPluginConfig.getBackupAdditionalFilesRegex());
        // test numbers
        assertEquals(120, thinBackupPluginConfig.getForceQuietModeTimeout());
        assertEquals(-1, thinBackupPluginConfig.getNrMaxStoredFull());
        // test booleans
        assertTrue(thinBackupPluginConfig.isWaitForIdle());
        assertTrue(thinBackupPluginConfig.isBackupBuildResults());
        assertTrue(thinBackupPluginConfig.isFailFast());

        assertFalse(thinBackupPluginConfig.isCleanupDiff());
        assertFalse(thinBackupPluginConfig.isMoveOldBackupsToZipFile());
        assertFalse(thinBackupPluginConfig.isBackupBuildArchive());
        assertFalse(thinBackupPluginConfig.isBackupPluginArchives());
        assertFalse(thinBackupPluginConfig.isBackupUserContents());
        assertFalse(thinBackupPluginConfig.isBackupConfigHistory());
        assertFalse(thinBackupPluginConfig.isBackupAdditionalFiles());
        assertFalse(thinBackupPluginConfig.isBackupNextBuildNumber());
        assertFalse(thinBackupPluginConfig.isBackupBuildsToKeepOnly());
    }
}
