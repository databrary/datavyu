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
import org.datavyu.controllers.component.TracksEditorController;
import org.datavyu.event.component.CarriageEvent;
import org.datavyu.event.component.TimescaleEvent;
import org.datavyu.event.component.TracksControllerEvent;
import org.datavyu.event.component.TracksControllerListener;
import org.datavyu.models.Identifier;
import org.datavyu.models.component.*;
import org.datavyu.plugins.StreamViewer;
import org.datavyu.plugins.Plugin;
import org.datavyu.plugins.PluginManager;
import org.datavyu.util.*;
import org.datavyu.util.ClockTimer.ClockListener;
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
import java.io.FileNotFoundException;
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

    /** Sync threshold for HARD sync between */
    private static final long SYNC_THRESHOLD = 31L; // milliseconds

    /** Threshold used to compare frame rates */
    private static final double ALMOST_EQUAL_FRAME_RATES = 1e-1*5;

    /** The logger for this class */
    private static Logger logger = LogManager.getLogger(VideoController.class);

    /** One second in milliseconds */
    private static final long MILLI_IN_SEC = 1000L;

    /** The 45x45 size for num pad keys */
    private static final int NUM_PAD_KEY_HEIGHT = 45;

    private static final int NUM_PAD_KEY_WIDTH = 45;

    private static final String NUM_PAD_KEY_SIZE = "w " + NUM_PAD_KEY_HEIGHT + "!, h " + NUM_PAD_KEY_WIDTH + "!";

    /** The 45x95 size for tall num pad keys (enter, PC plus) */
    private static final int TALL_NUM_PAD_KEY_HEIGHT = 95;

    private static final String TALL_NUM_PAD_KEY_SIZE = "span 1 2, w " + NUM_PAD_KEY_WIDTH + "!, h "
            + TALL_NUM_PAD_KEY_HEIGHT + "!";

    /** The 80x40 size for the text fields to the right of num pad */
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

    // initialize standard date format for clockTimer display.
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

    private static RateController shuttleRates = new RateController();

    /**
     * // TODO: What is this doing here?
     *
     * Visible?
     */
    private boolean visible;

    /** Determines whether or not the 'shift' key is being held */
    private boolean shiftMask = false;

    /** The set of streamViewers associated with this controller */
    private LinkedHashSet<StreamViewer> streamViewers = new LinkedHashSet<>();

    /** List of VideoInfo objects holding information about the filename, plugin */
    private LinkedHashSet<VideoInfo> videoInfos = new LinkedHashSet<>();

    /** Clock timer */
    private final ClockTimer clockTimer = new ClockTimer();

    /** Is the tracks panel currently shown */
    private boolean tracksPanelVisible = true;

    /** The controller for manipulating tracks */
    private MixerController mixerController = new MixerController();

    /** Button to create a new cell */
    private JButton createNewCell;

    /** Button to create a new cell setting offset */
    private JButton createNewCellSettingOffset;

    /** */
    private JButton findButton;

    /** */
    private JTextField offsetTextField;

    /** */
    private JTextField onsetTextField;

    /** */
    private JButton goBackButton;

    /** */
    private JTextField goBackTextField;

    /** */
    private JTextField stepSizeTextField;

    /** */
    private JLabel stepSizeLabel = new JLabel("Steps per second");

    /** */
    private JPanel stepSizePanel;

    /** */
    private JPanel gridButtonPanel;

    /** */
    private JLabel labelSpeed;

    /** */
    private JButton pauseButton;

    /** */
    private JButton playButton;

    /** */
    private JButton nineSetCellOffsetButton;

    /** */
    private JButton setCellOffsetButton;

    /** */
    private JButton setCellOnsetButton;

    /** */
    private JButton pointCellButton;

    /** */
    private JButton showTracksSmallButton;

    /** */
    private JButton shuttleBackButton;

    /** */
    private JButton shuttleForwardButton;

    /** */
    private JButton stopButton;

    /** */
    private JLabel timeStampLabel;

    /** */
    private JPanel tracksPanel;

    private ResourceMap resourceMap;

    private ActionMap actionMap;

    private String osModifier;

    private boolean highlightCells = false;

    private boolean highlightAndFocus = false;

    private FrameRateController frameRateController = new FrameRateController();

    /**
     * Create a new VideoController.
     *
     * @param parent The parent of this form.
     * @param modal  Should the dialog be modal or not?
     */
    public VideoController(final Frame parent, final boolean modal) {
        super(parent, modal);

        clockTimer.registerListener(this);

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

        final int defaultEndTime = (int) MixerController.DEFAULT_DURATION;

        clockTimer.setMinTime(0);
        clockTimer.setMaxTime(defaultEndTime);

        tracksPanel.add(mixerController.getTracksPanel(), "growx");
        mixerController.addTracksControllerListener(this);
        mixerController.getMixerModel().getViewportModel().addPropertyChangeListener(this);
        mixerController.getMixerModel().getRegionModel().addPropertyChangeListener(this);
        mixerController.getMixerModel().getNeedleModel().addPropertyChangeListener(this);

        showTracksPanel(tracksPanelVisible);

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
    private Identifier openVideo(final PluginChooser chooser) {
        final Plugin plugin = chooser.getSelectedPlugin();
        final File selectedFile = chooser.getSelectedFile();

        return openVideo(selectedFile, plugin);
    }

    /* Open a video using specified file path and plugin.
        @param filepath Name of video file
        @param plugin Name of plugin to use (current short names: jfx, ffmpeg, nativeosx
     */
    public Identifier openVideo(final String filepath, final String pluginStr) throws FileNotFoundException {
        File videoFile = new File(filepath);
        if(!videoFile.exists()){
            logger.error("Cannot find file: " + filepath);
            return null;
        }

        Plugin plugin = PluginManager.getInstance().getPluginFromShortName(pluginStr);
        if(plugin == null ){
            logger.error("Cannot find plugin: " + pluginStr);
            return null;
        }

        return openVideo(videoFile, plugin);
    }

    /** Open video file using given plugin.
     * @param videoFile Video file.
     * @param plugin Plugin to open with.
     */
    public Identifier openVideo(final File videoFile, final Plugin plugin){
        Identifier id = Identifier.generateIdentifier();
        new Thread(() -> {
            if (plugin != null) {
                try {
                    StreamViewer streamViewer = plugin.getNewStreamViewer(
                            id,
                            videoFile,
                            Datavyu.getApplication().getMainFrame(),
                            false);
                    addStream(plugin.getTypeIcon(), streamViewer);

                    // Add this video's information to videoInfos
                    videoInfos.add(new VideoInfo(id, videoFile, plugin, streamViewer));

                    mixerController.bindTrackActions(streamViewer.getIdentifier(), streamViewer.getCustomActions());
                    streamViewer.addViewerStateListener(mixerController.getTracksEditorController()
                            .getViewerStateListener(streamViewer.getIdentifier()));
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

        // We return the id here so the Ruby API can use it to query other classes for information.
        // This id should not be used internally since it is not guaranteed to be tied to a valid stream.
        return id;
    }
    /**
     * Tells the Data Controller if shift is being held or not.
     *
     * @param shift True for shift held; false otherwise.
     */
    public void setShiftMask(boolean shift) {
        shiftMask = shift;
    }

    /**
     * @param clockTime Current clockTimer time in milliseconds.
     */
    public void clockStart(double clockTime) {
        logger.info("Start");
        TracksEditorController tracksEditorController = mixerController.getTracksEditorController();
        for (StreamViewer streamViewer : streamViewers) {
            TrackModel trackModel = tracksEditorController.getTrackModel(streamViewer.getIdentifier());
            // TODO: Ensure that there is a return value by tying offset/duration directly to the object
            if (trackModel != null) {
                if (clockTime >= trackModel.getOffset()) {
                    logger.info("Clock Start Starts track: " + trackModel.getIdentifier() + " at time: " + clockTime);
                    streamViewer.start();
                }
            }
        }
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

    public void clockForceSync(double clockTime) {
        logger.info("Forced sync");
        for (StreamViewer streamViewer : streamViewers) {
            streamViewer.setCurrentTime((long) clockTime);
        }
        // Updates the position of the needle and label
        updateCurrentTimeLabelAndNeedle((long) clockTime);
    }

    @Override
    public void clockBoundaryCheck(double clockTime) {
        TracksEditorController tracksEditorController = mixerController.getTracksEditorController();
        for (StreamViewer streamViewer : streamViewers) {
            TrackModel trackModel = tracksEditorController.getTrackModel(streamViewer.getIdentifier());
            if (trackModel != null && !clockTimer.isStopped()) {
                // Only if in range and not already playing and not in seek playback
                if (clockTime < mixerController.getRegionController().getModel().getRegion().getRegionEnd()
                        && clockTime >= trackModel.getOffset()
                        && !streamViewer.isPlaying() && !streamViewer.isSeekPlaybackEnabled()) {
                    logger.info("Clock Boundary Starting track: " + trackModel.getIdentifier() + " Master Clock at " + clockTime +" and Streamviewer clock at "+ streamViewer.getCurrentTime());
                     streamViewer.start();
                }
                if ((clockTime < trackModel.getOffset()
                        || clockTime >= trackModel.getOffset() + trackModel.getDuration()
                        || clockTime >= mixerController.getRegionController().getModel().getRegion().getRegionEnd())
                        && streamViewer.isPlaying()) {
                    logger.info("Clock Boundary Stopping track: " + trackModel.getIdentifier() + " Master Clock at " + clockTime +" and Streamviewer clock at "+ streamViewer.getCurrentTime());
                    streamViewer.stop();
                }
            }
        }
        // Updates the position of the needle and label
        // Check for visible to remove Java Null pointer exception for non-initialised Needle Model
        if (visible) {
            updateCurrentTimeLabelAndNeedle((long) clockTime);
        }
    }

    @Override
    public void clockSeekPlayback(double clockTime) {
        TracksEditorController tracksEditorController = mixerController.getTracksEditorController();
        for (StreamViewer streamViewer : streamViewers) {
            if (streamViewer.isSeekPlaybackEnabled()) {
                TrackModel trackModel = tracksEditorController.getTrackModel(streamViewer.getIdentifier());
                if (trackModel != null) {
                    streamViewer.setCurrentTime((long) clockTime - trackModel.getOffset());
                    logger.info("Clock Seek Playback is seeking stream " + streamViewer.getIdentifier() + " to time: " + (clockTime - trackModel.getOffset()));
                }
            }
        }
    }

    /**
     * @param clockTime Current clockTimer time in milliseconds.
     */
    public void clockPeriodicSync(double clockTime) {
        TracksEditorController tracksEditorController = mixerController.getTracksEditorController();
        for (StreamViewer streamViewer : streamViewers) {
            TrackModel trackModel = tracksEditorController.getTrackModel(streamViewer.getIdentifier());
            if (trackModel != null) {
                double trackTime = Math.min(Math.max(clockTime - trackModel.getOffset(), 0), trackModel.getDuration());
                double difference = Math.abs(trackTime - streamViewer.getCurrentTime());
                if (difference >= ClockTimer.SYNC_THRESHOLD) {
                    streamViewer.setCurrentTime((long) trackTime);
                    logger.info("Sync of clock with difference: " + difference + " milliseconds.");
                }
            }
        }
        // Updates the position of the needle and label
        updateCurrentTimeLabelAndNeedle((long) clockTime);
    }

    /**
     * @param clockTime Current clockTimer time in milliseconds.
     */
    public void clockStop(double clockTime) {
        logger.info("Stop clock at " + (long) clockTime + " msec.");
        for (StreamViewer streamViewer : streamViewers) {
            // Sync streams at stop
            streamViewer.stop();
        }
        updateCurrentTimeLabelAndNeedle((long) clockTime);
    }

    /**
     * @param rate Current (updated) clockTimer rate.
     */
    public void clockRate(float rate) {
        labelSpeed.setText(FloatingPointUtils.doubleToFractionStr(rate));
        for (StreamViewer streamViewer : streamViewers) {
            streamViewer.setRate(rate);
        }
    }

    /**
     * @return the mixer controller.
     */
    public MixerController getMixerController() {
        return mixerController;
    }

    private void updateCurrentTimeLabelAndNeedle(long currentTime) {
        long currentTimeInRange = clockTimer.toRange(currentTime);

        timeStampLabel.setText(tracksPanelVisible ? CLOCK_FORMAT_HTML.format(currentTimeInRange)
                                                  : CLOCK_FORMAT_HTML.format(currentTimeInRange));
        mixerController.getMixerModel().getNeedleModel().setCurrentTime(currentTimeInRange);
    }

    /**
     * Get the current master clockTimer time for the controller.
     *
     * @return Time in milliseconds.
     */
    public long getCurrentTime() {
        return (long) clockTimer.getClockTime();
    }

    /**
     * Set time location for data streams.
     *
     * @param milliseconds The millisecond time.
     */
    public void setCurrentTime(final long milliseconds) {
        clockTimer.setForceTime(milliseconds);
    }

    /**
     * Recalculates the maximum viewer duration.
     */
    public void updateMaxViewerDuration() {
        long maxDuration = ViewportStateImpl.MINIMUM_MAX_END;

        for (StreamViewer streamViewer : streamViewers) {
            if ((streamViewer.getDuration() + streamViewer.getOffset()) > maxDuration) {
                maxDuration = streamViewer.getDuration() + streamViewer.getOffset();
            }
        }

        mixerController.getMixerModel().getViewportModel().setViewportMaxEnd(maxDuration, true);

        if (streamViewers.isEmpty()) {
            mixerController.getNeedleController().resetNeedlePosition();
            mixerController.getMixerModel().getRegionModel().resetPlaybackRegion();
        }
    }

    /**
     * Get the stream viewer with the identifier.
     *
     * @param id The identifier for the stream viewer.
     *
     * @return A stream viewer if found otherwise null.
     */
    public StreamViewer getStreamViewer(final Identifier id) {
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
        StreamViewer streamViewer = getStreamViewer(id);

        if ((streamViewer == null) || !shouldRemove()) {
            return;
        }

        streamViewers.remove(streamViewer);

        streamViewer.stop();
        streamViewer.close();

        JDialog viewDialog = streamViewer.getParentJDialog();

        if (viewDialog != null) {
            viewDialog.dispose();
        }

        // Remove the frame rate, this is problematic if we have several tracks with the same frame rate
        frameRateController.removeFrameRate(streamViewer.getIdentifier().asLong());

        // BugzID:2000
        streamViewer.removeViewerStateListener(
                mixerController.getTracksEditorController().getViewerStateListener(
                        streamViewer.getIdentifier()));

        // Recalculate the maximum playback duration
        updateMaxViewerDuration();

        // Remove the data viewer from the tracks panel
        mixerController.deregisterTrack(streamViewer.getIdentifier());

        // Data viewer removed, mark project as changed
        Datavyu.getProjectController().projectChanged();
    }

    /**
     * Binds a window event listener to a stream viewer.
     *
     * @param id The identifier of the viewer.
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
        JButton jButton = new JButton();
        jButton.setAction(actionMap.get(name + "Action"));
        jButton.setIcon(resourceMap.getIcon(name + "Button.icon" + dotModifier));
        jButton.setPressedIcon(resourceMap.getIcon(name + "SelectedButton.icon" + dotModifier));
        jButton.setFocusPainted(false);
        jButton.setName(name + "Button");
        return jButton;
    }

    private JButton buildButton(final String name) {
        return buildButton(name, null);
    }

    private JPanel makeLabelAndTextFieldPanel(JLabel label, JTextField textField) {
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
                // Open a new frame here as i have assumed you have declared "frame" as instance variable
                if (e.getClickCount() >= 3) {
                    int newTime = Integer.parseInt(JOptionPane.showInputDialog(null,
                            "Enter new time in ms", getCurrentTime()));
                    setCurrentTime(newTime);
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
        playButton = buildButton("start");
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
        gridButtonPanel.add(makeLabelAndTextFieldPanel(new JLabel("Onset"), onsetTextField), WIDE_TEXT_FIELD_SIZE);

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
        gridButtonPanel.add(makeLabelAndTextFieldPanel(new JLabel("Offset"), offsetTextField),
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
        gridButtonPanel.add(makeLabelAndTextFieldPanel(new JLabel("Jump back by"), goBackTextField),
                WIDE_TEXT_FIELD_SIZE);
    }

    private void addStepSizePanel() {
        // Go back text field
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

                    // Parse the new frame rate
                    float newFramesPerSecond = 1F/Float.parseFloat(stepSizeTextField.getText());

                    // Update the frame rate controller with the user defined frame rate
                    frameRateController.addUserFrameRate(newFramesPerSecond);

                    // Update the streams with the new rate
                    clockTimer.setRate(newFramesPerSecond);

                    stepSizeTextField.setEnabled(false);
                    updateStepSizePanelColor();
                }
            }
            public void keyTyped(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
        });
        stepSizePanel = makeLabelAndTextFieldPanel(stepSizeLabel, stepSizeTextField);
        gridButtonPanel.add(stepSizePanel, WIDE_TEXT_FIELD_SIZE);
        updateStepSizeTextField();
    }

    private void updateStepSizeTextField() {

        // If we don't have any stream viewers, disable the label and remove the user-defined rate
        if (streamViewers.isEmpty()) {
            stepSizeTextField.setEnabled(false);
            frameRateController.removeUserFrameRate();
        } else if (frameRateController.isZeroRate()) {
            stepSizeTextField.setEnabled(false);
            stepSizeTextField.setText("");
        } else {
            stepSizeTextField.setText(Float.toString(frameRateController.getFrameRate()));
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
    private void showTracksButtonActionPerformed(ActionEvent evt) {

        // TODO: Fix this assert through proper error handling
        assert (evt.getSource() instanceof JButton);

        JButton button = (JButton) evt.getSource();
        ResourceMap resourceMap = Application.getInstance(org.datavyu.Datavyu.class).getContext().getResourceMap(
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
    }

    /**
     * Returns set of StreamViewers.
     *
     * @return a set of StreamViewers.
     */
    public LinkedHashSet<StreamViewer> getStreamViewers() {
        return streamViewers;
    }

    /**
     * @return set of video info objects.
     */
    public LinkedHashSet<VideoInfo> getVideoInfos() { return videoInfos; }

    /**
     * Adds a data streamViewer to this data controller.
     *
     * @param imageIcon      The imageIcon associated with the data streamViewer
     * @param streamViewer    The new streamViewer that we are adding to the data controller
     */
    public void addStream(final ImageIcon imageIcon, final StreamViewer streamViewer) {
        assert streamViewer.getIdentifier() != null;

        // Add the streamViewer
        streamViewers.add(streamViewer);

        // Adjust the overall frame rate
        frameRateController.addFrameRate(streamViewer.getIdentifier().asLong(), streamViewer.getFramesPerSecond());

        updateStepSizeTextField();
        updateStepSizePanelColor();

        // Add as track
        mixerController.addNewTrack(
                streamViewer.getIdentifier(),
                imageIcon,
                streamViewer.getSourceFile(),
                streamViewer.getDuration(),
                streamViewer.getOffset(),
                streamViewer.getTrackPainter());

        // min and max time
        long maxTime = mixerController.getTracksEditorController().getMaxTime();
        long minTime = mixerController.getTracksEditorController().getMinTime();

        // Adjust the maximum stream time
        mixerController.getMixerModel().getViewportModel().setViewportWindow(minTime, maxTime);

        Datavyu.getProjectController().projectChanged();
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
     * Action to invoke when the user clicks on the set cell offset button
     */
    @Action
    public void setCellOffsetAction() {
        logger.info("Set cell offset");
        new SetSelectedCellStopTimeController(getCurrentTime());
        setOffsetField(getCurrentTime());
    }

    /**
     * @param show true to show the tracks layout, false otherwise
     */
    private void showTracksPanel(final boolean show) {
        updateCurrentTimeLabelAndNeedle(getCurrentTime());
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
        switch (e.getEventType()) {
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
     * Handles a TimescaleEvent that is created when pulling on the needle with the mouse
     *
     * @param e The timescale event that triggered this action
     */
    private void handleTimescaleEvent(final TimescaleEvent e) {
        logger.info("Change time to " + e.getTime() + " milliseconds and toggle: " + e.getToggleStartStop());

        // Set the time
//        clockTimer.setTime(e.getTime());
        clockTimer.setForceTime(e.getTime());

        // Toggle
        if (e.getToggleStartStop()) {
            clockTimer.toggle();
        }
    }

    /**
     * Handles a CarriageEvent when the carriage moves due to user interaction
     *
     * @param e The carriage event
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
            viewer.setOffset(e.getOffset());
        }

        Datavyu.getProjectController().projectChanged();

        // Recalculate the maximum playback duration.
        long maxDuration = ViewportStateImpl.MINIMUM_MAX_END;

        for (StreamViewer streamViewer : streamViewers) {
            if ((streamViewer.getDuration() + streamViewer.getOffset()) > maxDuration) {
                maxDuration = streamViewer.getDuration() + streamViewer.getOffset();
            }
        }

        mixerController.getMixerModel().getViewportModel().setViewportMaxEnd(maxDuration, false);
    }

    private void handleNeedleChange(final NeedleState needle) {
        // Nothing to do here, since we update the needle based on the clock timer and not vice versa
        // Notice, that dragging on the needle is handled through "handleTimescaleEvent"
    }

    private void handleRegionChange(final RegionState region) {
        final long start = region.getRegionStart();
        final long end = region.getRegionEnd();
        logger.info("Set Region with start " + start + " ane end " + end );
        clockTimer.setMinTime(start);
        clockTimer.setMaxTime(end);
    }

    private void handleViewportChange(final ViewportState viewport) {
        clockTimer.setMaxTime(viewport.getMaxEnd());
    }

    /**
     * Simulates start button clicked.
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
     * Action to invoke when the user clicks on the start button.
     */
    @Action
    @SuppressWarnings("unused")  // Called through actionMap
    public void startAction() {
        logger.info("Play");
        clockTimer.setRate(1f);
    }

    /**
     * Action to invoke when the user clicks on the pause button.
     */
    @Action
    @SuppressWarnings("unused")  // Called through actionMap
    public void pauseAction() {
        // Toggle between isPlaying and not isPlaying
        if (clockTimer.isStopped()) {
            logger.info("Pause: Resume isPlaying at rate: " + clockTimer.getRate());
            clockTimer.start();
            labelSpeed.setText(FloatingPointUtils.doubleToFractionStr(clockTimer.getRate()));
        } else {
            logger.info("Pause: Stop isPlaying at rate: " + clockTimer.getRate());
            clockTimer.stop();
            clockTimer.setForceTime((long) clockTimer.getStreamTime());
            labelSpeed.setText("[" + FloatingPointUtils.doubleToFractionStr(clockTimer.getRate())  + "]");
        }
    }

    /**
     * Action to invoke when the user clicks on the stop button.
     */
    @Action
    public void stopAction() {
        logger.info("Stop.");
        clockTimer.setRate(0f);
        clockTimer.setForceTime((long) clockTimer.getStreamTime());
    }

    /**
     * Action to invoke when the user clicks on the shuttle forward button.
     */
    @Action
    @SuppressWarnings("unused")  // Called through actionMap
    public void shuttleForwardAction() {
        logger.info("Shuttle forward.");
        shuttle(1);
    }

    /**
     * Action to invoke when the user clicks on the shuttle back button.
     */
    @Action
    @SuppressWarnings("unused")  // Called through actionMap
    public void shuttleBackAction() {
        logger.info("Shuttle back.");
        shuttle(-1);
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
    @SuppressWarnings("unused")  // Called through actionMap
    public void findAction() {
        if (shiftMask) {
            findOffsetAction();
        } else {

            try {
                logger.info("Finding to " + onsetTextField.getText() + " "
                        + CLOCK_FORMAT.parse(onsetTextField.getText()).getTime());
                clockTimer.setForceTime(CLOCK_FORMAT.parse(onsetTextField.getText()).getTime());
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
            clockTimer.setForceTime(CLOCK_FORMAT.parse(offsetTextField.getText()).getTime());
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
                final long findTextTime = CLOCK_FORMAT.parse(onsetTextField.getText()).getTime();
                final long findOffsetTime = CLOCK_FORMAT.parse(offsetTextField.getText()).getTime();

                final long newWindowPlayStart = findTextTime;
                final long newWindowPlayEnd = (findOffsetTime > newWindowPlayStart)
                        ? findOffsetTime : newWindowPlayStart;
                mixerController.getMixerModel().getNeedleModel().setCurrentTime(newWindowPlayStart);
                mixerController.getMixerModel().getRegionModel().setPlaybackRegion(newWindowPlayStart, newWindowPlayEnd);
            } catch (ParseException e) {
                logger.error("Unable to set region of interest for playback " + e);
            }
        }
    }

    /**
     * Action to invoke when the user clicks on the go back button.
     */
    @Action
    @SuppressWarnings("unused")  // Called through actionMap
    public void goBackAction() {
        try {
            long backTime = -CLOCK_FORMAT.parse(goBackTextField.getText()).getTime();
            long newTime = (long) clockTimer.getClockTime() + backTime;
            logger.info("Jump back by " +  backTime + " From: " + clockTimer.getStreamTime() + " To: " + newTime);
            clockTimer.setForceTime(newTime);

            //Bugzilla bug #179 - undoes the behavior from FogBugz BugzID:721
            // BugzID:721 - After going back - start isPlaying again.
            //playAt(PLAY_RATE);
        } catch (ParseException e) {
            logger.error("Unable to jump back " + e);
        }
    }

    private boolean almostEqual(double value1, double value2, double threshold) {
        return Math.abs(value1 - value2) <= threshold;
    }

    /**
     * Action to invoke when the user clicks on the jog backwards button.
     */
    @Action
    public void jogBackAction() {
        logger.info("Jog back");
        if (!clockTimer.isStopped()) {
            clockTimer.setRate(0f);
        } else {
            syncStreams();
            TracksEditorController tracksEditorController = mixerController.getTracksEditorController();
            double frameRate = frameRateController.getFrameRate();
            long clockTime = (long) clockTimer.getClockTime();
            long stepSize = (long) Math.ceil(MILLI_IN_SEC / frameRate); // step size is in milliseconds
            for (StreamViewer streamViewer : streamViewers) {
                // TODO: Tie offset & duration to stream viewer only and pull it in the track model
                TrackModel trackModel = tracksEditorController.getTrackModel(streamViewer.getIdentifier());
                // We can only use the step function if this frame rate is close enough to the highest frame rate
                if (streamViewer.isStepEnabled() && almostEqual(streamViewer.getFramesPerSecond(), frameRate,
                        ALMOST_EQUAL_FRAME_RATES)) {
                    streamViewer.stepBackward();
                } else if (trackModel != null){
                    // Get the stream time
                    long trackTime = clockTime - trackModel.getOffset();

                    // Notice that the new time is in jogs to frame markers by being modulo step size
                    long newTime = Math.min(Math.max(trackTime - (trackTime % stepSize) - stepSize, 0),
                            trackModel.getDuration());

                    logger.info("Jog back from " + trackTime + " milliseconds to " + newTime + " milliseconds");

                    streamViewer.setCurrentTime(newTime);
                }
                // otherwise we can't step
            }
            // Update the clock timer with the new time
            long newTime = clockTime - (clockTime % stepSize) - stepSize;
            //Force the time in order to update the cell highlighting
            clockTimer.setForceTime(newTime);
            updateCurrentTimeLabelAndNeedle(newTime);
        }
    }

    /**
     * Get the frame rate controller for this video controller
     *
     * @return The frame rate controller
     */
    public FrameRateController getFrameRateController() {
        return frameRateController;
    }

    /**
     * Force sync between streams up to a threshold
     */
    private void syncStreams() {
        long clockTime = (long) clockTimer.getClockTime();
        double frameRate = frameRateController.getFrameRate();
        long stepSize = (long)(MILLI_IN_SEC / frameRate);
        TracksEditorController tracksEditorController = mixerController.getTracksEditorController();
        for (StreamViewer streamViewer : streamViewers) {
            TrackModel trackModel = tracksEditorController.getTrackModel(streamViewer.getIdentifier());
            if (trackModel != null){
                // Get the stream time
                long trackTime = clockTime - trackModel.getOffset();
                // Notice that the new time is in jogs to frame markers by being modulo step size
//                long newTime = Math.min(Math.max(trackTime - (trackTime % stepSize), 0),
//                        trackModel.getDuration());
                // We should sync according to the trackTime and not the newTime the hjog methods
                // will set the new time fot the streams
//                if (Math.abs(newTime - streamViewer.getCurrentTime()) < SYNC_THRESHOLD) {
                if (Math.abs(trackTime - streamViewer.getCurrentTime()) >= ClockTimer.SYNC_THRESHOLD) {
                    streamViewer.setCurrentTime(trackTime);
//                    streamViewer.setCurrentTime(newTime);
                }
            }
        }
    }

    /**
     * Action to invoke when the user clicks on the jog forwards button.
     */
    @Action
    public void jogForwardAction() {
        logger.info("Jog forward");
        if (!clockTimer.isStopped()) {
            clockTimer.setRate(0);
        } else {
            syncStreams();
            double frameRate = frameRateController.getFrameRate();
            long clockTime = (long) clockTimer.getClockTime();
            long stepSize = (long) Math.ceil(MILLI_IN_SEC / frameRate); // step size is in milliseconds
            TracksEditorController tracksEditorController = mixerController.getTracksEditorController();
            for (StreamViewer streamViewer : streamViewers) {
                // TODO: Tie offset & duration to stream viewer only and pull it in the track model
                TrackModel trackModel = tracksEditorController.getTrackModel(streamViewer.getIdentifier());
                if (streamViewer.isStepEnabled() && almostEqual(streamViewer.getFramesPerSecond(), frameRate,
                        ALMOST_EQUAL_FRAME_RATES)) {
                    streamViewer.stepForward();
                } else if (trackModel != null){
                    // Get the stream time
                    long trackTime = clockTime - trackModel.getOffset();

                    // Notice that the new time is in jogs to frame markers by being modulo step size
                    long newTime = Math.min(Math.max(trackTime - (trackTime % stepSize) + stepSize, 0),
                            trackModel.getDuration());

                    logger.info("Jog forward from " + trackTime + " milliseconds to " + newTime + " milliseconds.");

                    streamViewer.setCurrentTime(newTime);
                }
                // otherwise we can't step
            }
            // Update the clock timer with the new time
            long newTime = clockTime - (clockTime % stepSize) + stepSize;
            //Force the time in order to update the cell highlighting
            clockTimer.setForceTime(newTime);
            updateCurrentTimeLabelAndNeedle(newTime);
        }
    }

    /**
     * @param shuttleJump The required rate and direction of the shuttle.
     */
    private void shuttle(int shuttleJump) {
        float currentRate = clockTimer.getRate();
        float nextRate = shuttleRates.nextRate(currentRate, shuttleJump);
        clockTimer.setRate(nextRate);
        logger.info("Changed rate from " + currentRate + " to " + nextRate + " for " + shuttleJump + " jumps.");
    }

    /**
     * Action to invoke when the user clicks on the create new cell button.
     */
    @Action
    @SuppressWarnings("unused")  // Called through actionMap
    public void createNewCellAction() {
        logger.info("New cell");

        // Ensure precise sync up
        long time = getCurrentTime();
        if (!clockTimer.isStopped()) {
            clockTimer.setForceTime(time);
        }

        CreateNewCellController controller = new CreateNewCellController();
        controller.createDefaultCell(true);
    }

    /**
     * Action to invoke when the user clicks on the new cell button.
     */
    @Action
    @SuppressWarnings("unused")  // Called through actionMap
    public void createNewCellAndSetOffsetAction() {
        logger.info("New cell set offset");

        // Ensure precise sync up
        long time = getCurrentTime();
        if (!clockTimer.isStopped()) {
            clockTimer.setForceTime(time);
        }

        new CreateNewCellController(time, true);
    }

    /**
     * Action to invoke when the user clicks on the new cell offset button.
     */
    @Action
    @SuppressWarnings("unused")  // Called through actionMap
    public void pointCellAction() {
        logger.info("Set new cell offset");

        // Set precise clock
        long time = getCurrentTime();
        if (!clockTimer.isStopped()) {
            clockTimer.setForceTime(time);
        }

        new CreateNewCellController(time, false);
        new SetNewCellStopTimeController(time);
        setOffsetField(time);
    }

    @Override
    public void propertyChange(final PropertyChangeEvent e) {
        Object source = e.getSource();
        if (source instanceof NeedleModel) {
            handleNeedleChange(((NeedleModel) source).getNeedle());
        } else if (source instanceof ViewportModel) {
            handleViewportChange(((ViewportModel) source).getViewport());
        } else if (source instanceof RegionModel) {
            handleRegionChange(((RegionModel) source).getRegion());
        }
    }

    public ClockTimer getClockTimer() {
        return clockTimer;
    }

    class VideoInfo{
        private Identifier id;
        private File file;
        private Plugin plugin;
        private StreamViewer stream;

        private VideoInfo(Identifier id, File file, Plugin plugin, StreamViewer stream){
            this.id = id;
            this.file = file;
            this.plugin = plugin;
            this.stream = stream;
        }

       public Identifier getID(){ return id; }
       public File getFile(){ return file; }
       public Plugin getPlugin(){ return plugin; }
       public StreamViewer getStream(){ return stream; }
    }
}
