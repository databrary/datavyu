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

import java.util.EventObject;


/**
 * Event object used to inform listeners about child component events
 */
public class TracksControllerEvent extends EventObject {

    private static final long serialVersionUID = 6049024296868823563L;

    public enum EventType {
        CARRIAGE_EVENT, /** @see CarriageEvent */
        TIMESCALE_EVENT, /** @see TimescaleEvent */
    }

    /** Event from child component */
    private EventObject eventObject;

    /** Type of track event that happened */
    private EventType eventType;

    public TracksControllerEvent(final Object source, final EventType eventType, final EventObject eventObject) {
        super(source);
        this.eventObject = eventObject;
        this.eventType = eventType;
    }

    /**
     * @return Needle event from child component
     */
    public EventObject getEventObject() {
        return eventObject;
    }

    /**
     * @return Type of track event that happened
     */
    public EventType getEventType() {
        return eventType;
    }
}
