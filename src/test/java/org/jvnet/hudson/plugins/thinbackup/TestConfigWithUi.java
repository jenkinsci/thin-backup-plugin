package org.jvnet.hudson.plugins.thinbackup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlNumberInput;
import org.htmlunit.html.HtmlTextInput;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class TestConfigWithUi {

    /**
     * Sets backupPath and max stored full backups and checks that it survives the next visit.
     */
    @Test
    void uiAndStorage(JenkinsRule r) throws Throwable {
        assertEquals("", ThinBackupPluginImpl.get().getBackupPath());
        assertEquals(-1, ThinBackupPluginImpl.get().getNrMaxStoredFull());
        final HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");

        HtmlTextInput textbox = config.getInputByName("_.backupPath");
        HtmlNumberInput numberInput = config.getInputByName("_.nrMaxStoredFull");
        textbox.setText("c:\\temp");
        numberInput.setText("10");
        r.submit(config);
        assertEquals("c:\\temp", ThinBackupPluginImpl.get().getBackupPath(), "global config page let us edit it");
        assertEquals(10, ThinBackupPluginImpl.get().getNrMaxStoredFull(), "global config page let us edit it");

        r.restart();

        assertEquals("c:\\temp", ThinBackupPluginImpl.get().getBackupPath(), "still there after restart of Jenkins");
        assertEquals(10, ThinBackupPluginImpl.get().getNrMaxStoredFull(), "global config page let us edit it");
    }
}
