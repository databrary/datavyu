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
package org.datavyu.views;

import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.Datavyu;
import org.datavyu.Datavyu.Platform;
import org.datavyu.controllers.CreateNewCellController;
import org.datavyu.controllers.SetNewCellStopTimeController;
import org.datavyu.controllers.SetSelectedCellStartTimeController;
import org.datavyu.controllers.SetSelectedCellStopTimeController;
import org.datavyu.controllers.component.MixerController;
import org.datavyu.event.component.CarriageEvent;
import org.datavyu.event.component.TimescaleEvent;
import org.datavyu.event.component.TracksControllerEvent;
import org.datavyu.event.component.TracksControllerListener;
import org.datavyu.models.Identifier;
import org.datavyu.models.PlaybackModel;
import org.datavyu.models.component.*;
import org.datavyu.plugins.StreamViewer;
import org.datavyu.plugins.Plugin;
import org.datavyu.plugins.PluginManager;
import org.datavyu.plugins.quicktime.QTPlugin;
import org.datavyu.util.ClockTimer;
import org.datavyu.util.ClockTimer.ClockListener;
import org.datavyu.util.FloatingPointUtils;
import org.datavyu.util.MacOS;
import org.datavyu.util.WindowsOS;
import org.datavyu.views.component.TrackPainter;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Video controller
 */
