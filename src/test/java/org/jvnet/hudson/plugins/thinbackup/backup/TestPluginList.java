/**
 *  Copyright (C) 2011  Matthias Steinkogler, Thomas Fürer
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses.
 */
package org.jvnet.hudson.plugins.thinbackup.backup;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

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
