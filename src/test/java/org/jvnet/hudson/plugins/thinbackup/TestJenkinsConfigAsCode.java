package org.jvnet.hudson.plugins.thinbackup;


import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestJenkinsConfigAsCode {

    @Rule
    public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void should_support_configuration_as_code() {
        ThinBackupPluginImpl thinBackupPluginConfig = ThinBackupPluginImpl.get();
        final String backupPath = thinBackupPluginConfig.getBackupPath();
        assertEquals("c:\\temp\\thin-backup", backupPath);
        assertEquals("0 12 * * 1-5", thinBackupPluginConfig.getDiffBackupSchedule());
        assertEquals(120, thinBackupPluginConfig.getForceQuietModeTimeout());
        assertTrue(thinBackupPluginConfig.isWaitForIdle());
    }
}
