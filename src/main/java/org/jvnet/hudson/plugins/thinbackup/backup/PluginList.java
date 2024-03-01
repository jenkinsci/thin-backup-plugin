/*
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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.XmlFile;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

public class PluginList implements Comparable<PluginList> {
    private Map<String, String> plugins;
    private final File pluginsXml;

    public PluginList(final File pluginsXml) {
        this.pluginsXml = pluginsXml;
        plugins = new HashMap<>();
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
        new XmlFile(Jenkins.XSTREAM, pluginsXml).write(this);
    }

    public void load() throws IOException {
        final XmlFile xmlFile = new XmlFile(Jenkins.XSTREAM, pluginsXml);
        if (xmlFile.exists()) {
            xmlFile.unmarshal(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PluginList that = (PluginList) o;
        return Objects.equals(plugins, that.plugins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plugins);
    }

    @Override
    public int compareTo(@NonNull final PluginList other) {
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
