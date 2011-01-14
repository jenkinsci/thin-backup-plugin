package org.jvnet.hudson.plugins.thinbackup.backup;

import hudson.XmlFile;
import hudson.model.Hudson;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

public class PluginList implements Comparable<PluginList> {
  private Map<String, String> plugins;
  transient private final File pluginsXml;

  public PluginList(final File pluginsXml) {
    this.pluginsXml = pluginsXml;
    plugins = new HashMap<String, String>();
  }

  public Map<String, String> getPlugins() {
    return plugins;
  }

  public void add(final String name, final String version) {
    plugins.put(name, version);
  }

  public void setPlugins(final Map<String, String> plugins) {
    this.plugins = plugins;
  }

  public void save() throws IOException {
    new XmlFile(Hudson.XSTREAM, pluginsXml).write(this);
  }

  public void load() throws IOException {
    final XmlFile xmlFile = new XmlFile(Hudson.XSTREAM, pluginsXml);
    if (xmlFile.exists()) {
      xmlFile.unmarshal(this);
    }
  }

  /**
   * @param other
   * @return -1 if <i>other</i> is different than <b>this</b>.
   */
  public int compareTo(final PluginList other) {
    if (other == null) {
      return -1;
    }

    final Map<String, String> plugins2 = other.getPlugins();

    if (plugins2.size() != plugins.size()) {
      return -1;
    }

    for (final Entry<String, String> entry : this.plugins.entrySet()) {
      final String plugin = entry.getKey();
      final String version = entry.getValue();
      final String prevVersion = plugins2.get(plugin);

      if (StringUtils.isEmpty(version) || StringUtils.isEmpty(prevVersion) || !version.equals(prevVersion)) {
        return -1;
      }
    }

    return 0;
  }
}
