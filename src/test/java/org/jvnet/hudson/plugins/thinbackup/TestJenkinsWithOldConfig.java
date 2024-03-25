package org.jvnet.hudson.plugins.thinbackup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.not;

import hudson.ExtensionList;
import java.io.File;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

public class TestJenkinsWithOldConfig {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @LocalData
    @Test
    public void testOldConfig() {
        final File rootDir = r.jenkins.getRootDir();
        final String[] list = rootDir.list();
        // check that data is converted
        assertThat(list, not(hasItemInArray("thinBackup.xml")));
        assertThat(list, hasItemInArray("org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl.xml"));
        // check that config is correctly set
        final ExtensionList<ThinBackupPluginImpl> plugins = r.jenkins.getExtensionList(ThinBackupPluginImpl.class);
        Assert.assertEquals(1, plugins.size());
        Assert.assertEquals("c:\\temp\\thin-backup", plugins.get(0).getBackupPath());
    }
}
