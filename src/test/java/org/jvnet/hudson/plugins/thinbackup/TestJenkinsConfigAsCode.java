package org.jvnet.hudson.plugins.thinbackup;

import static org.junit.jupiter.api.Assertions.*;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
class TestJenkinsConfigAsCode {

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    void should_support_configuration_as_code(JenkinsConfiguredWithCodeRule r) {
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