public final class VideoController extends DatavyuDialog
        implements ClockListener, TracksControllerListener, PropertyChangeListener {

    /** The logger for this class */
    private static Logger logger = LogManager.getLogger(VideoController.class);

    /** One second in milliseconds */
    private static final long ONE_SECOND = 1000L;

    /** Rate of playback for rewinding */
    private static final float REWIND_RATE = -32F;

    /** Rate of normal playback */
    private static final float PLAY_RATE = 1F;

    /** Rate of playback for fast forwarding */
    private static final float FAST_FORWARD_RATE = 32F;

    /** How often to synchronise the streamViewers with the master clock */
    private static final long SYNC_PULSE = 500;

    /** The jump multiplier for shift-jogging */
    private static final int SHIFT_JOG = 5;

    /** The jump multiplier for ctrl-jogging */
    private static final int CTRL_JOG = 10;

    /** The 45x45 size for number pad keys */
    private static final int NUM_PAD_KEY_HEIGHT = 45;

    private static final int NUM_PAD_KEY_WIDTH = 45;

    private static final String NUM_PAD_KEY_SIZE = "w " + NUM_PAD_KEY_HEIGHT + "!, h " + NUM_PAD_KEY_WIDTH + "!";

    /** The 45x95 size for tall numpad keys (enter, PC plus) */
    private static final int TALL_NUM_PAD_KEY_HEIGHT = 95;

    private static final String TALL_NUM_PAD_KEY_SIZE = "span 1 2, w " + NUM_PAD_KEY_WIDTH + "!, h "
            + TALL_NUM_PAD_KEY_HEIGHT + "!";

    /** The 80x40 size for the text fields to the right of numpad */
    private static final int WIDE_TEXT_FIELD_WIDTH = 90;

    private static final int WIDE_TEXT_FIELD_HEIGHT = 45;

    private static final String WIDE_TEXT_FIELD_SIZE = "w " + WIDE_TEXT_FIELD_WIDTH + "!, h "
            + WIDE_TEXT_FIELD_HEIGHT + "!";

    private static final Font TEXT_FIELD_FONT = new Font("Arial", Font.PLAIN, 10);

    private static final Font TEXT_LABEL_FONT = new Font("Arial", Font.PLAIN, 10);

    /** Format for time */
    private static final DateFormat CLOCK_FORMAT;

    private static final DateFormat CLOCK_FORMAT_HTML;

    private static int timeStampFontSize = 15;

    // initialize standard date format for clock display.
    static {
        CLOCK_FORMAT = new SimpleDateFormat("HH:mm:ss:SSS");
        CLOCK_FORMAT.setTimeZone(new SimpleTimeZone(0, "NO_ZONE"));

        Color hoursColor = TimescaleConstants.HOURS_COLOR;
        Color minutesColor = TimescaleConstants.MINUTES_COLOR;
        Color secondsColor = TimescaleConstants.SECONDS_COLOR;
        Color millisecondsColor = TimescaleConstants.MILLISECONDS_COLOR;

        CLOCK_FORMAT_HTML = new SimpleDateFormat("'<html>" + "<font color=\""
                + toRGBString(hoursColor) + "\">'HH'</font>':"
                + "'<font color=\"" + toRGBString(minutesColor)
                + "\">'mm'</font>':" + "'<font color=\""
                + toRGBString(secondsColor) + "\">'ss'</font>':"
                + "'<font color=\"" + toRGBString(millisecondsColor)
                + "\">'SSS'</font>" + "</html>'");
        CLOCK_FORMAT_HTML.setTimeZone(new SimpleTimeZone(0, "NO_ZONE"));
    }

    // Initialize SHUTTLE_RATES, for 0 players stop
    private static float[] SHUTTLE_RATES = new float[]{
            -32F, -16F, -8F, -4F, -2F, -1F, -1/2F, -1/4F, -1/8F, -1/16F, -1/32F,
                0F, 1/32F, 1/16F, 1/8F, 1/4F, 1/2F, 1F, 2F, 4F, 8F, 16F, 32F};

    /**
     * // TODO: What is this doing?
     *
     * Visible?
     */
    private boolean visible = false;

    /** Determines whether or not the 'shift' key is being held */
    private boolean shiftMask = false;

    /** Determines whether or not 'control' key is being held */
    private boolean ctrlMask = false;

    /** The list of streamViewers associated with this controller */
    private Set<StreamViewer> streamViewers;

    /** Clock timer */
    private ClockTimer clock = new ClockTimer();

    /** Is the tracks panel currently shown */
    private boolean tracksPanelVisible = true;

    /** The controller for manipulating tracks */
    private MixerController mixerController;

    /** Button to create a new cell */
    private javax.swing.JButton createNewCell;

    /** Button to create a new cell setting offset */
    private javax.swing.JButton createNewCellSettingOffset;

    /** */
    private javax.swing.JButton findButton;

    /** */
    private javax.swing.JTextField offsetTextField;

    /** */
    private javax.swing.JTextField onsetTextField;

    /** */
    private javax.swing.JButton goBackButton;

    /** */
    private javax.swing.JTextField goBackTextField;

    /** */
    private javax.swing.JTextField stepSizeTextField;

    /** */
    private javax.swing.JLabel stepSizeLabel = new JLabel("Steps per second");

    /** */
    private javax.swing.JPanel stepSizePanel;

    /** */
    private javax.swing.JPanel gridButtonPanel;

    /** */
    private javax.swing.JLabel labelSpeed;

    /** */
    private javax.swing.JButton pauseButton;

    /** */
    private javax.swing.JButton playButton;

    /** */
    private javax.swing.JButton nineSetCellOffsetButton;

    /** */
    private javax.swing.JButton setCellOffsetButton;

    /** */
    private javax.swing.JButton setCellOnsetButton;

    /** */
    private javax.swing.JButton pointCellButton;

    /** */
    private javax.swing.JButton showTracksSmallButton;

    /** */
    private javax.swing.JButton shuttleBackButton;

    /** */
    private javax.swing.JButton shuttleForwardButton;

    /** */
    private javax.swing.JButton stopButton;

    /** */
    private javax.swing.JLabel timeStampLabel;

    /** */
    private javax.swing.JPanel tracksPanel;

    /** Model containing playback information */
    private PlaybackModel playbackModel;

    private boolean qtWarningShown = false;

    private org.jdesktop.application.ResourceMap resourceMap;

    private javax.swing.ActionMap actionMap;

    private String osModifier;

    private boolean highlightCells = false;

    private boolean highlightAndFocus = false;

    /**
     * Create a new VideoController.
     *
     * @param parent The parent of this form.
     * @param modal  Should the dialog be modal or not?
     */
    public VideoController(final java.awt.Frame parent, final boolean modal) {
        super(parent, modal);

        clock.registerListener(this);

        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        resourceMap = Application.getInstance(org.datavyu.Datavyu.class)
                                        .getContext().getResourceMap(VideoController.class);

        actionMap = Application.getInstance(org.datavyu.Datavyu.class)
                                    .getContext().getActionMap(VideoController.class, this);

        if (Datavyu.getPlatform() == Platform.MAC) {
            osModifier = "osx";
            setJMenuBar(((JFrame) parent).getJMenuBar());
        } else {
            osModifier = "win";
        }
        initComponents();

        setResizable(false);
        setName(this.getClass().getSimpleName());
        streamViewers = new LinkedHashSet<>();

        playbackModel = new PlaybackModel();
        playbackModel.setPauseRate(0);
        playbackModel.setLastSyncTime(0);
        playbackModel.setMaxDuration(ViewportStateImpl.MINIMUM_MAX_END);

        final int defaultEndTime = (int) MixerController.DEFAULT_DURATION;

        playbackModel.setStartTime(0);
        playbackModel.setEndTime(defaultEndTime);

        mixerController = new MixerController();
        tracksPanel.add(mixerController.getTracksPanel(), "growx");
        mixerController.addTracksControllerListener(this);
        mixerController.getMixerModel().getViewportModel().addPropertyChangeListener(this);
        mixerController.getMixerModel().getRegionModel().addPropertyChangeListener(this);
        mixerController.getMixerModel().getNeedleModel().addPropertyChangeListener(this);

        showTracksPanel(tracksPanelVisible);
        updateCurrentTimeLabel();

        visible = true;
    }

    /**
     * TODO: Put to some time utils?
     *
     * @param time
     * @return
     */
    public static String formatTime(final long time) {
        return CLOCK_FORMAT.format(new Date(time));
    }

    /**
     * TODO: Put to some color utils?
     *
     * @param color
     * @return
     */
    private static String toRGBString(final Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Handles opening a data source.
     *
     * @param chooser The plugin chooser used to open the data source.
     */
    private void openVideo(final PluginChooser chooser) {
        final Plugin plugin = chooser.getSelectedPlugin();
        final File selectedFile = chooser.getSelectedFile();

        new Thread(() -> {
            if (plugin != null) {
                try {
                    StreamViewer streamViewer = plugin.getNewStreamViewer(Datavyu.getApplication().getMainFrame(),
                            false);
                    Identifier newIdentifier = Identifier.generateIdentifier();
                    streamViewer.setIdentifier(newIdentifier);
                    streamViewer.setSourceFile(selectedFile);
                    streamViewer.seek(clock.getTime());
                    // Fake playback is not available for the ffmpeg plugin
                    streamViewer.setEnableFakePlayback( !"FFmpeg Plugin".equals(plugin.getPluginName()) );
                    addStreamViewer(plugin.getTypeIcon(), streamViewer, selectedFile, streamViewer.getTrackPainter());
                    mixerController.bindTrackActions(newIdentifier, streamViewer.getCustomActions());
                    streamViewer.addViewerStateListener(mixerController.getTracksEditorController()
                                    .getViewerStateListener(newIdentifier));
                } catch (Throwable t) {
                    logger.error(t);

                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    t.printStackTrace(pw);
                    // stack trace as a string
                    if (plugin.getNamespace().contains("quicktime")) {
                        JLabel label = new JLabel();
                        Font font = label.getFont();

                        // create some css from the label's font
                        StringBuilder style = new StringBuilder("font-family:").append(font.getFamily()).append(";");
                        style.append("font-weight:").append(font.isBold() ? "bold" : "normal").append(";");
                        style.append("font-size:").append(font.getSize()).append("pt;");

                        // html content
                        JEditorPane ep = new JEditorPane("text/html", "<html><body style=\"" + style + "\">" //
                                + "Error: Could not load Quicktime.  <a href=\"https://support.apple.com/kb/DL1822?locale=en_US\">Please install Quicktime 7.7.6 from here</a><br>" +
                                "and when installing, select \"Custom Install\" and then left click on the [+] next to the red X<br>" +
                                "next to \"Legacy options\", click on the red X next to Quicktime For Java and select <br>\"Will be installed to local harddrive\". Then click \"Next\" and install.<br>" +
                                "Afterwards relaunch Datavyu to use the Quicktime plugin." //
                                + "</body></html>");

                        // handle link events
                        ep.addHyperlinkListener(new HyperlinkListener() {
                            @Override
                            public void hyperlinkUpdate(HyperlinkEvent evt) {
                                if (evt.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                                    try {
                                        // roll your own link launcher or use Desktop if J6+
                                        Desktop.getDesktop().browse(evt.getURL().toURI());
                                    } catch (Exception e) {
                                        logger.error("Error when opening hyper text " + e);
                                    }
                                }
                            }
                        });
                        ep.setEditable(false);
                        ep.setBackground(label.getBackground());

                        // show
                        JOptionPane.showMessageDialog(null, ep);
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "This plugin could not load this file. Error message:\n" + t.getMessage());
                    }
                }
            }
        }).start();
    }

    /**
     * Tells the Data Controller if shift is being held or not.
     *
     * @param shift True for shift held; false otherwise.
     */
    public void setShiftMask(final boolean shift) {
        shiftMask = shift;
    }

    /**
     * Tells the Data Controller if ctrl is being held or not.
     *
     * @param ctrl True for ctrl held; false otherwise.
     */
    public void setCtrlMask(final boolean ctrl) {
        ctrlMask = ctrl;
    }

    /**
     * @param time Current clock time in milliseconds.
     */
    public void clockStart(final long time) {
        resetSync();

        long playTime = time;
        final long windowPlayStart = playbackModel.getStartTime();

        if (playTime < windowPlayStart) {
            playTime = windowPlayStart;
            clockStep(playTime);
        }

        float currentRate = clock.getRate();
        clock.stop();

        setCurrentTime(playTime);
        clock.setTime(playTime);
        clock.setRate(currentRate);

        clock.start();
    }

    /**
     * Reset the sync.
     */
    private void resetSync() {
        playbackModel.setLastSyncTime(0);
    }

    /**
     * Keep track of whether this should be visible or not
     */
    public boolean shouldBeVisible() {
        return visible;
    }

    public void setShouldBeVisible(boolean v) {
        visible = v;
    }

    /**
     * @param time Current clock time in milliseconds.
     */
    public void clockTick(final long time) {
        try {
            setCurrentTime(time);

            for (StreamViewer streamViewer : streamViewers) {
                // Fake play back; rapid search of times
                if (streamViewer.isFakePlayback()) {
                    if ((time > streamViewer.getStartTime()) && isInPlayRange(time, streamViewer)) {
                        streamViewer.seek(time - streamViewer.getStartTime());
                    }
                } else {
                    // Synchronise streamViewers only if we have exceeded our pulse time.
                    if ((time - playbackModel.getLastSyncTime())
                            > (SYNC_PULSE * clock.getRate()) || playbackModel.getLastSyncTime() == 0) {
                        playbackModel.setLastSyncTime(time);

                        for (StreamViewer v : streamViewers) {
                        /*
                         * Use offsets to determine if the video file should
                         * start playing.
                         */
                            if (!v.isPlaying() && isInPlayRange(time, v)) {
                                v.play();
                            }

                            // BugzID:1797 - Viewers who are "playing" outside their
                            // timeframe should be asked to stop.
                            if (v.isPlaying() && !isInPlayRange(time, v)) {
                                v.stop();
                            }
                        }
                    }
                }
            }

            // BugzID:466 - Prevent rewind wrapping the clock past the start
            // point of the view window.
            final long windowPlayStart = playbackModel.getStartTime();

            if (time < windowPlayStart) {
                setCurrentTime(windowPlayStart);
                clock.stop();
                clockStop(windowPlayStart);
            }

            // BugzID:756 - don't play video once past the max duration.
            final long windowPlayEnd = playbackModel.getEndTime();

            if ((time >= windowPlayEnd) && (clock.getRate() >= 0)) {
                clock.stop();
                setCurrentTime(windowPlayEnd);
                clockStop(windowPlayEnd);
                clock.setTime(windowPlayEnd);
            }
        } catch (Exception e) {
            logger.error("Unable to Sync streamViewers " + e);
        }
    }

    /**
     * Determines whether a StreamViewer has data for the desired time.
     *
     * @param time The time we wish to play at or seek to.
     * @param view The StreamViewer to check.
     * @return True if data exists at this time, and false otherwise.
     */
    private boolean isInPlayRange(final long time, final StreamViewer view) {
        return (time >= view.getStartTime()) && (time < (view.getStartTime() + view.getDuration()));
    }

    /**
     * Method to sync the clock to the video.
     *
     * @param time
     */
    private void adjustClock(final long time) {
        if (streamViewers.size() == 1
                && (time < playbackModel.getEndTime() && time > playbackModel.getStartTime())) {
            // Using an iterator because streamViewers is a set
            for (StreamViewer viewer : streamViewers) {
                try {
                    long viewerTime = viewer.getCurrentTime();

                    long stepSize = ((ONE_SECOND) / (long) playbackModel.getCurrentFramesPerSecond());

                     /* BugzID:1544 - Preserve precision - force jog to frame markers. */
                    if (!clock.isStopped()) {
                        long mod = (viewerTime % stepSize);

                        if (mod != 0) {
                            viewerTime = viewerTime + stepSize - mod;
                        }
                    }

                    clock.setTimeWithoutNotify(viewerTime);
                    resetSync();
                    updateCurrentTimeLabel();
                } catch (Exception e) {
                    logger.error("Could not adjust clock. Error: " + e);
                }
            }
        }
    }

    /**
     * @param time Current clock time in milliseconds.
     */
    public void clockStop(final long time) {
        clock.stop();
        resetSync();

        /*
            This is the new style time reckoning where the timer gets updated from the video
         */
        if (streamViewers.size() == 1 && (time < playbackModel.getEndTime()
                && time > playbackModel.getStartTime())) {
            // Using an iterator because streamViewers is a set
            for (StreamViewer viewer : streamViewers) {
                if (viewer.isPlaying()) {
                    viewer.stop();
                    try {
                        long viewerTime = time;

                        long stepSize = ((ONE_SECOND) / (long) playbackModel.getCurrentFramesPerSecond());

                        /* BugzID:1544 - Preserve precision - force jog to frame markers. */
                        long mod = (viewerTime % stepSize);

                        if (mod != 0) {
                            viewerTime = viewerTime + stepSize - mod;
                        }

                        viewer.seek(viewerTime);

                        clock.setTimeWithoutNotify(viewerTime);
                        resetSync();
                        updateCurrentTimeLabel();
                    } catch (Exception e) {
                        logger.error("Error when stopping clock " + e);
                    }
                }
            }
        } else {
            setCurrentTime(time);
            for (StreamViewer viewer : streamViewers) {
                if (viewer.isPlaying()) {
                    viewer.stop();
                }
            }
        }
    }

    /**
     * @param rate Current (updated) clock rate.
     */
    public void clockRate(final float rate) {
        resetSync();
        labelSpeed.setText(FloatingPointUtils.doubleToFractionStr(rate));

        // If the rate is faster than 2x or slower than -1x, some viewers require a sequential seeks to play back
        // smoothly
        if (Math.abs(rate) > 2.0 || rate < -1) {
            long time = getCurrentTime();
            for (StreamViewer streamViewer : streamViewers) {
                if (streamViewer.isEnableFakePlayback()) {
                    // If the viewer requires fake playback use it
                    streamViewer.setFakePlayback(true);
                    streamViewer.stop();
                    if (isInPlayRange(time, streamViewer)) {
                        streamViewer.setPlaybackSpeed(rate);
                    }
                } else {
                    // No fake playback, just set the new playback speed
                    streamViewer.setPlaybackSpeed(rate);
                    if (!clock.isStopped()) {
                        streamViewer.play();
                    }
                }
            }

            // Use the
        } else {
            for (StreamViewer streamViewer : streamViewers) {
                // Reset fake playback
                streamViewer.setFakePlayback(false);
                // Set the playback speed
                streamViewer.setPlaybackSpeed(rate);
                // If we are not stopped play
                if (!clock.isStopped()) {
                    streamViewer.play();
                }
            }
        }
    }

    /**
     * @param time Current clock time in milliseconds.
     */
    public void clockStep(final long time) {
        resetSync();
        setCurrentTime(time);
        for (StreamViewer viewer : streamViewers) {
            try {
                if (isInPlayRange(time, viewer) && time != viewer.getCurrentTime()) {
                    viewer.seek(time - viewer.getStartTime());
                }
            } catch (Exception e) {
                logger.info("Error when stepping clock " + e);
            }
        }
    }

    /**
     * @return the mixer controller.
     */
    public MixerController getMixerController() {
        return mixerController;
    }

    private void updateCurrentTimeLabel() {
        timeStampLabel.setText(tracksPanelVisible ? CLOCK_FORMAT_HTML.format(getCurrentTime())
                                                  : CLOCK_FORMAT_HTML.format(getCurrentTime()));
    }

    /**
     * Get the current master clock time for the controller.
     *
     * @return Time in milliseconds.
     */
    public long getCurrentTime() {
        return clock.getTime();
    }

    /**
     * Set time location for data streams.
     *
     * @param milliseconds The millisecond time.
     */
    public void setCurrentTime(final long milliseconds) {
        resetSync();
        updateCurrentTimeLabel();
        mixerController.getMixerModel().getNeedleModel().setCurrentTime(milliseconds);
    }

    /**
     * Recalculates the maximum viewer duration.
     */
    public void updateMaxViewerDuration() {
        long maxDuration = ViewportStateImpl.MINIMUM_MAX_END;

        for (StreamViewer streamViewer : streamViewers) {
            if ((streamViewer.getDuration() + streamViewer.getStartTime()) > maxDuration) {
                maxDuration = streamViewer.getDuration() + streamViewer.getStartTime();
            }
        }

        mixerController.getMixerModel().getViewportModel().setViewportMaxEnd(maxDuration, true);

        if (streamViewers.isEmpty()) {
            mixerController.getNeedleController().resetNeedlePosition();
            mixerController.getMixerModel().getRegionModel()
                    .resetPlaybackRegion();
        }
    }

    /**
     * Remove the specified viewer from the controller.
     *
     * @param viewer The viewer to shutdown.
     * @return True if the controller contained this viewer.
     */
    public boolean shutdown(final StreamViewer viewer) {

        // Did we remove the viewer?
        boolean removed = streamViewers.remove(viewer);

        if (removed) {

            viewer.unsetSourceFile();

            // BugzID:2000
            viewer.removeViewerStateListener(
                    mixerController.getTracksEditorController()
                            .getViewerStateListener(viewer.getIdentifier()));

            // Recalculate the maximum playback duration.
            updateMaxViewerDuration();

            // Remove the data viewer from the tracks panel.
            mixerController.deregisterTrack(viewer.getIdentifier());

            // Data viewer removed, mark project as changed.
            Datavyu.getProjectController().projectChanged();
        }

        return removed;
    }

    /**
     * Get the stream viewer with the id.
     *
     * @param id The identifier for the stream viewer.
     *
     * @return A stream viewer if found otherwise null.
     */
    private StreamViewer getStreamViewer(final Identifier id) {
        for (StreamViewer streamViewer : streamViewers) {
            if (streamViewer.getIdentifier().equals(id)) {
                return streamViewer;
            }
        }
        return null;
    }

    /**
     * Remove the specified viewer from the controller.
     *
     * @param id The identifier of the viewer to shutdown.
     */
    public void shutdown(final Identifier id) {
        StreamViewer viewer = getStreamViewer(id);

        if ((viewer == null) || !shouldRemove()) {
            return;
        }

        streamViewers.remove(viewer);

        viewer.stop();
        viewer.unsetSourceFile();

        JDialog viewDialog = viewer.getParentJDialog();

        if (viewDialog != null) {
            viewDialog.dispose();
        }

        // DO SOMETHING CONCERNING STEP_SIZE
        if (streamViewers.isEmpty()) {
            playbackModel.setCurrentFramesPerSecond(0f);
            updateStepSizeTextField();
        }

        // BugzID:2000
        viewer.removeViewerStateListener(
                mixerController.getTracksEditorController().getViewerStateListener(
                        viewer.getIdentifier()));

        // Recalculate the maximum playback duration
        updateMaxViewerDuration();

        // Remove the data viewer from the tracks panel
        mixerController.deregisterTrack(viewer.getIdentifier());

        // Data viewer removed, mark project as changed
        Datavyu.getProjectController().projectChanged();
    }

    /**
     * Binds a window event listener to a data viewer.
     *
     * @param id The identifier of the viewer to bind to.
     */
    public void bindWindowListenerToStreamViewer(final Identifier id, final WindowListener windowListener) {
        StreamViewer viewer = getStreamViewer(id);
        if (viewer != null && viewer.getParentJDialog() != null) {
            viewer.getParentJDialog().addWindowListener(windowListener);
        }
    }

    /**
     * Binds a window event listener to a data viewer.
     *
     * @param id The identifier of the viewer to bind to.
     */
    public void setStreamViewerVisibility(final Identifier id, final boolean visible) {
        StreamViewer viewer = getStreamViewer(id);
        if (viewer != null) {
            viewer.setViewerVisible(visible);
        }
    }


    /**
     * Presents a confirmation dialog when removing a plugin from the project.
     *
     * @return True if the plugin should be removed, false otherwise.
     */
    private boolean shouldRemove() {
        ResourceMap rMap = Application.getInstance(Datavyu.class).getContext().getResourceMap(Datavyu.class);
        String defaultOption = "Cancel";
        String alternativeOption = "OK";
        String[] options = Datavyu.getPlatform() == Platform.MAC ? MacOS.getOptions(defaultOption, alternativeOption) :
                WindowsOS.getOptions(defaultOption, alternativeOption);
        int selectedOption = JOptionPane.showOptionDialog(this,
                rMap.getString("ClosePluginDialog.message"),
                rMap.getString("ClosePluginDialog.title"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, defaultOption);
        return (Datavyu.getPlatform() == Platform.MAC) ? (selectedOption == 1) : (selectedOption == 0);
    }

    /**
     * Helper method for Building a button for the data controller - sets the icon, selected icon, action map, and name.
     *
     * @param name Prefix when looking for actions and buttons.
     * @param modifier Modifier (if any) to apply to the prefix. Maybe null.
     * @return A configured button.
     */
    private JButton buildButton(final String name, final String modifier) {
        String dotModifier = (modifier == null) || modifier.isEmpty() ? "" : "." + modifier;
        JButton result = new JButton();
        result.setAction(actionMap.get(name + "Action"));
        result.setIcon(resourceMap.getIcon(name + "Button.icon" + dotModifier));
        result.setPressedIcon(resourceMap.getIcon(name + "SelectedButton.icon" + dotModifier));
        result.setFocusPainted(false);
        result.setName(name + "Button");
        return result;
    }

    private JButton buildButton(final String name) {
        return buildButton(name, null);
    }

    private JPanel makeLabelAndTextfieldPanel(JLabel label, JTextField textField) {
        JPanel jPanel = new JPanel();
        label.setFont(TEXT_LABEL_FONT);
        textField.setFont(TEXT_FIELD_FONT);
        jPanel.add(label);
        jPanel.add(textField);
        return jPanel;
    }

    /**
     * Helper method for creating placeholder buttons
     */
    private JButton makePlaceholderButton(boolean visible) {
        JButton jButton = new JButton();
        jButton.setEnabled(false);
        jButton.setFocusPainted(false);
        jButton.setVisible(visible);
        return jButton;
    }

    /**
     * Helper method for creating placeholder buttons
     */
    private JButton makePlaceholderButton() {
        return makePlaceholderButton(true);
    }

    /**
     * Initialize the view for OS other than Macs.
     */
    private void initComponents() {
        gridButtonPanel = new JPanel();
        goBackTextField = new JTextField();
        stepSizeTextField = new JTextField();
        onsetTextField = new JTextField();
        JButton addDataButton = new JButton();
        timeStampLabel = new JLabel();
        labelSpeed = new JLabel();
        createNewCell = new JButton();
        JLabel atLabel = new JLabel();
        JLabel xLabel = new JLabel();
        offsetTextField = new JTextField();
        tracksPanel = new JPanel(new MigLayout("fill"));

        final int fontSize = 11;

        setTitle(resourceMap.getString("title"));
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(
                    final java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        gridButtonPanel.setBackground(Color.WHITE);
        gridButtonPanel.setLayout(new MigLayout("wrap 5"));

        // Add data button
        addDataButton.setText(resourceMap.getString("addDataButton.text"));
        addDataButton.setFont(new Font("Tahoma", Font.PLAIN, fontSize));
        addDataButton.setFocusPainted(false);
        addDataButton.setName("addDataButton");
        addDataButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                if (!qtWarningShown && !QTPlugin.hasQuicktimeLibs()) {
                    qtWarningShown = true;
                }
                openVideoButtonActionPerformed(evt);
            }
        });
        gridButtonPanel.add(addDataButton, "span 2, w 90!, h 25!, wrap");

        // Timestamp panel
        JPanel timestampPanel = new JPanel(new MigLayout("","push[][][]0![]push"));
        timestampPanel.setOpaque(false);

        // Timestamp label
        timeStampLabel.setFont(new Font("Tahoma", Font.BOLD, timeStampFontSize));
        timeStampLabel.setHorizontalAlignment(SwingConstants.CENTER);
        timeStampLabel.setText("00:00:00:000");
        timeStampLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        timeStampLabel.setName("timeStampLabel");
        timeStampLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // you can open a new frame here as
                // i have assumed you have declared "frame" as instance variable
                if (e.getClickCount() >= 3) {
                    int newTime = Integer.parseInt(JOptionPane.showInputDialog(null,
                            "Enter new time in ms", getCurrentTime()));
                    setCurrentTime(newTime);
                    clock.setTime(newTime);
                }
            }
        });
        timeStampLabel.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int count = e.getWheelRotation();
                timeStampFontSize = Math.min(72, Math.max(10, timeStampFontSize + count));
                timeStampLabel.setFont(new Font("Tahoma", Font.BOLD, timeStampFontSize));
                gridButtonPanel.setMinimumSize(timestampPanel.getMinimumSize());
                timeStampLabel.repaint();
                pack();
                validate();
            }
        });
        timestampPanel.add(timeStampLabel);

        atLabel.setText("@");
        timestampPanel.add(atLabel);

        labelSpeed.setFont(new Font("Tahoma", Font.BOLD, timeStampFontSize));
        labelSpeed.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1,
                2));
        labelSpeed.setName("labelSpeed");
        labelSpeed.setText("0");
        timestampPanel.add(labelSpeed);

        xLabel.setFont(new Font("Tahoma", Font.BOLD, fontSize));
        xLabel.setText("x");
        timestampPanel.add(xLabel);

        gridButtonPanel.add(timestampPanel, "north");

        // Placeholder at top, left: 'clear' or 'numlock' position
        gridButtonPanel.add(makePlaceholderButton(), NUM_PAD_KEY_SIZE);

        // Point cell: Mac equal sign, windows forward slash
        pointCellButton = buildButton("pointCell", osModifier);
        gridButtonPanel.add(pointCellButton, NUM_PAD_KEY_SIZE);

        // Show/hide: Mac forward flash, windows asterisk
        showTracksSmallButton = new JButton();
        showTracksSmallButton.setIcon(resourceMap.getIcon(
                "showTracksSmallButton.hide.icon." + osModifier));
        showTracksSmallButton.setName("showTracksSmallButton");
        showTracksSmallButton.getAccessibleContext().setAccessibleName(
                "Show Tracks");
        showTracksSmallButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                showTracksButtonActionPerformed(evt);
            }
        });
        showTracksSmallButton.setFocusPainted(false);
        gridButtonPanel.add(showTracksSmallButton, NUM_PAD_KEY_SIZE);

        // MAC and WINDOWS DIFFER
        if (osModifier.equals("osx")) {
            //Placeholder at asterisk location
            gridButtonPanel.add(makePlaceholderButton(), NUM_PAD_KEY_SIZE);
            //Placeholder - perhaps eventually the sync video button
            gridButtonPanel.add(makePlaceholderButton(false), WIDE_TEXT_FIELD_SIZE);
        } else {
            addGoBackPair();
        }
        // Set cell onset button with 7
        setCellOnsetButton = buildButton("setCellOnset");
        gridButtonPanel.add(setCellOnsetButton, NUM_PAD_KEY_SIZE);

        // Play video button with 8
        playButton = buildButton("play");
        playButton.setRequestFocusEnabled(false);
        gridButtonPanel.add(playButton, NUM_PAD_KEY_SIZE);

        // Set cell offset button with 9
        nineSetCellOffsetButton = buildButton("setCellOffset", "nine");
        gridButtonPanel.add(nineSetCellOffsetButton, NUM_PAD_KEY_SIZE);

        // MAC and WINDOWS DIFFER
        if (osModifier.equals("osx")) {
            addGoBackPair();
        } else {
            // Find button (big plus)
            findButton = buildButton("find", "win");
            gridButtonPanel.add(findButton, TALL_NUM_PAD_KEY_SIZE);
            // Placeholder - perhaps eventually the sync video button
            gridButtonPanel.add(makePlaceholderButton(false), WIDE_TEXT_FIELD_SIZE);
        }

        // Shuttle back button with 4
        shuttleBackButton = buildButton("shuttleBack");
        gridButtonPanel.add(shuttleBackButton, NUM_PAD_KEY_SIZE);

        // Stop button with 5
        stopButton = buildButton("stop");
        gridButtonPanel.add(stopButton, NUM_PAD_KEY_SIZE);

        // Shuttle forward button with 6
        shuttleForwardButton = buildButton("shuttleForward");
        gridButtonPanel.add(shuttleForwardButton, NUM_PAD_KEY_SIZE);


        // MAC and WINDOWS DIFFER
        if (osModifier.equals("osx")) {
            //Find button (small plus)
            findButton = buildButton("find", "osx");
            gridButtonPanel.add(findButton, NUM_PAD_KEY_SIZE);
        }
        addStepSizePanel();

        // Jog back button with 1
        JButton jogBackButton = buildButton("jogBack");
        gridButtonPanel.add(jogBackButton, NUM_PAD_KEY_SIZE);

        // Pause button with 2
        pauseButton = buildButton("pause");
        gridButtonPanel.add(pauseButton, NUM_PAD_KEY_SIZE);

        // Jog forward button with 3
        JButton jogForwardButton = buildButton("jogForward");
        gridButtonPanel.add(jogForwardButton, NUM_PAD_KEY_SIZE);

        // Create new cell button with enter
        createNewCell = buildButton("createNewCell");
        createNewCell.setAlignmentY(0.0F);
        createNewCell.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        gridButtonPanel.add(createNewCell, TALL_NUM_PAD_KEY_SIZE);

        // Onset text field
        onsetTextField.setHorizontalAlignment(SwingConstants.CENTER);
        onsetTextField.setText("00:00:00:000");
        onsetTextField.setToolTipText(resourceMap.getString(
                "onsetTextField.toolTipText"));
        onsetTextField.setName("findOnsetLabel");
        gridButtonPanel.add(makeLabelAndTextfieldPanel(new JLabel("Onset"), onsetTextField), WIDE_TEXT_FIELD_SIZE);

        // Create new cell setting offset button with zero
        createNewCellSettingOffset = buildButton("createNewCellAndSetOffset");
        gridButtonPanel.add(createNewCellSettingOffset, "span 2, w 95!, h 45!");

        // Set cell offset button
        setCellOffsetButton = buildButton("setCellOffset", "period");
        gridButtonPanel.add(setCellOffsetButton, NUM_PAD_KEY_SIZE);

        // Offset text field
        offsetTextField.setHorizontalAlignment(SwingConstants.CENTER);
        offsetTextField.setText("00:00:00:000");
        offsetTextField.setToolTipText(resourceMap.getString("offsetTextField.toolTipText"));
        offsetTextField.setEnabled(false); // Do we really want this? i don't see what makes it different from onset
        offsetTextField.setName("findOffsetLabel");
        gridButtonPanel.add(makeLabelAndTextfieldPanel(new JLabel("Offset"), offsetTextField),
                WIDE_TEXT_FIELD_SIZE);

        getContentPane().setLayout(new MigLayout("ins 0, hidemode 3, fillx", "[growprio 0]0[]", ""));
        getContentPane().add(gridButtonPanel, "");
        getContentPane().setBackground(Color.WHITE);

        tracksPanel.setBackground(Color.WHITE);
        tracksPanel.setVisible(false);
        getContentPane().add(tracksPanel, "growx");

        pack();
    }

    private void addGoBackPair() {
        //Go back button - minus key
        goBackButton = buildButton("goBack", null);
        gridButtonPanel.add(goBackButton, NUM_PAD_KEY_SIZE);
        //Go back text field
        goBackTextField.setHorizontalAlignment(SwingConstants.CENTER);
        goBackTextField.setText("00:00:05:000");
        goBackTextField.setName("goBackTextField");
        gridButtonPanel.add(makeLabelAndTextfieldPanel(new JLabel("Jump back by"), goBackTextField),
                WIDE_TEXT_FIELD_SIZE);
    }

    private void addStepSizePanel() {
        //Go back text field
        stepSizeTextField.setHorizontalAlignment(SwingConstants.CENTER);
        stepSizeTextField.setName("stepSizeTextField");
        stepSizeTextField.setToolTipText("Double click to change");
        stepSizeTextField.setPreferredSize(new Dimension(WIDE_TEXT_FIELD_WIDTH - 10, 18));
        stepSizeTextField.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2 && !streamViewers.isEmpty()) {
                    stepSizeTextField.setEnabled(true);
                    pressStop();
                }
            }
            public void mouseEntered(MouseEvent e) {}
            public void mousePressed(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
        });
        stepSizeTextField.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    float newFramesPerSecond = Float.parseFloat(stepSizeTextField.getText());
                    playbackModel.setCurrentFramesPerSecond(newFramesPerSecond);
                    for (StreamViewer dv : streamViewers) {
                        dv.setFramesPerSecond(newFramesPerSecond);
                    }
                    stepSizeTextField.setEnabled(false);
                    updateStepSizePanelColor();
                }
            }
            public void keyTyped(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
        });
        stepSizePanel = makeLabelAndTextfieldPanel(stepSizeLabel, stepSizeTextField);
        gridButtonPanel.add(stepSizePanel, WIDE_TEXT_FIELD_SIZE);
        updateStepSizeTextField();
    }

    private void updateStepSizeTextField() {
        if (playbackModel == null) {
            stepSizeTextField.setEnabled(false);
        } else if (playbackModel.getCurrentFramesPerSecond() == 0f) {
            stepSizeTextField.setEnabled(false);
            stepSizeTextField.setText("");
        } else {
            stepSizeTextField.setText(Float.toString(playbackModel.getCurrentFramesPerSecond()));
        }
    }

    private void updateStepSizePanelColor() {
        boolean assumedFps = false;
        if (streamViewers != null) {
            for (StreamViewer streamViewer : streamViewers) {
                if (streamViewer.isAssumedFramesPerSecond()) {
                    assumedFps = true;
                }
            }
        }
        stepSizePanel.setBackground(assumedFps ? Color.RED : Color.LIGHT_GRAY);
    }

    /**
     * Action to invoke when the user closes the data controller.
     *
     * @param evt The event that triggered this action.
     */
    private void formWindowClosing(final WindowEvent evt) {
        setVisible(false);
        visible = false;
    }

    /**
     * Action to invoke when the user clicks on the open button.
     *
     * @param evt The event that triggered this action.
     */
    private void openVideoButtonActionPerformed(final ActionEvent evt) {
        logger.info("Add data");

        PluginChooser chooser;

        switch (Datavyu.getPlatform()) {
            case WINDOWS:
                chooser = new WindowsJFC();
                break;
            case MAC:
                chooser = new MacOSJFC();
                break;
            case LINUX:
                chooser = new LinuxJFC();
                break;
            default:
                throw new NotImplementedException("Plugin chooser not implemented.");
        }

        PluginManager pluginManager = PluginManager.getInstance();
        chooser.addPlugin(pluginManager.getPlugins());

        for (FileFilter fileFilter : pluginManager.getFileFilters()) {
            chooser.addChoosableFileFilter(fileFilter);
            chooser.setFileFilter(fileFilter);
        }

        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
            openVideo(chooser);
        }
    }

    /**
     * Action to invoke when the user clicks the show tracks button.
     *
     * @param evt The event that triggered this action.
     */
    private void showTracksButtonActionPerformed(final java.awt.event.ActionEvent evt) {

        // TODO: Fix this assert through proper error handling
        assert (evt.getSource() instanceof JButton);

        JButton button = (JButton) evt.getSource();
        ResourceMap resourceMap = Application.getInstance(
                org.datavyu.Datavyu.class).getContext().getResourceMap(
                VideoController.class);

        if (tracksPanelVisible) {
            logger.info("Show tracks (" + button.getName() + ")");
            // Panel is being displayed, hide it
            button.setIcon(resourceMap.getIcon(button.getName() + ".show.icon." + osModifier));
            button.setPressedIcon(resourceMap.getIcon(button.getName() + "Selected.show.icon." + osModifier));
        } else {
            logger.info("Hide tracks (" + button.getName() + ")");
            // Panel is hidden, show it
            button.setIcon(resourceMap.getIcon(button.getName() + ".hide.icon." + osModifier));
            button.setPressedIcon(resourceMap.getIcon(button.getName() + "Selected.hide.icon." + osModifier));
        }

        tracksPanelVisible = !tracksPanelVisible;
        showTracksPanel(tracksPanelVisible);
        updateCurrentTimeLabel();
    }

    /**
     * Adds a data viewer to this data controller.
     *
     * @param icon   The icon associated with the data viewer.
     * @param viewer The new viewer that we are adding to the data controller.
     * @param file      The parent file that the viewer represents.
     */
    private void addStreamViewer(final ImageIcon icon, final StreamViewer viewer, final File file,
                                 final TrackPainter trackPainter) {
        assert viewer.getIdentifier() != null;

        addViewer(viewer, 0);

        // Add the file to the tracks information panel
        addTrack(viewer.getIdentifier(), icon, file.getAbsolutePath(), file.getName(), viewer.getDuration(),
                 viewer.getStartTime(), trackPainter);

        Datavyu.getProjectController().projectChanged();
    }

    /**
     * Returns set of StreamViewers.
     *
     * @return a set of StreamViewers.
     */
    public Set<StreamViewer> getStreamViewers() {
        return streamViewers;
    }

    /**
     * Adds a track to the tracks panel.
     *
     * @param icon         Icon associated with the track
     * @param mediaPath    Absolute file path to the media file.
     * @param name         The name of the track to add.
     * @param duration     The duration of the data feed in milliseconds.
     * @param startTime    The time start time of the data feed in milliseconds.
     * @param trackPainter Track painter to use.
     */
    public void addTrack(final Identifier id, final ImageIcon icon, final String mediaPath, final String name,
                         final long duration, final long startTime, final TrackPainter trackPainter) {
        mixerController.addNewTrack(id, icon, mediaPath, name, duration, startTime, trackPainter);
    }

    /**
     * Add a viewer to the data controller with the given startTime.
     *
     * @param viewer The data viewer to add.
     * @param startTime The startTime value in milliseconds.
     */
    public void addViewer(final StreamViewer viewer, final long startTime) {
        // Add the QTDataViewerDialog to the list of streamViewers we are controlling.
        streamViewers.add(viewer);
        viewer.setStartTime(startTime);

        // It is possible that the viewer will be handling its own window. In that case
        // don't worry about it.
        if (viewer.getParentJDialog() != null) {
            boolean visible = viewer.getParentJDialog().isVisible();
            Datavyu.getApplication().show(viewer.getParentJDialog());
            if (!visible) {
                viewer.getParentJDialog().setVisible(false);
            }
        }

        // Adjust the overall frame rate
        float framesPerSecond = viewer.getFramesPerSecond();
        if (framesPerSecond > playbackModel.getCurrentFramesPerSecond()) {
            playbackModel.setCurrentFramesPerSecond(framesPerSecond);
        }
        updateStepSizeTextField();
        updateStepSizePanelColor();

        // Update track viewer
        long maxDuration = playbackModel.getMaxDuration();

        if ((viewer.getStartTime() + viewer.getDuration()) > maxDuration) {
            maxDuration = viewer.getStartTime() + viewer.getDuration();
        }

        // BugzID:2114 - If this is the first viewer we are adding, always reset max duration.
        if (streamViewers.size() == 1) {
            maxDuration = viewer.getStartTime() + viewer.getDuration();
        }

        mixerController.getMixerModel().getViewportModel().setViewportMaxEnd(maxDuration, true);
    }

    /**
     * Action to invoke when the user clicks the set cell onset button.
     */
    @Action
    public void setCellOnsetAction() {
        logger.info("Set cell onset");
        new SetSelectedCellStartTimeController(getCurrentTime());
        setOnsetField(getCurrentTime());
    }

    /**
     * Action to invoke when the user clicks on the set cell offest button.
     */
    @Action
    public void setCellOffsetAction() {
        logger.info("Set cell offset");
//        adjustClock(getCurrentTime());
        new SetSelectedCellStopTimeController(getCurrentTime());
        setOffsetField(getCurrentTime());
    }

    /**
     * @param show true to show the tracks layout, false otherwise.
     */
    public void showTracksPanel(final boolean show) {
        tracksPanel.setVisible(show);
        tracksPanel.repaint();
        pack();
        validate();
    }

    /**
     * Handler for a TracksControllerEvent.
     *
     * @param e event
     */
    public void tracksControllerChanged(final TracksControllerEvent e) {
        switch (e.getTracksEvent()) {
            case CARRIAGE_EVENT:
                handleCarriageEvent((CarriageEvent) e.getEventObject());
                break;
            case TIMESCALE_EVENT:
                handleTimescaleEvent((TimescaleEvent) e.getEventObject());
                break;
            default:
                break;
        }
    }

    /**
     * Handles a TimescaleEvent.
     *
     * @param e The timescale event that triggered this action.
     */
    private void handleTimescaleEvent(final TimescaleEvent e) {
        final boolean wasClockRunning = !clock.isStopped();
        final boolean togglePlaybackMode = e.getTogglePlaybackMode();

        if (!wasClockRunning && togglePlaybackMode) {
            playAt(PLAY_RATE);
            clockStart(e.getTime());
        } else {
            seekTime(e.getTime());
        }
    }

    private void seekTime(final long time) {
        long newTime = time;

        if (newTime < playbackModel.getStartTime()) {
            newTime = playbackModel.getStartTime();
        }

        if (newTime > playbackModel.getEndTime()) {
            newTime = playbackModel.getEndTime();
        }

        clockStop(newTime);
        setCurrentTime(newTime);
        clock.setTime(newTime);
    }

    /**
     * Handles a CarriageEvent (when the carriage moves due to user
     * interaction).
     *
     * @param e The carriage event that triggered this action.
     */
    private void handleCarriageEvent(final CarriageEvent e) {

        switch (e.getEventType()) {

            case OFFSET_CHANGE:
                handleCarriageOffsetChangeEvent(e);
                break;

            case CARRIAGE_LOCK:
            case MARKER_CHANGED:
            case MARKER_SAVE:
                Datavyu.getProjectController().projectChanged();
                break;

            default:
                throw new IllegalArgumentException("Unknown carriage event.");
        }
    }

    /**
     * @param e
     */
    private void handleCarriageOffsetChangeEvent(final CarriageEvent e) {

        // Look through our data streamViewers and update the offset
        StreamViewer viewer = getStreamViewer(e.getTrackId());
        if (viewer != null) {
            viewer.setStartTime(e.getOffset());
        }

        Datavyu.getProjectController().projectChanged();

        // Recalculate the maximum playback duration.
        long maxDuration = ViewportStateImpl.MINIMUM_MAX_END;

        for (StreamViewer streamViewer : streamViewers) {
            if ((streamViewer.getDuration() + streamViewer.getStartTime()) > maxDuration) {
                maxDuration = streamViewer.getDuration() + streamViewer.getStartTime();
            }
        }

        mixerController.getMixerModel().getViewportModel().setViewportMaxEnd(maxDuration, false);
    }

    private void handleNeedleChanged(final PropertyChangeEvent e) {

        if (clock.isStopped()) {
            final long newTime = mixerController.getMixerModel().getNeedleModel().getCurrentTime();
            clock.setTime(newTime);
            clockStep(newTime);
        }

        updateCurrentTimeLabel();
    }

    private void handleRegionChanged(final PropertyChangeEvent e) {
        final RegionState region = mixerController.getMixerModel().getRegionModel().getRegion();
        playbackModel.setStartTime(region.getRegionStart());
        playbackModel.setEndTime(region.getRegionEnd());
    }

    private void handleViewportChanged(final PropertyChangeEvent e) {
        final ViewportState viewport = mixerController.getMixerModel().getViewportModel().getViewport();
        playbackModel.setMaxDuration(viewport.getMaxEnd());
    }

    /**
     * Simulates play button clicked.
     */
    public void pressPlay() {
        playButton.doClick();
    }

    public void pressShowTracksSmall() {
        showTracksSmallButton.doClick();
    }

    /**
     * Simulates pause button clicked.
     */
    public void pressPause() {
        pauseButton.doClick();
    }

    /**
     * Simulates stop button clicked.
     */
    public void pressStop() {
        stopButton.doClick();
    }

    /**
     * Simulates shuttle forward button clicked.
     */
    public void pressShuttleForward() {
        shuttleForwardButton.doClick();
    }

    /**
     * Simulates shuttle back button clicked.
     */
    public void pressShuttleBack() {
        shuttleBackButton.doClick();
    }

    /**
     * Simulates find button clicked.
     */
    public void pressFind() {
        findButton.doClick();
    }

    /**
     * Simulates set cell onset button clicked.
     */
    public void pressSetCellOnset() {
        setCellOnsetButton.doClick();
    }

    /**
     * Simulates set cell offset button clicked.
     */
    public void pressSetCellOffsetPeriod() {
        setCellOffsetButton.doClick();
    }

    /**
     * Simulates set cell offset button clicked.
     */
    public void pressSetCellOffsetNine() {
        nineSetCellOffsetButton.doClick();
    }

    /**
     * Simulates set new cell onset button clicked.
     */
    public void pressPointCell() {
        pointCellButton.doClick();
    }

    /**
     * Simulates go back button clicked.
     */
    public void pressGoBack() {
        goBackButton.doClick();
    }

    /**
     * Simulates create new cell button clicked.
     */
    public void pressCreateNewCell() {
        createNewCell.doClick();
    }

    /**
     * Simulates create new cell setting offset button clicked.
     */
    public void pressCreateNewCellSettingOffset() {
        createNewCellSettingOffset.doClick();
    }

    /**
     * Action to invoke when the user clicks on the play button.
     */
    @Action
    public void playAction() {
        logger.info("Play");

        // BugzID:464 - When stopped at the end of the region of interest.
        // pressing play jumps the stream back to the start of the video before
        // starting to play again.
        if ((getCurrentTime() >= playbackModel.getEndTime()) && clock.isStopped()) {
            jumpTo(playbackModel.getStartTime());
        }

        playAt(PLAY_RATE);
    }

    /**
     * Action to invoke when the user clicks on the fast forward button.
     */
    @Action
    public void forwardAction() {
        logger.info("Fast forward");
        playAt(FAST_FORWARD_RATE);
    }

    /**
     * Action to invoke when the user clicks on the rewind button.
     */
    @Action
    public void rewindAction() {
        logger.info("Rewind");
        playAt(REWIND_RATE);
    }

    /**
     * Action to invoke when the user clicks on the pause button.
     */
    @Action
    public void pauseAction() {
        logger.info("Pause button ... " + System.currentTimeMillis());

        // Resume from pause at playback rate prior to pause.
        if (clock.isStopped()) {
            shuttleAt(playbackModel.getPauseRate());

            // Pause views - store current playback rate.
        } else {
            playbackModel.setPauseRate(clock.getRate());
            clock.stop();
            labelSpeed.setText("[" + FloatingPointUtils.doubleToFractionStr(playbackModel.getPauseRate())  + "]");
        }
    }

    /**
     * Action to invoke when the user clicks on the stop button.
     */
    @Action
    public void stopAction() {
        logger.info("Stop event..." + System.currentTimeMillis());
        clock.stop();
        playbackModel.setPauseRate(0);
    }

    /**
     * Action to invoke when the user clicks on the shuttle forward button.
     *
     * TODO: proper behavior for reversing shuttle direction?
     */
    @Action
    public void shuttleForwardAction() {
        logger.info("Shuttle forward..." + System.currentTimeMillis());
        shuttle(1);
    }

    /**
     * Action to invoke when the user clicks on the shuttle back button.
     */
    @Action
    public void shuttleBackAction() {
        logger.info("Shuttle back");
        shuttle(-1);
    }

    /**
     * Searches the shuttle rates array for the given rate, and returns the
     * index.
     *
     * @param rate The rate to search for.
     * @return The index of the rate, or -100 if not found.
     */
    private int rateToShuttleIndex(final float rate) {
        for (int i = 0; i < SHUTTLE_RATES.length; i++) {
            if (SHUTTLE_RATES[i] == rate) {
                return i;
            }
        }
        return -100; // TODO: FIX ME!
    }

    /**
     * Populates the find time in the controller.
     *
     * @param milliseconds The time to use when populating the find field.
     */
    public void setOnsetField(final long milliseconds) {
        onsetTextField.setText(CLOCK_FORMAT.format(milliseconds));
    }

    /**
     * Populates the find offset time in the controller.
     *
     * @param milliseconds The time to use when populating the find field.
     */
    public void setOffsetField(final long milliseconds) {
        offsetTextField.setText(CLOCK_FORMAT.format(milliseconds));
    }

    /**
     * Action to invoke when the user clicks on the find button.
     */
    @Action
    public void findAction() {
        if (shiftMask) {
            findOffsetAction();
        } else {

            try {
                logger.info("Finding to " + onsetTextField.getText() + " "
                        + CLOCK_FORMAT.parse(onsetTextField.getText()).getTime());
                jumpTo(CLOCK_FORMAT.parse(onsetTextField.getText()).getTime());
            } catch (ParseException e) {
                logger.error("unable to find within video", e);
            }
        }
    }

    /**
     * Action to invoke when the user holds shift down.
     */
    public void findOffsetAction() {
        try {
            jumpTo(CLOCK_FORMAT.parse(offsetTextField.getText()).getTime());
        } catch (ParseException e) {
            logger.error("Unable to find offset " + e);
        }
    }

    public void toggleCellHighlighting() {
        highlightCells = !highlightCells;
    }

    public boolean getCellHighlighting() {
        return highlightCells;
    }

    public void setCellHighlighting(boolean v) {
        highlightCells = v;
    }

    public void toggleCellHighlightingAutoFocus() {
        highlightAndFocus = !highlightAndFocus;
    }

    public boolean getCellHighlightAndFocus() {
        return highlightAndFocus;
    }

    public void clearRegionOfInterestAction() {
        mixerController.clearRegionOfInterest();
    }

    /**
     * Sets the playback region of interest to lie from the find time to offset time.
     */
    public void setRegionOfInterestAction() {
        if (this.getStreamViewers().size() > 0) {
            try {
                final long findTextTime = CLOCK_FORMAT.parse(
                        onsetTextField.getText()).getTime();
                final long findOffsetTime = CLOCK_FORMAT.parse(
                        offsetTextField.getText()).getTime();

                final long newWindowPlayStart = findTextTime;
                final long newWindowPlayEnd = (findOffsetTime > newWindowPlayStart)
                        ? findOffsetTime : newWindowPlayStart;
                mixerController.getMixerModel().getNeedleModel().setCurrentTime(
                        newWindowPlayStart);
                mixerController.getMixerModel().getRegionModel().setPlaybackRegion(
                        newWindowPlayStart, newWindowPlayEnd);
            } catch (ParseException e) {
                logger.error("Unable to set region of interest for playback " + e);
            }
        }
    }

    /**
     * Action to invoke when the user clicks on the go back button.
     */
    @Action
    public void goBackAction() {
        try {
            long j = -CLOCK_FORMAT.parse(goBackTextField.getText()).getTime();
            logger.info("Jump back by " + j);
            jump(j);

            //Bugzilla bug #179 - undoes the behavior from FogBugz BugzID:721
            // BugzID:721 - After going back - start playing again.
            //playAt(PLAY_RATE);
        } catch (ParseException e) {
            logger.error("Unable to jump back " + e);
        }
    }

    /**
     * Action to invoke when the user clicks on the jog backwards button.
     */
    @Action
    public void jogBackAction() {
        logger.info("Jog back");

        if (!clock.isStopped()) {
            clockStop(clock.getTime());
        } else {

            int mul = 1;

            if (shiftMask) {
                mul = SHIFT_JOG;
            }

            if (ctrlMask) {
                mul = CTRL_JOG;
            }

            long stepSize = ((-ONE_SECOND) / (long) playbackModel.getCurrentFramesPerSecond());
            long nextTime = mul * stepSize;

            /* BugzID:1361 - Disallow jog to skip past the region boundaries. */
            if ((clock.getTime() + nextTime) > playbackModel.getStartTime()) {
                stopAction();
                jump(nextTime);
            } else {
                jumpTo(playbackModel.getStartTime());
            }
        }
    }

    /**
     * Action to invoke when the user clicks on the jog forwards button.
     */
    @Action
    public void jogForwardAction() {
        logger.info("Jog forward");

        if (!clock.isStopped()) {
            clockStop(clock.getTime());
        } else {

            int mul = 1;

            if (shiftMask) {
                mul = SHIFT_JOG;
            }

            if (ctrlMask) {
                mul = CTRL_JOG;
            }

            long stepSize = ((ONE_SECOND) / (long) playbackModel.getCurrentFramesPerSecond());
            long nextTime = (long) (mul * stepSize);

            /* BugzID:1361 - Disallow jog to skip past the region boundaries. */
            if ((clock.getTime() + nextTime) < playbackModel.getEndTime()) {
                stopAction();
                jump(nextTime);
            } else {
                jumpTo(playbackModel.getEndTime());
            }
        }
    }

    /**
     * @param rate Rate of play.
     */
    private void playAt(final float rate) {
        playbackModel.setPauseRate(0);
        shuttleAt(rate);
    }

    /**
     * @param shuttleJump The required rate/direction of the shuttle.
     */
    private void shuttle(final int shuttleJump) {
        float currentRate = clock.getRate();

        if (currentRate == 0) {
            currentRate = playbackModel.getPauseRate();
        }

        try {
            shuttleAt(SHUTTLE_RATES[rateToShuttleIndex(currentRate) + shuttleJump]);
        } catch (java.lang.ArrayIndexOutOfBoundsException e) {
            logger.error("Error finding shuttle index given at the current rate " + currentRate);
        }
    }

    /**
     * @param rate Rate of shuttle.
     */
    private void shuttleAt(final float rate) {
        clock.setRate(rate);
        clock.start();
    }

    /**
     * @param step Milliseconds to jump.
     */
    private void jump(final long step) {
        if ((clock.getTime() + step) > playbackModel.getStartTime()) {
//            stopAction();
//            clock.stop();
            clock.stepTime(step);
        } else {
            jumpTo(playbackModel.getStartTime());
        }
    }

    /**
     * @param time Absolute time to jump to.
     */
    private void jumpTo(final long time) {
        synchronized (clock) {
            clock.stop();
            clock.setTime(time);
        }
    }

    /**
     * Action to invoke when the user clicks on the create new cell button.
     */
    @Action
    public void createNewCellAction() {
        logger.info("New cell");
        if (!clock.isStopped()) adjustClock(getCurrentTime());
        CreateNewCellController controller = new CreateNewCellController();
        controller.createDefaultCell(true);
    }

    /**
     * Action to invoke when the user clicks on the new cell button.
     */
    @Action
    public void createNewCellAndSetOffsetAction() {
        logger.info("New cell set offset");
        if (!clock.isStopped()) adjustClock(getCurrentTime());
        new CreateNewCellController(getCurrentTime(), true);
    }

    /**
     * Action to invoke when the user clicks on the new cell offset button.
     */
    @Action
    public void pointCellAction() {
        logger.info("Set new cell offset");

        adjustClock(getCurrentTime());
        long time = getCurrentTime();
        new CreateNewCellController(time, false);
        new SetNewCellStopTimeController(time);
        setOffsetField(time);
    }

    /**
     * Action to invoke when the user clicks on the sync video button.
     */
    @Action
    public void syncVideoAction() {
        //not yet implemented
    }

    @Override
    public void propertyChange(final PropertyChangeEvent e) {
        Object source = e.getSource();
        if (source == mixerController.getNeedleController().getNeedleModel()) {
            handleNeedleChanged(e);
        } else if (source == mixerController.getMixerModel().getViewportModel()) {
            handleViewportChanged(e);
        } else if (source == mixerController.getRegionController().getModel()) {
            handleRegionChanged(e);
        }
    }

    public float getCurrentFPS() {
        return playbackModel.getCurrentFramesPerSecond();
    }

    public ClockTimer getClock() {
        return clock;
    }
}
