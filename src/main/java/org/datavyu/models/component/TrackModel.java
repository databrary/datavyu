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
package org.datavyu.models.component;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.models.Identifier;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;


/**
 * This model provides data feed information used to render a carriage on the tracks interface.
 */
public final class TrackModel {

    /** The logger for this class */
    private static Logger logger = LogManager.getLogger(TrackModel.class);

    /** Enumeration of track states. */
    public enum TrackState {
        NORMAL,     // Track is in the normal state
        SELECTED,   // Track is in the selected state
        SNAPPED     // Track is in the snapped state
    }

    /** Track identifier */
    private Identifier identifier;

    /** This is the duration of the track in milliseconds */
    private long duration;

    /** This is the offset of this track with respect to other tracks in milliseconds */
    private long offset;

    /** Track markers location in milliseconds that are sorted at all times */
    private Set<Long> markers = new TreeSet<>(new Comparator<Long>() {
        @Override
        public int compare(Long o1, Long o2) {
            long l1 = o1;
            long l2 = o2;
            return l1 == l2 ? 0 : l1 < l2 ? -1 : +1;
        }
    });

    /** Is there an error with track information */
    private boolean erroneous;

    /** Source file for this track */
    private String sourceFile;

    /** Name of this track */
    private String trackName;

    /** State of the track */
    private TrackState state;

    /** Is the track's movement locked */
    private boolean locked;

    /** Used to enable support for property change events. */
    private PropertyChangeSupport change;

    /**
     * Creates a new track model.
     */
    public TrackModel() {
        change = new PropertyChangeSupport(this);
    }

    /**
     * Copy constructor
     *
     * @param trackModel Model to copy from
     */
    private TrackModel(final TrackModel trackModel) {
        change = new PropertyChangeSupport(this);
        duration = trackModel.duration;
        offset = trackModel.offset;
        markers = new TreeSet<>(trackModel.markers);
        erroneous = trackModel.erroneous;
        sourceFile = trackModel.sourceFile;
        trackName = trackModel.trackName;
        state = trackModel.state;
        locked = trackModel.locked;
        identifier = trackModel.identifier;
    }

