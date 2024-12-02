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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JDialog;

import org.datavyu.models.Identifier;
import org.datavyu.views.component.DefaultTrackPainter;
import org.datavyu.views.component.TrackPainter;


/**
 * Interface that describes the methods to view a stream.
 */
public interface StreamViewer {

    /**
     * @return Identifier used to identify this viewer.
     */
    Identifier getIdentifier();

    /**
     * Retrieve the duration of the underlying stream.
     *
     * @return The duration in milliseconds.
     */
    long getDuration();

    /**
     * Retrieve the start time of the underlying stream.
     *
     * @return Get the offset for this stream viewer.
     */
    long getOffset();

    /**
     * Set the start time of the underlying stream.
     *
     * @param offset Offset in milliseconds.
     */
    void setOffset(final long offset);

    /**
     * Get the display window.
     *
     * @return A JDialog that will be displayed.
     */
    JDialog getParentJDialog();

    /**
     * Hides or shows the windows associated with this data viewer.
     */
    void setViewerVisible(boolean isVisible);

    /**
     * Return the file of the data.
     *
     * @return The data feed being used by this viewer.
     */
    File getSourceFile();

    /**
     * @return Currently stored frames per second.
     */
    float getFramesPerSecond();
    
    /**
     * @param framesPerSecond frame rate to assign
     */
    void setFramesPerSecond(float framesPerSecond);

    /**
     * @return The current time in milliseconds, or -1 if player PTS is NaN.
     */
    long getCurrentTime();

    /**
     * Plays the stream at the set rate
     */
    void start();

    /**
     * Stops the play back of the data stream
     */
    void stop();

    /**
     * Pauses the play back of the data stream
     */
    void pause();

    /**
     * Steps forward
     */
    void stepForward();

    /**
     * Steps backward
     */
    void stepBackward();

    /**
     * Does this viewer play?
     */
    boolean isPlaying();

    /**
     * Set rate
     *
     * @param speed Positive implies forwards, while negative implies reverse.
     */
    void setRate(float speed);

    /**
     * Get the playback rate.
     *
     * @return playback rate.
     */
    float getRate();

    /**
     * Set the stream time. If the time is out of range set to the next closest value in the stream.
     *
     * @param time Position in milliseconds.
     */
    void setCurrentTime(long time);

    /**
     * @return Custom track painter implementation. Must not return null.
     * Plugins that do not have a custom track painter implementation should
     * return {@link DefaultTrackPainter}.
     */
    TrackPainter getTrackPainter();

    /**
     * Read settings from the given input stream.
     *
     * @param is Input stream to load from.
     */
    void loadSettings(InputStream is);

    /**
     * Write settings to the given output stream.
     *
     * @param os Output stream to write to.
     */
    void storeSettings(OutputStream os);

    /**
     * Adds the given ViewerStateListener to the collection of listeners who
     * are interested in changes made to the project.
     * @param vsl The ViewerStateListener to add.
     */
    void addViewerStateListener(ViewerStateListener vsl);

    /**
     * Removes the given ViewerStateListener from the collection of listeners.
     * @param vsl The listener to remove.
     */
    void removeViewerStateListener(ViewerStateListener vsl);

    /**
     * Used to query the viewer for custom actions.
     *
     * @return custom actions.
     * @see CustomActionsAdapter
     */
    CustomActions getCustomActions();

    /**
     * Unload all data, to prepare for being closed. Essentially the opposite
     * of setSourceFile. Call to reduce the data viewer to a low-resource state.
     */
    void close();

    /**
     * Did we assume the frames per second start back rate?
     *
     * @return True if we assumed the playback rate; otherwise false.
     */
    boolean isAssumedFramesPerSecond();

    boolean isStepEnabled();

    boolean isSeekPlaybackEnabled();
}
