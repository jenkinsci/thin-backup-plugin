package org.jvnet.hudson.plugins.thinbackup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.ExtensionList;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

@WithJenkins
class TestJenkinsWithOldConfig {

    @LocalData
    @Test
    void testOldConfig(JenkinsRule r) {
        final File rootDir = r.jenkins.getRootDir();
        final String[] list = rootDir.list();
        // check that data is converted
        assertThat(list, not(hasItemInArray("thinBackup.xml")));
        assertThat(list, hasItemInArray("org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl.xml"));
        // check that config is correctly set
        final ExtensionList<ThinBackupPluginImpl> plugins = r.jenkins.getExtensionList(ThinBackupPluginImpl.class);
        assertEquals(1, plugins.size());
        assertEquals("c:\\temp\\thin-backup", plugins.get(0).getBackupPath());
    }
}