    /**
     * @see PropertyChangeSupport#addPropertyChangeListener(PropertyChangeListener)
     */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        change.addPropertyChangeListener(listener);
    }

    /**
     * @see PropertyChangeSupport#addPropertyChangeListener(String, PropertyChangeListener)
     */
    public void addPropertyChangeListener(final String property, final PropertyChangeListener listener) {
        change.addPropertyChangeListener(property, listener);
    }

    /**
     * @see PropertyChangeSupport#removePropertyChangeListener(PropertyChangeListener)
     */
    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        change.removePropertyChangeListener(listener);
    }

    /**
     * @see PropertyChangeSupport#removePropertyChangeListener(String, PropertyChangeListener)
     */
    public void removePropertyChangeListener(final String property, final PropertyChangeListener listener) {
        change.removePropertyChangeListener(property, listener);
    }

    /**
     * @return is the track locked.
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * @param locked lock state to set.
     */
    public void setLocked(final boolean locked) {
        boolean oldLocked = this.locked;
        this.locked = locked;
        change.firePropertyChange("locked", oldLocked, locked);
    }

    /**
     * @return The duration of the track in milliseconds
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Set the duration of the track in milliseconds
     *
     * @param duration the new duration.
     */
    public void setDuration(final long duration) {
        long oldDuration = this.duration;
        this.duration = duration;
        change.firePropertyChange("duration", oldDuration, duration);
    }

    /**
     * @return The offset of the track in milliseconds
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Set the offset of the track in milliseconds
     *
     * @param offset the new offset.
     */
    public void setOffset(final long offset) {
        logger.info("For track " + identifier + " set offset to " + offset + " milliseconds");
        long oldOffset = this.offset;
        this.offset = offset;
        change.firePropertyChange("offset", oldOffset, offset);
    }

    /**
     * @return absolute media path.
     */
    public String getSourceFile() {
        return sourceFile;
    }

    /**
     * Sets the absolute media path that this track represents.
     *
     * @param path absolute media path.
     */
    public void setSourceFile(final String path) {
        String oldPath = this.sourceFile;
        sourceFile = path;
        change.firePropertyChange("sourceFile", oldPath, path);
    }

    /**
     * @return Is there an error with track information
     */
    public boolean isErroneous() {
        return erroneous;
    }

    /**
     * Set track information error state.
     *
     * @param erroneous true if erroneous, false otherwise.
     */
    public void setErroneous(final boolean erroneous) {
        boolean oldErroneous = this.erroneous;
        this.erroneous = erroneous;
        change.firePropertyChange("erroneous", oldErroneous, erroneous);
    }

    /**
     * @return list of bookmark positions in milliseconds.
     */
    public List<Long> getMarkers() {
        return new ArrayList<>(markers); // markers are sorted
    }

    /**
     * Adds a snap marker position.
     *
     * @param marker new marker position in milliseconds
     */
    public void addMarker(final long marker) {
        if (markers.add(marker)) {
            // If we added the marker fire a change event -- otherwise it was already there
            change.firePropertyChange("markers", null, markers);
        }
    }
    
    /**
     * Removes a snap marker position.
     *
     * @param marker marker position in milliseconds
     */
    public void removeMarker(final long marker) {
        if (markers.remove(marker)) {
            // This marker was in the set fire a change event
            change.firePropertyChange("markers", null, markers);
        }
    }
    
    public void removeMarkers() {
    	if (!markers.isEmpty()) {
    		markers.clear();
	        change.firePropertyChange("markers", null, markers);
    	}
    }

    /**
     * Set the track name.
     *
     * @param trackName the new track name
     */
    public void setTrackName(final String trackName) {
        String oldTrackName = this.trackName;
        this.trackName = trackName;
        change.firePropertyChange("trackName", oldTrackName, trackName);
    }

    /**
     * @return selected state
     */
    public boolean isSelected() {
        return state == TrackState.SELECTED;
    }

    /**
     * Set the selected state.
     *
     * @param selected true if selected, false otherwise.
     */
    public void setSelected(final boolean selected) {
        boolean oldSelected = isSelected();
        state = selected ? TrackState.SELECTED : TrackState.NORMAL;
        change.firePropertyChange("selected", oldSelected, selected);
    }

    /**
     * Set the state of the track.
     *
     * @param state new state to use.
     */
    public void setState(final TrackState state) {
        TrackState oldState = this.state;
        this.state = state;
        change.firePropertyChange("state", oldState, state);
    }

    /**
     * @return Current track state.
     */
    public TrackState getState() {
        return state;
    }

    /**
     * @param identifier Identifier to use.
     */
    public void setIdentifier(final Identifier identifier) {
        this.identifier = identifier;
    }

    /**
     * @return Identifier of the track.
     */
    public Identifier getIdentifier() {
        return identifier;
    }

    /**
     * @return A copy of the track model but with a new Identifier.
     */
    public TrackModel copy() {
        return new TrackModel(this);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + markers.hashCode();
        result = (prime * result) + (int) (duration ^ (duration >>> 32));
        result = (prime * result) + (erroneous ? 1231 : 1237);
        result = (prime * result) + (locked ? 1231 : 1237);
        result = (prime * result) + (int) (offset ^ (offset >>> 32));
        result = (prime * result) + ((state == null) ? 0 : state.hashCode());
        result = (prime * result) + ((sourceFile == null) ? 0 : sourceFile.hashCode());
        result = (prime * result) + ((trackName == null) ? 0 : trackName.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        TrackModel other = (TrackModel) obj;

        if (!markers.equals(other.markers)) {
            return false;
        }

        if (duration != other.duration) {
            return false;
        }

        if (erroneous != other.erroneous) {
            return false;
        }

        if (locked != other.locked) {
            return false;
        }

        if (offset != other.offset) {
            return false;
        }

        if (state == null) {
            if (other.state != null) {
                return false;
            }
        } else if (!state.equals(other.state)) {
            return false;
        }

        if (sourceFile == null) {
            if (other.sourceFile != null) {
                return false;
            }
        } else if (!sourceFile.equals(other.sourceFile)) {
            return false;
        }

        if (trackName == null) {
            if (other.trackName != null) {
                return false;
            }
        } else if (!trackName.equals(other.trackName)) {
            return false;
        }

        return true;
    }

    @Override public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TrackModel [marker={");
        for (Long bookmark : markers) {
        	builder.append(bookmark);
        	if (markers.size() > 1) {
        		builder.append(", ");
        	}
        }
        builder.append("}, duration=");
        builder.append(duration);
        builder.append(", erroneous=");
        builder.append(erroneous);
        builder.append(", locked=");
        builder.append(locked);
        builder.append(", offset=");
        builder.append(offset);
        builder.append(", state=");
        builder.append(state);
        builder.append(", trackId=");
        builder.append(sourceFile);
        builder.append(", trackName=");
        builder.append(trackName);
        builder.append("]");
        return builder.toString();
    }
}
