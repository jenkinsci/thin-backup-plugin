package org.jvnet.hudson.plugins.thinbackup.backup;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPluginList {

  private PluginList pluginList;

  @Before
  public void setup() throws IOException {
    final File pluginsXml = File.createTempFile("pluginList", ".xml");
    pluginList = new PluginList(pluginsXml);
    pluginList.add("default", "0.1");
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testAdd() throws Exception {
    Assert.assertEquals(1, pluginList.getPlugins().size());
    pluginList.add("plugin", "0.1");
    Assert.assertEquals(2, pluginList.getPlugins().size());
  }

  @Test
  public void testCompareToEqualPluginList() throws IOException {
    final File pluginsXml2 = File.createTempFile("pluginList2", ".xml");
    final PluginList pluginList2 = new PluginList(pluginsXml2);
    pluginList2.add("default", "0.1");

    Assert.assertEquals(0, pluginList.compareTo(pluginList2));
    Assert.assertEquals(0, pluginList2.compareTo(pluginList));

  }

  @Test
  public void testCompareToNotEqualPluginList() throws IOException {
    final File pluginsXml2 = File.createTempFile("pluginList2", ".xml");
    final PluginList pluginList2 = new PluginList(pluginsXml2);

    Assert.assertEquals(-1, pluginList.compareTo(pluginList2));
    Assert.assertEquals(-1, pluginList2.compareTo(pluginList));

    pluginList2.add("default", "0.2");

    Assert.assertEquals(-1, pluginList.compareTo(pluginList2));
    Assert.assertEquals(-1, pluginList2.compareTo(pluginList));
  }

  @Test
  public void testCompareToDifferentSecondPluginList() throws IOException {
    final File pluginsXml2 = File.createTempFile("pluginList2", ".xml");
    final PluginList pluginList2 = new PluginList(pluginsXml2);

    pluginList2.add("hudson", "0.2");

    Assert.assertEquals(-1, pluginList.compareTo(pluginList2));
    Assert.assertEquals(-1, pluginList2.compareTo(pluginList));
  }

}
