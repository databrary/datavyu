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
package org.datavyu.controllers.component;

import com.apple.eawt.event.*;
import com.google.common.collect.Maps;
import com.sun.jna.Platform;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.text.StrSubstitutor;
import org.datavyu.Datavyu;
import org.datavyu.event.component.*;
import org.datavyu.event.component.TracksControllerEvent.EventType;
import org.datavyu.models.Identifier;
import org.datavyu.models.component.*;
import org.datavyu.plugins.CustomActions;
import org.datavyu.views.component.TrackPainter;

import javax.swing.*;
import javax.swing.Box.Filler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * This class manages tracks information
 */
public final class MixerController implements PropertyChangeListener,
        CarriageEventListener, AdjustmentListener, TimescaleListener {

    public static final long DEFAULT_DURATION = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);

    private static final double DEFAULT_ZOOM = 0.0;

    private final int V_SCROLL_WIDTH = 17;

    private static final int H_SCROLL_HEIGHT = 17;

    private static final int REGION_EDGE_PADDING = 5;

    private static final int MIXER_MIN_WIDTH = 785;

    private static final int FILLER_DEPTH_ORDER = 0;

    private static final int TIME_SCALE_DEPTH_ORDER = 5;

    private static final int TRACKS_DEPTH_ORDER = 10;

    private static final int REGION_DEPTH_ORDER = 20;

    private static final int NEEDLE_DEPTH_ORDER = 30;

    private static final int MARKER_DEPTH_ORDER = 50;

    private static final int TRACKS_SCROLL_BAR_DEPTH_ORDER = 60;

    private final int TRACKS_SCROLL_BAR_RANGE = 1000000;

    /** Zoom icon */
    private final ImageIcon zoomIcon = new ImageIcon(getClass().getResource("/icons/magnifier.png"));

    /** Master mixer to listen to */
    private final MixerModel mixerModel;

    /** Viewport model */
    private final ViewportModel viewportModel;

    /** Region model */
    private final RegionModel regionModel;

    /** Listens and processes gestures on Mac OS X */
    private final OSXGestureListener osxGestureListener = Platform.isMac() ? new OSXGestureListener() : null;

    /** Tracks panel */
    private JPanel tracksPanel;

    /** Scroll pane with track information */
    private JScrollPane tracksScrollPane;

    /** Zoom value in (0, 1.0) where increasing values represent "zooming in" */
    private double zoomValue = DEFAULT_ZOOM;

    /** Listeners for track controller events */
    private EventListenerList eventListenerList;

    /** Controller managing the time scale */
    private TimescaleController timescaleController;

    /** Controller managing the timing needle */
    private NeedleController needleController;

    /** Controller managing a selected region */
    private RegionController regionController;

    /** Controller managing tracks */
    private TracksEditorController tracksEditorController;

    /** Bookmark (create snap point) button */
    private JButton bookmarkButton;

    /** Button for locking and unlocking all tracks */
    private JToggleButton lockToggle;

    /** Tracks horizontal scroll bar */
    private JScrollBar tracksScrollBar;

    /** Do we update the tracks scroll bar? */
    private boolean isUpdatingTracksScrollBar = false;

    /** Zoom slider */
    private JSlider zoomSlide;

    /** Update the zoom slider? */
    private boolean isUpdatingZoomSlide = false;

    /** Enable highlighting */
    private JButton enableHighlight;

    /** Enable highlighting and focus */
    private JButton enableHighlightAndFocus;

    /**
     * Create a new MixerController.
     */
    public MixerController() {
        mixerModel = new MixerModelImpl();

        viewportModel = mixerModel.getViewportModel();
        viewportModel.addPropertyChangeListener(this);

        regionModel = mixerModel.getRegionModel();
        regionModel.addPropertyChangeListener(this);

        runInEDT(new Runnable() {
            @Override
            public void run() {
                initView();
            }
        });
    }

    private static void runInEDT(final Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    private void initView() {
        eventListenerList = new EventListenerList();

        // Set up the root panel
        tracksPanel = new JPanel();
        tracksPanel.setLayout(new MigLayout("ins 0","[left|left|left]rel push[right|right] [left|left]", ""));
        tracksPanel.setBackground(Color.WHITE);

        if (Platform.isMac()) {
            osxGestureListener.register(tracksPanel);
        }

        // Menu buttons
        lockToggle = new JToggleButton("Lock all");
        lockToggle.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                lockToggleHandler(e);
            }
        });
        lockToggle.setName("lockToggleButton");

        bookmarkButton = new JButton("Add Bookmark");
        bookmarkButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                addBookmarkHandler();
            }
        });
        bookmarkButton.setEnabled(false);
        bookmarkButton.setName("bookmarkButton");

        JButton snapRegion = new JButton("Snap Region");
        snapRegion.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                snapRegionHandler(e);
            }
        });
        snapRegion.setName("snapRegionButton");

        JButton clearRegion = new JButton("Clear Region");
        clearRegion.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                clearRegionHandler(e);
            }
        });
        clearRegion.setName("clearRegionButton");

        enableHighlight = new JButton("Enable Cell Highlighting");
        enableHighlight.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                enableHighlightHandler(e);
            }
        });
        enableHighlight.setName("enableHighlightButton");

        enableHighlightAndFocus = new JButton("Enable Highlight and Focus");
        enableHighlightAndFocus.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                enableHighlightAndFocusHandler(e);
            }
        });
        enableHighlightAndFocus.setName("enableHighlightAndFocusButton");

        zoomSlide = new JSlider(JSlider.HORIZONTAL, 1, 1000, 1);
        zoomSlide.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {

                if (!isUpdatingZoomSlide && zoomSlide.getValueIsAdjusting()) {
                    try {
                        isUpdatingZoomSlide = true;
                        zoomValue = (double) (zoomSlide.getValue() - zoomSlide.getMinimum())
                                           / (zoomSlide.getMaximum() - zoomSlide.getMinimum() + 1);
                        viewportModel.setViewportZoom(zoomValue, needleController.getNeedleModel().getCurrentTime());
                    } finally {
                        isUpdatingZoomSlide = false;
                    }
                }
            }
        });
        zoomSlide.setName("zoomSlider");
        zoomSlide.setBackground(tracksPanel.getBackground());

        JButton zoomRegionButton = new JButton("", zoomIcon);
        zoomRegionButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                zoomToRegion(e);
            }
        });
        zoomRegionButton.setName("zoomRegionButton");

        tracksPanel.add(bookmarkButton);
        tracksPanel.add(snapRegion);
        tracksPanel.add(lockToggle);
        tracksPanel.add(zoomRegionButton);
        tracksPanel.add(zoomSlide, "wrap");
        tracksPanel.add(enableHighlight);
        tracksPanel.add(enableHighlightAndFocus);
        tracksPanel.add(clearRegion, "wrap");

        timescaleController = new TimescaleController(mixerModel);
        timescaleController.addTimescaleEventListener(this);
        needleController = new NeedleController(this, mixerModel);
        regionController = new RegionController(mixerModel);
        tracksEditorController = new TracksEditorController(this, mixerModel);

        needleController.setTimescaleTransitionHeight(
                timescaleController.getTimescaleModel()
                        .getZoomWindowToTrackTransitionHeight());
        needleController.setZoomIndicatorHeight(
                timescaleController.getTimescaleModel()
                        .getZoomWindowIndicatorHeight());

        // Set up the layered pane
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(new MigLayout("fillx, ins 0"));

        final int layeredPaneHeight = 272;
        final int timescaleViewHeight = timescaleController.getTimescaleModel().getHeight();

        final int needleHeadHeight = (int) Math.ceil(NeedleConstants.NEEDLE_HEAD_HEIGHT);
        final int tracksScrollPaneY = needleHeadHeight + 1;
        final int timescaleViewY = layeredPaneHeight - H_SCROLL_HEIGHT - timescaleViewHeight;
        final int tracksScrollPaneHeight = timescaleViewY - tracksScrollPaneY;
        final int tracksScrollBarY = timescaleViewY + timescaleViewHeight;
        final int needleAndRegionMarkerHeight = (timescaleViewY
                + timescaleViewHeight
                - timescaleController.getTimescaleModel()
                .getZoomWindowIndicatorHeight()
                - timescaleController.getTimescaleModel()
                .getZoomWindowToTrackTransitionHeight() + 1);

        // Set up filler component responsible for horizontal resizing of the layout.
        {
            // Null args; let layout manager handle sizes.
            Box.Filler filler = new Filler(null, null, null);
            filler.setName("Filler");
            filler.addComponentListener(new SizeHandler());

            Map<String, String> constraints = Maps.newHashMap();
            constraints.put("wmin", Integer.toString(MIXER_MIN_WIDTH));

            // TODO Could probably use this same component to handle vertical resizing...
            String template = "id filler, h 0!, grow 100 0, wmin ${wmin}, cell 0 0 ";
            StrSubstitutor sub = new StrSubstitutor(constraints);

            layeredPane.setLayer(filler, FILLER_DEPTH_ORDER);
            layeredPane.add(filler, sub.replace(template), FILLER_DEPTH_ORDER);
        }

        // Set up the timescale layout
        {
            JComponent timescaleView = timescaleController.getTimescaleComponent();

            Map<String, String> constraints = Maps.newHashMap();
            constraints.put("x", Integer.toString(TimescaleConstants.XPOS_ABS));
            constraints.put("y", Integer.toString(timescaleViewY));

            // Calculate padding from the right
            int rightPad = (int) (RegionConstants.RMARKER_WIDTH + V_SCROLL_WIDTH
                                 + REGION_EDGE_PADDING);
            constraints.put("x2", "(filler.w-" + rightPad + ")");
            constraints.put("y2", "(tscale.y+${height})");
            constraints.put("height", Integer.toString(timescaleViewHeight));

            String template = "id tscale, pos ${x} ${y} ${x2} ${y2}";
            StrSubstitutor sub = new StrSubstitutor(constraints);

            // Must call setLayer first.
            layeredPane.setLayer(timescaleView, TIME_SCALE_DEPTH_ORDER);
            layeredPane.add(timescaleView, sub.replace(template), TIME_SCALE_DEPTH_ORDER);
        }

        // Set up the scroll pane's layout.
        {
            tracksScrollPane = new JScrollPane(tracksEditorController.getView());
            tracksScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            tracksScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            tracksScrollPane.setBorder(BorderFactory.createEmptyBorder());
            tracksScrollPane.setName("jScrollPane");

            Map<String, String> constraints = Maps.newHashMap();
            constraints.put("x", "0");
            constraints.put("y", Integer.toString(tracksScrollPaneY));
            constraints.put("x2", "(filler.w-" + REGION_EDGE_PADDING + ")");
            constraints.put("height", Integer.toString(tracksScrollPaneHeight));

            String template = "pos ${x} ${y} ${x2} n, h ${height}!";
            StrSubstitutor sub = new StrSubstitutor(constraints);

            layeredPane.setLayer(tracksScrollPane, TRACKS_DEPTH_ORDER);
            layeredPane.add(tracksScrollPane, sub.replace(template), TRACKS_DEPTH_ORDER);
        }

        // Create the region markers and set up the layout.
        {
            JComponent regionView = regionController.getView();

            Map<String, String> constraints = Maps.newHashMap();

            int x = (int) (TrackConstants.HEADER_WIDTH - RegionConstants.RMARKER_WIDTH);
            constraints.put("x", Integer.toString(x));
            constraints.put("y", "0");

            // Padding from the right
            int rightPad = REGION_EDGE_PADDING + V_SCROLL_WIDTH - 2;

            constraints.put("x2", "(filler.w-" + rightPad + ")");
            constraints.put("height", Integer.toString(needleAndRegionMarkerHeight));

            String template = "pos ${x} ${y} ${x2} n, h ${height}::";
            StrSubstitutor sub = new StrSubstitutor(constraints);

            layeredPane.setLayer(regionView, REGION_DEPTH_ORDER);
            layeredPane.add(regionView, sub.replace(template), REGION_DEPTH_ORDER);
        }

        // Set up the timing needle's layout
        {
            JComponent needleView = needleController.getNeedleComponent();

            Map<String, String> constraints = Maps.newHashMap();

            int x = (int) (TrackConstants.HEADER_WIDTH
                    - NeedleConstants.NEEDLE_HEAD_WIDTH
                    + NeedleConstants.NEEDLE_WIDTH);
            constraints.put("x", Integer.toString(x));
            constraints.put("y", "0");

            // Padding from the right
            int rightPad = REGION_EDGE_PADDING + V_SCROLL_WIDTH - 1;

            constraints.put("x2", "(filler.w-" + rightPad + ")");
            constraints.put("height",
                    Integer.toString(
                            needleAndRegionMarkerHeight
                                    + timescaleController.getTimescaleModel()
                                    .getZoomWindowToTrackTransitionHeight()
                                    + timescaleController.getTimescaleModel()
                                    .getZoomWindowIndicatorHeight() - 1));

            String template = "pos ${x} ${y} ${x2} n, h ${height}::";
            StrSubstitutor sub = new StrSubstitutor(constraints);

            layeredPane.setLayer(needleView, NEEDLE_DEPTH_ORDER);
            layeredPane.add(needleView, sub.replace(template), NEEDLE_DEPTH_ORDER);
        }

        // Set up the snap marker's layout
        {
            JComponent markerView = tracksEditorController.getMarkerView();

            Map<String, String> constraints = Maps.newHashMap();
            constraints.put("x", Integer.toString(TimescaleConstants.XPOS_ABS));
            constraints.put("y", Integer.toString(needleHeadHeight + 1));

            // Padding from the right
            int rightPad = REGION_EDGE_PADDING + V_SCROLL_WIDTH - 1;

            constraints.put("x2", "(filler.w-" + rightPad + ")");
            constraints.put("height", Integer.toString(needleAndRegionMarkerHeight - needleHeadHeight - 1));

            String template = "pos ${x} ${y} ${x2} n, h ${height}::";
            StrSubstitutor sub = new StrSubstitutor(constraints);

            layeredPane.setLayer(markerView, MARKER_DEPTH_ORDER);
            layeredPane.add(markerView, sub.replace(template), MARKER_DEPTH_ORDER);
        }

        // Set up the tracks horizontal scroll bar
        {
            tracksScrollBar = new JScrollBar(Adjustable.HORIZONTAL);
            tracksScrollBar.setValues(0, TRACKS_SCROLL_BAR_RANGE, 0, TRACKS_SCROLL_BAR_RANGE);
            tracksScrollBar.setUnitIncrement(TRACKS_SCROLL_BAR_RANGE / 20);
            tracksScrollBar.setBlockIncrement(TRACKS_SCROLL_BAR_RANGE / 2);
            tracksScrollBar.addAdjustmentListener(this);
            tracksScrollBar.setValueIsAdjusting(false);
            tracksScrollBar.setVisible(false);
            tracksScrollBar.setName("horizontalScrollBar");

            Map<String, String> constraints = Maps.newHashMap();
            constraints.put("x", Integer.toString(TimescaleConstants.XPOS_ABS));
            constraints.put("y", Integer.toString(tracksScrollBarY));

            int rightPad = (int) (RegionConstants.RMARKER_WIDTH
                    + V_SCROLL_WIDTH + REGION_EDGE_PADDING);
            constraints.put("x2", "(filler.w-" + rightPad + ")");
            constraints.put("height", Integer.toString(H_SCROLL_HEIGHT));

            String template = "pos ${x} ${y} ${x2} n, h ${height}::";
            StrSubstitutor sub = new StrSubstitutor(constraints);

            layeredPane.setLayer(tracksScrollBar, TRACKS_SCROLL_BAR_DEPTH_ORDER);
            layeredPane.add(tracksScrollBar, sub.replace(template), TRACKS_SCROLL_BAR_DEPTH_ORDER);
        }

        {
            Map<String, String> constraints = Maps.newHashMap();
            constraints.put("span", "6");
            constraints.put("width", Integer.toString(MIXER_MIN_WIDTH));
            constraints.put("height", Integer.toString(layeredPaneHeight));

            String template = "growx, span ${span}, w ${width}::, h ${height}::, wrap";
            StrSubstitutor sub = new StrSubstitutor(constraints);

            tracksPanel.add(layeredPane, sub.replace(template));
        }

        tracksPanel.validate();
    }

    /**
     * @return the panel containing the tracks interface.
     */
    public JPanel getTracksPanel() {
        return tracksPanel;
    }

    /**
     * Add a new track to the interface.
     *
     * @param id           Identifier of the track.
     * @param icon         Icon associated with the track.
     * @param mediaPath    Absolute path to the media file.
     * @param duration     The total duration of the track in milliseconds.
     * @param offset       The amount of playback offset in milliseconds.
     * @param trackPainter The track painter to use.
     */
    public void addNewTrack(final Identifier id, final ImageIcon icon,
                            final File mediaPath, final long duration,
                            final long offset, final TrackPainter trackPainter) {

        // Check if the scale needs to be updated
        final long trackEnd = duration + offset;
        final ViewportState viewport = viewportModel.getViewport();

        if ((trackEnd > viewport.getMaxEnd()) || ((tracksEditorController.numberOfTracks() == 0) && (trackEnd > 0))) {
            viewportModel.setViewportMaxEnd(trackEnd, true);
            regionModel.resetPlaybackRegion();
        }

        tracksEditorController.addNewTrack(id, icon, mediaPath, duration, offset, this, trackPainter);
        tracksScrollPane.validate();

        updateGlobalLockToggle();
    }

    /**
     * Clears the region of interest and zooms all the way out.
     */
    public void clearRegionAndZoomOut() {
        clearRegionOfInterest();
        zoomToRegion(null);
    }

    /**
     * Bind track actions to a data viewer.
     *
     * @param trackId Identifier of the track
     * @param actions Actions to bind with
     */
    public void bindTrackActions(final Identifier trackId, final CustomActions actions) {
        if (actions == null) {
            return;
        }
        runInEDT(new Runnable() {
            @Override
            public void run() {
                tracksEditorController.bindTrackActions(trackId, actions);
            }
        });
    }

    /**
     * Used to set up the track interface.
     *
     * @param trackId  Track identifier.
     * @param bookmarks Bookmark position in milliseconds.
     * @param lock True if track movement is locked, false otherwise.
     */
    public void setTrackInterfaceSettings(final Identifier trackId, final List<Long> bookmarks, final boolean lock) {
        runInEDT(new Runnable() {
            @Override
            public void run() {
                tracksEditorController.setBookmarkPositions(trackId, bookmarks);
                tracksEditorController.setMovementLock(trackId, lock);
            }
        });
    }

    /**
     * For backwards compatibility; used the set up the track interface. If
     * there are multiple tracks identified by the same media path, only the
     * first track found is used.
     *
     * @param mediaPath Absolute path to the media file.
     * @param bookmarks Bookmark position in milliseconds.
     * @param lock      True if track movement is locked, false otherwise.
     */
    @Deprecated
    public void setTrackInterfaceSettings(final String mediaPath, final List<Long> bookmarks, final boolean lock) {
        runInEDT(new Runnable() {
            @Override
            public void run() {
                tracksEditorController.setBookmarkPositions(mediaPath, bookmarks);
                tracksEditorController.setMovementLock(mediaPath, lock);
            }
        });
    }

    /**
     * Zooms into the displayed region and re-adjusts the timing needle accordingly
     *
     * @param evt
     */
    private void zoomToRegion(final ActionEvent evt) {
        final ViewportState viewport = viewportModel.getViewport();
        final RegionState region = regionModel.getRegion();

        if (region.getRegionDuration() >= 1) {
            final int percentOfRegionToPadOutsideMarkers = 5;
            assert (percentOfRegionToPadOutsideMarkers >= 0) && (percentOfRegionToPadOutsideMarkers <= 100);

            final long displayedAreaStart = Math.max(region.getRegionStart()
                    - (region.getRegionDuration()
                    * percentOfRegionToPadOutsideMarkers / 100), 0);
            final long displayedAreaEnd = Math.min(region.getRegionEnd()
                    + (region.getRegionDuration()
                    * percentOfRegionToPadOutsideMarkers / 100),
                    viewport.getMaxEnd());

            viewportModel.setViewportWindow(displayedAreaStart,
                    displayedAreaEnd);
            needleController.setCurrentTime(region.getRegionStart());
        }
    }

    /**
     * Remove from track panel.
     *
     * @param trackId identifier of the track to remove.
     */
    public void deregisterTrack(final Identifier trackId) {
        tracksEditorController.removeTrack(trackId, this);

        // Update tracks panel display
        tracksScrollPane.validate();

        updateGlobalLockToggle();
    }

    /**
     * Removes all track components from this controller and resets components.
     */
    public void removeAll() {
        tracksEditorController.removeAllTracks();

        viewportModel.resetViewport();
        regionModel.resetPlaybackRegion();
        needleController.setCurrentTime(0);

        tracksScrollPane.validate();

        tracksPanel.validate();
        tracksPanel.repaint();

        updateGlobalLockToggle();
    }

    public TrackModel getTrackModel(final Identifier id) {
        return tracksEditorController.getTrackModel(id);
    }

    public MixerModel getMixerModel() {
        return mixerModel;
    }

    /**
     * @return NeedleController.
     */
    public NeedleController getNeedleController() {
        return needleController;
    }

    /**
     * @return RegionController.
     */
    public RegionController getRegionController() {
        return regionController;
    }

    /**
     * @return TimescaleController.
     */
    public TimescaleController getTimescaleController() {
        return timescaleController;
    }

    /**
     * @return TracksEditorController.
     */
    public TracksEditorController getTracksEditorController() {
        return tracksEditorController;
    }

    private void updateZoomSlide(final ViewportState viewport) {
        assert SwingUtilities.isEventDispatchThread();

        if (isUpdatingZoomSlide) {
            return;
        }

        try {
            isUpdatingZoomSlide = true;

            zoomSlide.setValue((int) Math.round(
                    (viewport.getZoomLevel()
                            * (zoomSlide.getMaximum() - zoomSlide.getMinimum()
                            + 1)) + zoomSlide.getMinimum()));
        } finally {
            isUpdatingZoomSlide = false;
        }
    }

    /**
     * Update scroll bar values.
     */
    private void updateTracksScrollBar(final ViewportState viewport) {
        assert SwingUtilities.isEventDispatchThread();

        if (isUpdatingTracksScrollBar) {
            return;
        }

        try {
            isUpdatingTracksScrollBar = true;

            final int startValue = (int) Math.round(
                    (double) viewport.getViewStart() * TRACKS_SCROLL_BAR_RANGE
                            / viewport.getMaxEnd());
            final int extentValue = (int) Math.round(
                    (double) (viewport.getViewDuration())
                            * TRACKS_SCROLL_BAR_RANGE / viewport.getMaxEnd());

            tracksScrollBar.setValues(startValue, extentValue, 0,
                    TRACKS_SCROLL_BAR_RANGE);
            tracksScrollBar.setUnitIncrement(extentValue / 20);
            tracksScrollBar.setBlockIncrement(extentValue / 2);
            tracksScrollBar.setVisible((viewport.getViewDuration())
                    < viewport.getMaxEnd());
        } finally {
            isUpdatingTracksScrollBar = false;
            tracksPanel.validate();
        }
    }

    /**
     * Handles the event for adding a temporal bookmark to selected tracks.
     */
    private void addBookmarkHandler() {
        tracksEditorController.addTemporalBookmarkToSelected(
                needleController.getNeedleModel().getCurrentTime());
    }

    /**
     * Handles the event for toggling the snap functionality on and off.
     *
     * @param e expecting the event to be generated from a JToggleButton
     */
    private void snapRegionHandler(final ActionEvent e) {
        Datavyu.getVideoController().setRegionOfInterestAction();

    }

    /**
     * Handles the event for clearing the snap region.
     *
     * @param e The event that triggered this action.
     */
    private void clearRegionHandler(final ActionEvent e) {
        clearRegionOfInterest();
    }

    /**
     * Enables/Disables cell highlighting.
     *
     * @param e The event that triggered this action.
     */
    private void enableHighlightHandler(final ActionEvent e) {
        Datavyu.getVideoController().toggleCellHighlighting();

        if (!Datavyu.getVideoController().getCellHighlighting()) {
            enableHighlight.setText("Enable Cell Highlighting");
        } else {
            enableHighlight.setText("Disable Cell Highlighting");
        }

        Datavyu.getProjectController().getSpreadSheetPanel().redrawCells();
    }

    public void enableHighlightAndFocusHandler(final ActionEvent e) {
        Datavyu.getVideoController().toggleCellHighlightingAutoFocus();
        Datavyu.getVideoController().setCellHighlighting(Datavyu.getVideoController().getCellHighlightAndFocus());

        if (!Datavyu.getVideoController().getCellHighlighting()) {
            enableHighlightAndFocus.setText("Enable Highlight and Focus");
        } else {
            enableHighlightAndFocus.setText("Disable Highlight and Focus");
        }

        Datavyu.getProjectController().getSpreadSheetPanel().redrawCells();
    }

    /**
     * Clears the region of interest.
     */
    public void clearRegionOfInterest() {
        regionModel.resetPlaybackRegion();
    }

    /**
     * Handles the event for toggling movement of tracks on and off.
     *
     * @param e the event to handle
     */
    private void lockToggleHandler(final ActionEvent e) {
        JToggleButton toggle = (JToggleButton) e.getSource();
        tracksEditorController.setLockedState(toggle.isSelected());
        updateGlobalLockToggle();
    }

    /**
     * Handles the event for scrolling the tracks interface horizontally.
     *
     * @param e the event to handle
     */
    public void adjustmentValueChanged(final AdjustmentEvent e) {

        if (isUpdatingTracksScrollBar) {
            return;
        }

        final ViewportState viewport = viewportModel.getViewport();

        final int startValue = tracksScrollBar.getValue();

        assert tracksScrollBar.getMinimum() == 0;

        final long newWindowStart = (long) Math.round((double) startValue
                / tracksScrollBar.getMaximum() * viewport.getMaxEnd());
        final long newWindowEnd = newWindowStart + viewport.getViewDuration()
                - 1;

        viewportModel.setViewportWindow(newWindowStart, newWindowEnd);

        tracksPanel.repaint();
    }

    /**
     * TrackPainter recorded a change in the track's offset using the mouse.
     *
     * @param e the event to handle
     */
    public void offsetChanged(final CarriageEvent e) {
        final boolean wasOffsetChanged = tracksEditorController.setTrackOffset(
                e.getTrackId(), e.getOffset(), e.getTime());
        final CarriageEvent newEvent;

        if (wasOffsetChanged) {
            final long newOffset = tracksEditorController.getTrackModel(
                    e.getTrackId()).getOffset();
            newEvent = new CarriageEvent(e.getSource(), e.getTrackId(),
                    newOffset, e.getMarkers(), e.getDuration(),
                    e.getTime(), e.getEventType(),
                    e.hasModifiers());
        } else {
            newEvent = e;
        }

        fireTracksControllerEvent(EventType.CARRIAGE_EVENT, newEvent);
        tracksPanel.invalidate();
        tracksPanel.repaint();
    }

    /**
     * Track is requesting current temporal position to create a bookmark.
     *
     * @param carriageEvent the event to handle
     */
    public void requestMarker(final CarriageEvent carriageEvent) {
        TrackController trackController = (TrackController) carriageEvent.getSource();
        trackController.addReferencedMarker(needleController.getNeedleModel().getCurrentTime());

        CarriageEvent newEvent = new CarriageEvent(carriageEvent.getSource(),
                carriageEvent.getTrackId(), carriageEvent.getOffset(), trackController.getMarkers(),
                carriageEvent.getDuration(), carriageEvent.getTime(),
                CarriageEvent.EventType.MARKER_CHANGED, carriageEvent.hasModifiers());

        fireTracksControllerEvent(EventType.CARRIAGE_EVENT, newEvent);
    }

    /**
     * Track is requesting for bookmark to be saved.
     *
     * @param carriageEvent the event to handle
     */
    public void saveMarker(final CarriageEvent carriageEvent) {
        fireTracksControllerEvent(EventType.CARRIAGE_EVENT, carriageEvent);
    }

    /**
     * Track lock state changed.
     *
     * @param e the event to handle
     */
    public void lockStateChanged(final CarriageEvent e) {
        fireTracksControllerEvent(EventType.CARRIAGE_EVENT, e);
        updateGlobalLockToggle();
    }

    /**
     * A track's selection state was changed.
     *
     * @param e the event to handle
     */
    public void selectionChanged(final CarriageEvent e) {
        bookmarkButton.setEnabled(tracksEditorController.hasSelectedTracks());
    }

    public void jumpToTime(final TimescaleEvent e) {
        fireTracksControllerEvent(EventType.TIMESCALE_EVENT, e);
    }

    /**
     * Register listeners who are interested in events from this class.
     *
     * @param listener the listener to register
     */
    public void addTracksControllerListener(final TracksControllerListener listener) {
        synchronized (this) {
            eventListenerList.add(TracksControllerListener.class, listener);
        }
    }

    /**
     * De-register listeners from receiving events from this class.
     *
     * @param listener the listener to remove
     */
    public void removeTracksControllerListener(final TracksControllerListener listener) {
        synchronized (this) {
            eventListenerList.remove(TracksControllerListener.class, listener);
        }
    }

    private void updateGlobalLockToggle() {

        if (tracksEditorController.isAnyTrackUnlocked()) {
            lockToggle.setSelected(false);
            lockToggle.setText("Lock all");
        } else {
            lockToggle.setSelected(true);
            lockToggle.setText("Unlock all");
        }
    }

    /**
     * Used to fire a new event informing listeners about new child component
     * events.
     *
     * @param tracksEvent The event to handle
     * @param eventObject The event object to repackage
     */
    private void fireTracksControllerEvent(final EventType tracksEvent, final EventObject eventObject) {
        TracksControllerEvent e = new TracksControllerEvent(this, tracksEvent, eventObject);
        Object[] listeners = eventListenerList.getListenerList();
        synchronized (this) {
            // The listener list contains the listening class and then the listener instance
            for (int iListener = 0; iListener < listeners.length; iListener += 2) {
                if (listeners[iListener] == TracksControllerListener.class) {
                    ((TracksControllerListener) listeners[iListener + 1])
                            .tracksControllerChanged(e);
                }
            }
        }
    }

    private void handleViewportChanged(final ViewportState newViewport) {
        runInEDT(new Runnable() {
            @Override
            public void run() {
                updateZoomSlide(newViewport);
                updateTracksScrollBar(newViewport);
                tracksScrollPane.repaint();
            }
        });
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {

        if (evt.getSource() == mixerModel.getViewportModel()) {
            final ViewportState newViewport =
                    (evt.getNewValue() instanceof ViewportState)
                            ? (ViewportState) evt.getNewValue() : null;
            handleViewportChanged(newViewport);
        }
    }

    private void handleResize() {
        final ViewportState viewport = viewportModel.getViewport();

        if (Double.isNaN(viewport.getResolution())) {
            viewportModel.setViewport(viewport.getViewStart(),
                    viewport.getViewEnd(), viewport.getMaxEnd(),
                    timescaleController.getTimescaleComponent().getWidth());
        } else {
            viewportModel.resizeViewport(viewportModel.getViewport()
                    .getViewStart(), timescaleController.getTimescaleComponent().getWidth());
        }
    }

    /**
     * Handles component resizing
     */
    private final class SizeHandler extends ComponentAdapter {
        @Override
        public void componentResized(final ComponentEvent e) {
            handleResize();
        }
    }

    private class OSXGestureListener implements MagnificationListener,
            GesturePhaseListener, SwipeListener {

        /**
         * Cumulative sum of the current zoom gesture, where positive values
         * indicate zooming in (enlarging) and negative values indicate zooming
         * out (shrinking). On a 2009 MacBook Pro, pinch-and-zooming from
         * corner-to-corner of the trackpad will result in a total sum of
         * approximately +3.0 (zooming in) or -3.0 (zooming out).
         */
        private double osxMagnificationGestureSum = 0;

        /**
         * Relative zoom when the magnification gesture began.
         */
        private double osxMagnificationGestureInitialZoomSetting;

        void register(final JComponent component) {
            GestureUtilities.addGestureListenerTo(tracksPanel, osxGestureListener);
        }

        /**
         * Invoked when a magnification gesture ("pinch and squeeze") is performed by the user on Mac OS X.
         *
         * @param event contains the scale of the magnification
         */
        @Override
        public void magnify(final MagnificationEvent event) {
            osxMagnificationGestureSum += event.getMagnification();
            // Amount of the pinch-and-squeeze gesture required to perform a full zoom in the mixer
            final double fullZoomMotion = 2.0;
            final double newZoomSetting = Math.min(Math.max(osxMagnificationGestureInitialZoomSetting
                            + (osxMagnificationGestureSum / fullZoomMotion), 0.0), 1.0);
            viewportModel.setViewportZoom(newZoomSetting, needleController.getNeedleModel().getCurrentTime());
        }

        /**
         * Indicates that the user has started performing a gesture on Mac OS X.
         */
        @Override
        public void gestureBegan(final GesturePhaseEvent e) {
            osxMagnificationGestureSum = 0;
            osxMagnificationGestureInitialZoomSetting = viewportModel.getViewport().getZoomLevel();
        }

        /**
         * Indicates that the user has finished performing a gesture on Mac OS X.
         */
        @Override
        public void gestureEnded(final GesturePhaseEvent e) {
        }

        @Override
        public void swipedDown(final SwipeEvent e) {
        }

        @Override
        public void swipedLeft(final SwipeEvent e) {
            swipeHorizontal(false);
        }

        @Override
        public void swipedRight(final SwipeEvent e) {
            swipeHorizontal(true);
        }

        private void swipeHorizontal(final boolean swipeLeft) {

            // The number of horizontal swipe actions needed to move the scroll bar along by the visible amount (i.e. a page left/right action)
            final int swipesPerVisibleAmount = 5;
            final int newValue = tracksScrollBar.getValue()
                    + ((swipeLeft ? -1 : 1) * tracksScrollBar.getVisibleAmount()
                    / swipesPerVisibleAmount);
            tracksScrollBar.setValue(Math.max(Math.min(newValue,
                    tracksScrollBar.getMaximum() - tracksScrollBar.getVisibleAmount()), tracksScrollBar.getMinimum()));
        }

        @Override
        public void swipedUp(final SwipeEvent e) {
        }
    }
}
