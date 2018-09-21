/*
 * Copyright (c) 2011 OpenSHAPA Foundation, http://openshapa.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.datavyu.plugins;

import org.datavyu.Datavyu;
import org.datavyu.models.Identifier;
import org.datavyu.util.VersionRange;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.UUID;


/**
 * Plugin interface - specifies the methods an OpenSHAPA plugin must implement.
 */
public interface Plugin {

    /**
     * @return A new instance of the plugins data viewer.
     */
    StreamViewer getNewStreamViewer(final Identifier identifier, final File sourceFile, final Frame parent,
                                    boolean modal);

    /**
     * @return The data viewer class.
     */
    Class<? extends StreamViewer> getViewerClass();

    /**
     * @return The icon representing this plugin. This can return null if
     *         this plugin has no icon representing its type.
     */
    ImageIcon getTypeIcon();

    /**
     * @return Plugin name.
     */
    String getPluginName();

    /**
     * @return Plugin short name (without 'plugin.' prefix)
     */
    String getShortName();

    /**
     * @return Plugin UUID.
     */
    UUID getPluginUUID();

    /**
     *<p>A classifier string is a string representing the class of your plugin
     * as part of some namespace. An example of such a string could be
     * "companyfoo.video", which a person could interpret as "a plugin by
     * company foo which handles video". OpenSHAPA will not attempt to
     * interpret the string.</p>
     *<p>This classifier is used to group plugins with the same or similar
     * capabilities together. This grouping is used to select a backup or
     * alternative plugin (with the same classifier and file type handling) in
     * the case where the plugin used to visualize some data is not installed.
     *</p>
     *<p>The classifier string cannot be null or the empty string.</p>
     * @return Classifier string.
     */
    String getNamespace();

    /**
     * @return The various _groups_ of files that this plugin supports. As an
     * example, a plugin might be able to open both video and audio files.
     * The plugin should then return two filters: one for video files, another
     * for audio files.
     */
    Filter[] getFilters();

    List<Datavyu.Platform> getValidPlatforms();

    default VersionRange getValidVersions() {
        return new VersionRange(0,99);
    }
}
