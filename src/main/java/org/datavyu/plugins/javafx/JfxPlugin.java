package org.datavyu.plugins.javafx;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.datavyu.Datavyu;
import org.datavyu.models.Identifier;
import org.datavyu.plugins.StreamViewer;
import org.datavyu.plugins.Filter;
import org.datavyu.plugins.FilterNames;
import org.datavyu.plugins.Plugin;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.UUID;


public class JfxPlugin implements Plugin {

    private static final List<Datavyu.Platform> VALID_OPERATING_SYSTEMS = Lists.newArrayList(
            Datavyu.Platform.WINDOWS, Datavyu.Platform.MAC, Datavyu.Platform.LINUX);

    private static final UUID pluginUUID = UUID.nameUUIDFromBytes("plugin.jfx".getBytes());

    private static final Filter VIDEO_FILTER = new Filter() {
        final SuffixFileFilter ff;
        final List<String> ext;

        {
            ext = Lists.newArrayList(".mp4", ".m4v");
            ff = new SuffixFileFilter(ext, IOCase.INSENSITIVE);
        }

        @Override
        public FileFilter getFileFilter() {
            return ff;
        }

        @Override
        public String getName() {
            return FilterNames.MP4.getFilterName();
        }

        @Override
        public Iterable<String> getExtensions() {
            return ext;
        }
    };

    private static final Filter[] FILTERS = new Filter[]{VIDEO_FILTER};

    @Override
    public String getNamespace() {
        return "datavyu.video";
    }

    @Override
    public UUID getPluginUUID() {return pluginUUID; }

    @Override
    public Filter[] getFilters() {
        return FILTERS;
    }

    @Override
    public StreamViewer getNewStreamViewer(final Identifier identifier, final File sourceFile, final Frame parent,
                                           boolean modal) {
        return new JfxStreamViewer(identifier, sourceFile, parent, modal);
    }

    @Override
    public String getPluginName() {
        return "JavaFX Video";
    }

    @Override
    public ImageIcon getTypeIcon() {
        return new ImageIcon(getClass().getResource("/icons/JavaFX-Icon32.png"));
    }

    @Override
    public Class<? extends StreamViewer> getViewerClass() {
        return JfxStreamViewer.class;
    }

    @Override
    public List<Datavyu.Platform> getValidPlatforms() {
        return VALID_OPERATING_SYSTEMS;
    }
}
