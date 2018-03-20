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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.datavyu.event.component.CarriageEvent;
import org.datavyu.event.component.CarriageEventAdapter;
import org.datavyu.event.component.CarriageEventListener;
import org.datavyu.event.component.TrackMouseEventListener;
import org.datavyu.models.Identifier;
import org.datavyu.models.component.*;
import org.datavyu.plugins.CustomActions;
import org.datavyu.plugins.ViewerStateListener;
import org.datavyu.views.component.TrackPainter;
import org.datavyu.views.component.TracksEditorPainter;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Manages multiple TrackController instances.
 */
public final class TracksEditorController implements TrackMouseEventListener {

    /** Main UI panel */
    private JPanel editingPanel;

    /** UI component for displaying a snap position */
    private final SnapMarkerController snapMarkerController;

    /** List of track controllers */
    private final Map<Identifier, TrackController> tracks;

    private final MixerController mixerController;

    private final MixerModel mixerModel;

    /** Handles the selection model for tracks */
    private final CarriageSelection selectionHandler;

    /**
     * Create a new tracks editor controller
     */
    TracksEditorController(final MixerController mixerController, final MixerModel mixerModel) {
        tracks = Maps.newLinkedHashMap();
        this.mixerController = mixerController;
        this.mixerModel = mixerModel;
        snapMarkerController = new SnapMarkerController(mixerModel);
        selectionHandler = new CarriageSelection();
        initView();
    }

    /**
     * Initialise UI elements.
     */
    private void initView() {
        editingPanel = new TracksEditorPainter();
    }

    /**
     * @return Main tracks editor view
     */
    public JComponent getView() {
        return editingPanel;
    }

    /**
     * @return The snap marker view
     */
    public JComponent getMarkerView() {
        return snapMarkerController.getView();
    }

    public long getMinTime() {
        long minTime = Long.MAX_VALUE;
        for (TrackController trackController : tracks.values()) {
            minTime = Math.min(minTime, trackController.getOffset());
        }
        return minTime;
    }

    public long getMaxTime() {
        long maxTime = Long.MIN_VALUE;
        for (TrackController trackController : tracks.values()) {
            maxTime = Math.max(maxTime, trackController.getOffset() + trackController.getDuration());
        }
        return maxTime;
    }

