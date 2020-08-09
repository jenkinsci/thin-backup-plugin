package org.jvnet.hudson.plugins.thinbackup.utils;

import java.io.File;
import java.io.Serializable;

import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

public class ExistsAndReadableFileFilter extends AbstractFileFilter implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Wraps the supplied filter to make if safe against broken symlinks and missing read permissions
     * @param filter The core filter that'll be wrapped inside safety checks
     * @return The wrapped file filter
     */
    public static IOFileFilter wrapperFilter(final IOFileFilter filter) {
      return FileFilterUtils.asFileFilter((dir, name) -> {
        File file = new File(dir, name);
        if (file.exists() && file.canRead()) {
          return filter.accept(file);
            }
        return false;
        });
    }
}