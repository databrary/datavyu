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
package org.datavyu.event.component;

import org.datavyu.models.Identifier;

import java.util.EventObject;
import java.util.List;


/**
 * Event object for a track carriage event.
 */
public final class CarriageEvent extends EventObject {

    /** Fix this class for java object serialization */
    private static final long serialVersionUID = 4009420871939032673L;

    /** Describes the type of event that occurred */
    public enum EventType {
        MARKER_CHANGED,     // Snap position changed
        MARKER_REQUEST,     // Requesting snap position
        MARKER_SAVE,        // Requesting snap saving
        CARRIAGE_SELECTION, // Track changed selection state
        OFFSET_CHANGE,      // Track changed offset position
        CARRIAGE_LOCK       // Track is (un)locked
    }

    /** Track identifier */
    private final Identifier trackId;

    /** New time offset, in milliseconds, for the given track */
    private final long offset;  // TODO: Offset or start time?

    /** Duration of the track in milliseconds */
    private final long duration;

    /** Track markers in milliseconds */
    private final List<Long> markers;

    /** Temporal position of the mouse. Only meaningful for EventType.OFFSET_CHANGE */
    private final long time;

    /** Event type */
    private final EventType eventType;

    /** Were there input modifiers */
    private final boolean hasModifiers;

    /**
     * @param source
     * @param trackId
     * @param offset
     * @param markers
     * @param duration
     * @param time
     * @param eventType
     * @param hasModifiers;
     */
    public CarriageEvent(final Object source, final Identifier trackId, final long offset, final List<Long> markers,
                         final long duration, final long time, final EventType eventType, final boolean hasModifiers) {
        super(source);
        this.trackId = trackId;
        this.offset = offset;
        this.markers = markers;
        this.duration = duration;
        this.time = time;
        this.eventType = eventType;
        this.hasModifiers = hasModifiers;
    }

    /**
     * @return New time offset in milliseconds.
     */
    public long getOffset() {
        return offset;
    }

    /**
     * @return the markers
     */
    public List<Long> getMarkers() {
        return markers;
    }

    /**
     * @return Track identifier
     */
    public Identifier getTrackId() {
        return trackId;
    }

    /**
     * @return Track duration in milliseconds
     */
    public long getDuration() {
        return duration;
    }

    /**
     * @return the temporal position of the mouse in milliseconds. Only
     * meaningful for OFFSET_CHANGE events.
     */
    public long getTime() {
        return time;
    }

    /**
     * @return event type
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * @return true if modifiers were present, false otherwise..
     */
    public boolean hasModifiers() {
        return hasModifiers;
    }
}