    /**
     * Adds a new track to the interface
     *
     * @param icon         Icon associated with the track
     * @param trackId      Track identifier
     * @param mediaPath    Path to the media file
     * @param duration     Duration of the track in milliseconds
     * @param offset       Track offset in milliseconds
     * @param listener     Register the listener interested in {@link CarriageEvent}.
     *                     Null if uninterested
     * @param trackPainter The track painter to use
     */
    public void addNewTrack(final Identifier trackId, final ImageIcon icon,
                            final File mediaPath, final long duration,
                            final long offset, final CarriageEventListener listener,
                            final TrackPainter trackPainter) {

        // TrackController
        final TrackController trackController = new TrackController(mixerModel, trackPainter);
        trackController.setTrackInformation(trackId, icon, mediaPath, duration, offset);
        trackController.addMarker(-1);

        if (duration < 0) {
            trackController.setErroneous(true);
        }

        if (listener != null) {
            trackController.addCarriageEventListener(listener);
        }

        trackController.addCarriageEventListener(selectionHandler);
        trackController.addTrackMouseEventListener(this);
        trackController.attachAsWindowListener();

        tracks.put(trackId, trackController);

        editingPanel.add(trackController.getView(),"pad 0 0 0 " + -RegionConstants.RMARKER_WIDTH + ", growx");
        editingPanel.invalidate();

        // BugzID:2391 - Make the newly added track visible.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                editingPanel.scrollRectToVisible(trackController.getView().getBounds());
            }
        });
    }

    /**
     * Bind track actions to a data viewer.
     *
     * @param trackId Track identifier.
     * @param actions Actions to bind with.
     */
    public void bindTrackActions(final Identifier trackId, final CustomActions actions) {
        TrackController trackController = tracks.get(trackId);
        if (trackController != null) {
            trackController.bindTrackActions(actions);
        }
    }

    public ViewerStateListener getViewerStateListener(final Identifier trackId) {
        return tracks.get(trackId);
    }

    /**
     * Remove a specific track from the controller. Also unregisters the given listener from the track
     *
     * @param trackId  track identifier
     * @param listener listener for carriage events
     * @return true if a track was removed, false otherwise
     */
    public boolean removeTrack(final Identifier trackId, final CarriageEventListener listener) {

        if (tracks.containsKey(trackId)) {
            TrackController trackController = tracks.remove(trackId);

            trackController.removeCarriageEventListener(listener);
            trackController.removeCarriageEventListener(selectionHandler);
            trackController.removeTrackMouseEventListener(this);

            editingPanel.remove(trackController.getView());
            editingPanel.validate();
            editingPanel.repaint();

            return true;

        } else {
            return false;
        }
    }

    /**
     * Remove all tracks from the controller.
     */
    public void removeAllTracks() {

        for (TrackController trackController : tracks.values()) {
            trackController.removeTrackMouseEventListener(this);
            trackController.removeCarriageEventListener(selectionHandler);
        }

        tracks.clear();
        editingPanel.removeAll();
        editingPanel.repaint();
    }

    /**
     * Sets the track offset for the given media if it exists. If offset snapping is enabled through,
     * then this function will attempt to synchronize the track position with every other
     * track's position of interest. A position of interest includes time 0, start of a track, bookmarked
     * positions, end of a track, and the current needle position.
     *
     * @param trackId              Identifies a track
     * @param newOffset            New track offset position
     * @param snapTemporalPosition If snapping is enabled, the closest position of interest to
     *                             snapTemporalPosition will be used as the first candidate for
     *                             synchronization.
     * @return true if the offset was set, false otherwise.
     */
    public boolean setTrackOffset(final Identifier trackId, final long newOffset, final long snapTemporalPosition) {

        TrackController trackController = tracks.get(trackId);

        if (trackController == null) {
            return false;
        }

        trackController.setTrackOffset(newOffset);
        snapMarkerController.setMarkerTime(-1); // TODO: Use aa constant with description why set to -1!

        SnapPoint snapPoint = snapOffset(trackId, snapTemporalPosition);
        trackController.setMoveable(snapPoint == null);

        if (snapPoint == null) {
            snapMarkerController.setMarkerTime(-1);
        } else {
            snapMarkerController.setMarkerTime(snapPoint.snapMarkerPosition);
            trackController.setTrackOffset(newOffset + snapPoint.snapOffset);
        }

        return true;
    }

    /**
     * Naive snap algorithm:
     * <ol>
     * <li>Compile a list of snap points for the given track.</li>
     * <li>Compile a list of candidate snap points from every other track.</li>
     * <li>Find a snap position by comparing the snap points for the given track against every other track.</li>
     * <li>A candidate snap point is chosen as the new offset value if it is
     * within +/- 5 seconds of the snap point being compared against.</li>
     * <li>If no snap points are found, then return null.</li>
     * </ol>
     *
     * @param trackId              Identifier of the track being moved.
     * @param temporalSnapPosition The snap position to start searching from.
     * @return see Javadoc for explanation.
     */
    private SnapPoint snapOffset(final Identifier trackId, final long temporalSnapPosition) {

        final ViewportState viewport = mixerModel.getViewportModel().getViewport();

        // Points on other (non-selected) data tracks that can be used for alignment
        final List<Long> snapCandidates = Lists.newArrayList();

        // Points on the current/selected data track that may be used for alignment against other data tracks
        final List<Long> snapPoints = Lists.newArrayList();

        long longestDuration = 0;

        // add time zero as a candidate snap point
        if (viewport.isTimeInViewport(0)) {
            snapCandidates.add(0L);
        }

        // add region markers as snap candidates
        final RegionState region = mixerModel.getRegionModel().getRegion();

        if (viewport.isTimeInViewport(region.getRegionStart())) {
            snapCandidates.add(region.getRegionStart());
        }

        if (viewport.isTimeInViewport(region.getRegionEnd())) {
            snapCandidates.add(region.getRegionEnd());
        }

        // add the needle as a candidate snap point
        final long needlePosition = mixerController.getNeedleController().getNeedleModel().getCurrentTime();

        if (viewport.isTimeInViewport(needlePosition)) {
            snapCandidates.add(needlePosition);
        }

        // Compile track and candidate snap points
        for (TrackController trackController : tracks.values()) {
            final List<Long> snapList = trackController.getTrackModel().getIdentifier().equals(trackId) ? snapPoints : snapCandidates;

            // add the left side (start) of the track as a snap point
            final long startTime = trackController.getOffset();

            if (startTime > 0) {
                snapList.add(startTime);
            }

            // add all of the bookmarks as snap points
            for (Long bookmark : trackController.getMarkers()) {
                final long time = startTime + bookmark;
                if (time > 0) {
                    snapList.add(time);
                }
            }

            // add the right side (end) of the track as a snap point
            final long duration = trackController.getDuration();
            final long endTime = startTime + duration;

            if (endTime > 0) {
                snapList.add(endTime);
            }

            if (duration > longestDuration) {
                longestDuration = duration;
            }
        }

        // If there are no snap candidates just exit immediately
        if (snapCandidates.isEmpty()) {
            return null;
        }

        final long snappingThreshold = TrackController.calculateSnappingThreshold(viewport);

        // Remove duplicate candidate snap points
        for (int i = snapCandidates.size() - 1; i > 0; i--) {
            if (snapCandidates.get(i).equals(snapCandidates.get(i - 1))) {
                snapCandidates.remove(i);
            }
        }

        // Sort the candidate snap points
        Collections.sort(snapCandidates);

        // Search for a snap position nearest to temporalSnapPosition
        int nearestIndex = Collections.binarySearch(snapPoints, temporalSnapPosition);

        if (nearestIndex < 0) {
            nearestIndex = -(nearestIndex + 1);
        }

        if (nearestIndex >= snapPoints.size()) {
            nearestIndex = snapPoints.size() - 1;
        }

        if ((nearestIndex >= 0) && (nearestIndex < snapPoints.size())) {
            final long rightSnapTime = snapPoints.get(nearestIndex);
            long leftSnapTime = rightSnapTime;

            if (nearestIndex > 0) {
                leftSnapTime = snapPoints.get(nearestIndex - 1);
            }

            // Add the closest snap point as first search position
            if (Math.abs(rightSnapTime - temporalSnapPosition) < Math.abs(temporalSnapPosition - leftSnapTime)) {
                snapPoints.add(0, rightSnapTime);
            } else {
                snapPoints.add(0, leftSnapTime);
            }
        }

        // Search for snap position
        for (Long snapPoint : snapPoints) {
            int candidateIndex = Collections.binarySearch(snapCandidates,
                    snapPoint);

            if (candidateIndex < 0) {
                candidateIndex = -(candidateIndex + 1);
            }

            if (candidateIndex >= snapCandidates.size()) {
                candidateIndex = snapCandidates.size() - 1;
            }

            final long upperSnapTime = snapCandidates.get(candidateIndex);
            long lowerSnapTime = upperSnapTime;

            if (candidateIndex > 0) {
                lowerSnapTime = snapCandidates.get(candidateIndex - 1);
            }

            if ((lowerSnapTime < snapPoint) && (Math.abs(snapPoint - lowerSnapTime) < snappingThreshold)) {
                final SnapPoint sp = new SnapPoint();
                sp.snapOffset = lowerSnapTime - snapPoint;
                sp.snapMarkerPosition = lowerSnapTime;
                return sp;
            }

            // Check if the candidate snap points can be used
            if (Math.abs(upperSnapTime - snapPoint) < snappingThreshold) {
                final SnapPoint sp = new SnapPoint();
                sp.snapOffset = upperSnapTime - snapPoint;
                sp.snapMarkerPosition = upperSnapTime;

                return sp;
            }
        }

        return null;
    }

    /**
     * Adds the current temporal position as a bookmark to all selected tracks.
     *
     * @param position temporal position in milliseconds
     */
    public void addTemporalBookmarkToSelected(final long position) {
        for (TrackController trackController : tracks.values()) {
            if (trackController.isSelected()) {
                trackController.addReferencedMarker(position);
                trackController.saveMarker();
            }
        }
    }

    /**
     * @return True if at least one track is selected, false otherwise.
     */
    public boolean hasSelectedTracks() {
        for (TrackController trackController : tracks.values()) {
            if (trackController.isSelected()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return number of tracks being managed by this controller
     */
    public int numberOfTracks() {
        return tracks.size();
    }

    /**
     * Sets the carriage movement locking state.
     *
     * @param lockState true if carriages are not allowed to move, false otherwise.
     */
    public void setLockedState(final boolean lockState) {
        for (TrackController trackController : tracks.values()) {
            trackController.setLocked(lockState);
        }
    }

    /**
     * Handles a mouse released event on a track.
     *
     * @param e The event to handle.
     */
    public void mouseReleased(final MouseEvent e) {
        snapMarkerController.setMarkerTime(-1);
    }

    /**
     * Set the bookmark for the given track.
     *
     * @param trackId Track identifier.
     * @param positions Positions of the bookmarks in milliseconds.
     */
    public void setBookmarkPositions(final Identifier trackId, final List<Long> positions) {
        TrackController trackController = tracks.get(trackId);
        if (trackController != null) {
            trackController.addMarkers(positions);
        }
    }

    /**
     * Set the bookmark for the given track. For backwards compatibility only.
     *
     * @param mediaPath Absolute path to the media file represented by the
     *                  track.
     * @param positions  Positions of the bookmarks in milliseconds.
     */
    @Deprecated
    public void setBookmarkPositions(final String mediaPath, final List<Long> positions) {
        for (TrackController trackController : tracks.values()) {
            if (trackController.getTrackModel().getSourceFile().equals(mediaPath)) {
                trackController.addMarkers(positions);
                return;
            }
        }
    }

    /**
     * Set the movement lock state for a given track
     *
     * @param trackId Track identifier
     * @param lock true if the track's movement is locked, false otherwise
     */
    public void setMovementLock(final Identifier trackId, final boolean lock) {
        TrackController trackController = tracks.get(trackId);
        if (trackController != null) {
            trackController.setLocked(lock);
        }
    }

    /**
     * Set the movement lock state for a given track. For backwards compatibility only.
     *
     * @param mediaPath Absolute path to the media file represented by the track
     * @param lock      true if the track's movement is locked, false otherwise
     */
    @Deprecated
    public void setMovementLock(final String mediaPath, final boolean lock) {
        for (TrackController trackController : tracks.values()) {
            if (trackController.getTrackModel().getSourceFile().equals(mediaPath)) {
                trackController.setLocked(lock);
                return;
            }
        }
    }

    /**
     * Get the track model for a given identifier.
     *
     * @param trackId identifier used to search for the track
     * @return null if there is no such track, the associated TrackModel otherwise
     */
    public TrackModel getTrackModel(final Identifier trackId) {
        TrackController trackController = tracks.get(trackId);
        return (trackController != null) ? trackController.getTrackModel() : null;
    }

    public boolean isAnyTrackUnlocked() {
        for (TrackController trackController : tracks.values()) {
            if (!trackController.getTrackModel().isLocked()) {
                return true;
            }
        }
        return tracks.isEmpty();
    }

    /**
     * Deselect all tracks except for the given track.
     *
     * @param selected
     */
    private void deselectExcept(final TrackController selected) {
        for (TrackController trackController : tracks.values()) {
            if (trackController != selected) {
                trackController.deselect();
            }
        }
    }

    /**
     * Inner class for handling carriage selection.
     */
    private class CarriageSelection extends CarriageEventAdapter {

        @Override
        public void selectionChanged(final CarriageEvent e) {
            if (!e.hasModifiers()) {
                deselectExcept((TrackController) e.getSource());
            }
        }
    }

    /**
     * Inner class for packaging snap information.
     */
    private static class SnapPoint {

        /** The new snap offset position in milliseconds */
        long snapOffset;

        /** The snap marker position to paint */
        long snapMarkerPosition;

        public String toString() {
            return "[SnapPoint snapOffset=" + snapOffset + ", snapMarkerPosition=" + snapMarkerPosition + "]";
        }
    }
}
