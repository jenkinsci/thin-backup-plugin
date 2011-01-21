/*
 * The MIT License
 *
 * Copyright (c) 2011, Borland (a Micro Focus Company), Matthias Steinkogler, Thomas Fuerer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
