package org.jvnet.hudson.plugins.thinbackup;

import static org.junit.Assert.assertEquals;

import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlNumberInput;
import org.htmlunit.html.HtmlTextInput;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsSessionRule;

public class TestConfigWithUi {

    @Rule
    public JenkinsSessionRule sessions = new JenkinsSessionRule();

    /**
     * Sets backupPath and max stored full backups and checks that it survives the next visit.
     */
    @Test
    public void uiAndStorage() throws Throwable {
        sessions.then(r -> {
            assertEquals("", ThinBackupPluginImpl.get().getBackupPath());
            assertEquals(-1, ThinBackupPluginImpl.get().getNrMaxStoredFull());
            final HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");

            HtmlTextInput textbox = config.getInputByName("_.backupPath");
            HtmlNumberInput numberInput = config.getInputByName("_.nrMaxStoredFull");
            textbox.setText("c:\\temp");
            numberInput.setText("10");
            r.submit(config);
            assertEquals(
                    "global config page let us edit it",
                    "c:\\temp",
                    ThinBackupPluginImpl.get().getBackupPath());
            assertEquals(
                    "global config page let us edit it",
                    10,
                    ThinBackupPluginImpl.get().getNrMaxStoredFull());
        });
        sessions.then(r -> {
            assertEquals(
                    "still there after restart of Jenkins",
                    "c:\\temp",
                    ThinBackupPluginImpl.get().getBackupPath());
            assertEquals(
                    "global config page let us edit it",
                    10,
                    ThinBackupPluginImpl.get().getNrMaxStoredFull());
        });
    }
}
