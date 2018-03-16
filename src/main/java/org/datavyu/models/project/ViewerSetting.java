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
package org.datavyu.models.project;

import com.sun.istack.internal.localization.NullLocalizable;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.plugins.Plugin;
import org.datavyu.plugins.PluginManager;

import java.io.*;
import java.util.UUID;


/**
 * Stores user settings for a data viewer
 */
// TODO: Can we remove / replace this class by using the Swing Application Framework?
public final class ViewerSetting {

    /** Logger for this class */
    private static Logger logger = LogManager.getLogger(ViewerSetting.class);

    /** Version number */
    // TODO: Tie this back to the overall version numbers!! This is a design mistake.
    public static final int VERSION = 3; // May change it to 4

    /** Track settings associated with this data viewer */
    private TrackSettings trackSettings;

    /** Fully qualified name of the plugin */
    private String pluginName;

    /** Fully qualified UUID of the plugin */
    private UUID pluginUUID;

    /** Plugin classifier */
    // TODO: Consider refactoring this name. I remember this has to do with the file name matching to plugin but unused.
    private String pluginClassifier;

    /** Absolute file path to the data source */
    private String filePath;

    /** Playback offset in milliseconds */
    private long offset;

    /** Identifier of settings file */
    private String settingsId;

    /** The settings data */
    private byte[] settingsData;

    /** Output stream */
    private ByteArrayOutputStream settingsOutput;
    public ViewerSetting() { }

    /**
     * Copy constructor
     *
     * @param viewerSetting
     */
    private ViewerSetting(final ViewerSetting viewerSetting) {
        trackSettings = viewerSetting.trackSettings.copy();
        pluginName = viewerSetting.pluginName;
        pluginUUID = viewerSetting.pluginUUID;
        pluginClassifier = viewerSetting.pluginClassifier;
        filePath = viewerSetting.filePath;
        offset = viewerSetting.offset;
        settingsId = viewerSetting.settingsId;
    }

    /**
     * @return track settings associated with this data viewer.
     */
    public TrackSettings getTrackSettings() {
        return trackSettings;
    }

    /**
     * @param trackSettings track settings used by this data viewer.
     */
    public void setTrackSettings(final TrackSettings trackSettings) {
        this.trackSettings = trackSettings;
    }

    /**
     * @return Absolute file path to the data source.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @param filePath Absolute file path to the data source.
     */
    public void setFilePath(final String filePath) {
        this.filePath = filePath;
    }

    /**
     * Retained for backwards compatibility.
     *
     * @return
     */
    @Deprecated
    public long getOffset() {
        return offset;
    }

    /**
     * Retained for backwards compatibility.
     *
     * @param offset
     */
    @Deprecated
    public void setOffset(final long offset) {
        this.offset = offset;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(final String pluginName) {
        this.pluginName = pluginName;
        if(pluginUUID == null){
            for(Plugin p : PluginManager.getInstance().getPlugins()){
                if(pluginName.equals(p.getViewerClass().getName())){
                    setPluginUUID(p.getPluginUUID());
                    break;
                }
            }
        }
    }

    public UUID getPluginUUID() { return pluginUUID;}

    public void setPluginUUID(final UUID pluginUUID) {
        this.pluginUUID =  pluginUUID;
        if(pluginName == null){
            for(Plugin p : PluginManager.getInstance().getPlugins()){
                if(pluginUUID.equals(pluginUUID)){
                    setPluginName(p.getViewerClass().getName());
                    break;
                }
            }
        }
    }

    /**
     * @return the pluginClassifier
     */
    public String getPluginClassifier() {
        return pluginClassifier;
    }

    /**
     * @param pluginClassifier the pluginClassifier to set
     */
    public void setPluginClassifier(final String pluginClassifier) {
        this.pluginClassifier = pluginClassifier;
    }

    /**
     * @return String identifier for these settings.
     */
    public String getSettingsId() {
        return settingsId;
    }

    /**
     * Set the identifier for these settings.
     *
     * @param settingsId Identifier to use.
     */
    public void setSettingsId(final String settingsId) {
        this.settingsId = settingsId;
    }

    /**
     * Copy viewer settings from the given input stream into an internal buffer.
     * The settings can be read using {@link #getSettingsInputStream()}.
     *
     * @param is InputStream to copy from.
     */
    public void copySettings(final InputStream is) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            IOUtils.copy(is, os);
            settingsData = os.toByteArray();
        } catch (IOException e) {
            logger.error("Failed to save the copy settings. Error: ", e);
        }
        IOUtils.closeQuietly(os);
    }

    /**
     * Write the viewer settings to the given output stream.
     *
     * @param os OutputStream to write to.
     * @throws IOException If there are problems writing to the given output
     *                     stream.
     */
    public void writeSettings(final OutputStream os) throws IOException {
        assert os != null;

        if(settingsOutput != null) settingsOutput.writeTo(os);
    }

    /**
     * @return InputStream to use for reading settings.
     */
    public InputStream getSettingsInputStream() {
        return new ByteArrayInputStream(settingsData);
    }

    /**
     * @return OutputStream to use for writing settings.
     */
    public OutputStream getSettingsOutputStream() {
        // Can't re-use the stream, need empty buffer.
        // TODO: Re-design this part. Don't give out the stream. Need to close the byte array output stream.
        settingsOutput = new ByteArrayOutputStream();
        return settingsOutput;
    }

    public ViewerSetting copy() {
        return new ViewerSetting(this);
    }
}
