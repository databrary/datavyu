/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.datavyu.plugins.qtkitplayer;

import com.google.common.collect.Lists;
import com.sun.jna.Platform;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.datavyu.Datavyu;
import org.datavyu.models.Identifier;
import org.datavyu.plugins.StreamViewer;
import org.datavyu.plugins.Filter;
import org.datavyu.plugins.FilterNames;
import org.datavyu.plugins.Plugin;
import org.datavyu.util.VersionRange;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.UUID;


public final class QtKitPlugin implements Plugin {

    private static final List<Datavyu.Platform> VALID_OPERATING_SYSTEMS = Lists.newArrayList(Datavyu.Platform.MAC);

    private static final UUID pluginUUID = UUID.nameUUIDFromBytes("plugin.qtkit".getBytes());

    private static final Filter VIDEO_FILTER = new Filter() {
        final SuffixFileFilter ff;
        final List<String> ext;

        {
            ext = Lists.newArrayList(".avi", ".mov", ".mpg", ".mp4");
            ff = new SuffixFileFilter(ext, IOCase.INSENSITIVE);
        }

        @Override
        public FileFilter getFileFilter() {
            return ff;
        }

        @Override
        public String getName() {
            return FilterNames.VIDEO.getFilterName();
        }

        @Override
        public Iterable<String> getExtensions() {
            return ext;
        }
    };

    @Override
    public StreamViewer getNewStreamViewer(final Identifier identifier, final File sourceFile, final Frame parent,
                                           final boolean modal) {
        if (Platform.isMac() || Platform.isWindows()) {
            return new QtKitViewerDialog(identifier, sourceFile, parent, modal);
        } else {
            return null;
        }
    }

    /**
     * @return icon representing this plugin.
     */
    @Override
    public ImageIcon getTypeIcon() {
        return new ImageIcon(getClass().getResource("/icons/gstreamerplugin-icon.png"));
    }

    @Override
    public String getNamespace() {
        return "datavyu.video";
    }

    @Override
    public UUID getPluginUUID() {return pluginUUID; }

    @Override
    public Filter[] getFilters() {
        return new Filter[]{VIDEO_FILTER};
    }

    @Override
    public String getPluginName() {
        return "QTKit Video";
    }

    @Override
    public Class<? extends StreamViewer> getViewerClass() {
        return Platform.isMac() ? QtKitViewerDialog.class : null;
    }

    @Override
    public List<Datavyu.Platform> getValidPlatforms() {
        return VALID_OPERATING_SYSTEMS;
    }

    @Override
    public VersionRange getValidVersions() {
        return new VersionRange(0, 10); // Start with OS version 10, go to 99 for future
    }
}
