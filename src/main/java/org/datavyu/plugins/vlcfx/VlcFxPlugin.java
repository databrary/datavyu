package org.datavyu.plugins.vlcfx;

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


public class VlcFxPlugin implements Plugin {

    private static final List<Datavyu.Platform> VALID_OPERATING_SYSTEMS = Lists.newArrayList(Datavyu.Platform.WINDOWS,
            Datavyu.Platform.MAC, Datavyu.Platform.LINUX);

    private static final UUID pluginUUID = UUID.nameUUIDFromBytes("plugin.vlcfx".getBytes());

    private static final Filter VIDEO_FILTER = new Filter() {
        final SuffixFileFilter ff;
        final List<String> ext;

        {
            ext = Lists.newArrayList(".avi", ".mov", ".mpg", ".mp4", ".mkv", ".m4v");
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

    private static final Filter[] FILTERS = new Filter[]{VIDEO_FILTER};

    @Override
    public String getNamespace() {
        return "datavyu.video";
    }

    @Override
    public Filter[] getFilters() {
        return FILTERS;
    }

    @Override
    public StreamViewer getNewStreamViewer(final Identifier identifier, final File sourceFile, final Frame parent,
                                           final boolean modal) {
        return new VlcFxDataViewerDialog(identifier, sourceFile, parent, modal);
    }

    @Override
    public String getPluginName() {
        return "VLCFX Video";
    }

    @Override
    public UUID getPluginUUID() {return pluginUUID; }

    @Override
    public ImageIcon getTypeIcon() {
        return new ImageIcon(getClass().getResource("/icons/vlc_cone.png"));
    }

    @Override
    public Class<? extends StreamViewer> getViewerClass() {
        return VlcFxDataViewerDialog.class;
    }

    @Override
    public List<Datavyu.Platform> getValidPlatforms() {
        return VALID_OPERATING_SYSTEMS;
    }
}
