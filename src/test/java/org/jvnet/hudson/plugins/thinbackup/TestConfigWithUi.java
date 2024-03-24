package org.jvnet.hudson.plugins.thinbackup;

import static org.junit.Assert.assertEquals;

import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlTextInput;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsSessionRule;

public class TestConfigWithUi {

    @Rule
    public JenkinsSessionRule sessions = new JenkinsSessionRule();

    /**
     * Sets backupPath and checks that it survives the next visit.
     */
    @Test
    public void uiAndStorage() throws Throwable {
        sessions.then(r -> {
            assertEquals("", ThinBackupPluginImpl.get().getBackupPath());
            final HtmlForm config =
                    r.createWebClient().goTo("manage/thinBackup/backupsettings").getFormByName("saveSettings");

            HtmlTextInput textbox = config.getInputByName("backupPath");
            textbox.setText("c:\\temp");
            r.submit(config);
            assertEquals(
                    "global config page let us edit it",
                    "c:\\temp",
                    ThinBackupPluginImpl.get().getBackupPath());
        });
        sessions.then(r -> {
            assertEquals(
                    "still there after restart of Jenkins",
                    "c:\\temp",
                    ThinBackupPluginImpl.get().getBackupPath());
        });
    }
}
