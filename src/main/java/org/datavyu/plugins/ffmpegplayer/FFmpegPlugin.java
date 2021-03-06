package org.datavyu.plugins.ffmpegplayer;

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

public class FFmpegPlugin implements Plugin {

    private static final List<Datavyu.Platform> validOperatingSystem = Lists.newArrayList(Datavyu.Platform.WINDOWS,
                                                                Datavyu.Platform.MAC);

    private static final UUID pluginUUID = UUID.nameUUIDFromBytes("plugin.ffmpeg".getBytes());

    private static final Filter videoFilter = new Filter() {
        final List<String> fileEndings = Lists.newArrayList(".avi", ".mov", ".mpg", ".mp4");
        final SuffixFileFilter fileFilter = new SuffixFileFilter(fileEndings, IOCase.INSENSITIVE);

        @Override
        public FileFilter getFileFilter() {
            return fileFilter;
        }

        @Override
        public String getName() {
            return FilterNames.VIDEO.getFilterName();
        }

        @Override
        public Iterable<String> getExtensions() {
            return fileEndings;
        }
    };

    @Override
    public StreamViewer getNewStreamViewer(final Identifier identifier, final File sourceFile, final Frame parent,
                                           final boolean modal) {
        return new FFmpegStreamViewer(identifier, sourceFile, parent, modal);
    }

    @Override
    public Class<? extends StreamViewer> getViewerClass() {
        return FFmpegStreamViewer.class;
    }

    @Override
    public ImageIcon getTypeIcon() {
        return new ImageIcon(getClass().getResource("/icons/ffmpeg.png"));
    }

    @Override
    public String getPluginName() {
        return "FFmpeg Plugin (Experimental)";
    }

    @Override
    public UUID getPluginUUID() {return pluginUUID; }

    @Override
    public String getNamespace() {
        return "datavyu.video";
    }

    @Override
    public Filter[] getFilters() {
        return new Filter[]{videoFilter};
    }

    @Override
    public List<Datavyu.Platform> getValidPlatforms() {
        return validOperatingSystem;
    }
}
