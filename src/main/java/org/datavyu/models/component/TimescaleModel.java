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
package org.datavyu.models.component;

import java.awt.*;


/**
 * This model provides information used to render the time scale on the tracks
 * interface.
 */
public final class TimescaleModel {

    /**
     * Height of the zoom window indicator in pixels
     */
    private int zoomWindowIndicatorHeight;

    /**
     * Height of the transition between the zoom window indicator and the track in pixels
     */
    private int zoomWindowToTrackTransitionHeight;

    /**
     * This is the height of the scale
     */
    private int height;

    /**
     * Color of the zoom window indicator bar
     */
    private Color zoomWindowIndicatorColor = Color.black;

    /**
     * Background color of the time scale
     */
    private Color timescaleBackgroundColor = Color.black;

    /**
     * Color of the hourly interval markers
     */
    private Color hoursMarkerColor = Color.black;

    /**
     * Color of the minutes interval markers
     */
    private Color minutesMarkerColor = Color.black;

    /**
     * Color of the seconds interval markers
     */
    private Color secondsMarkerColor = Color.black;

    /**
     * Color of the milliseconds interval markers
     */
    private Color millisecondsMarkerColor = Color.black;

    public TimescaleModel() {}

    protected TimescaleModel(final TimescaleModel other) {
        zoomWindowIndicatorHeight = other.zoomWindowIndicatorHeight;
        zoomWindowToTrackTransitionHeight =
                other.zoomWindowToTrackTransitionHeight;
        height = other.height;
        zoomWindowIndicatorColor = other.zoomWindowIndicatorColor;
        timescaleBackgroundColor = other.timescaleBackgroundColor;
        hoursMarkerColor = other.hoursMarkerColor;
        minutesMarkerColor = other.minutesMarkerColor;
        secondsMarkerColor = other.secondsMarkerColor;
        millisecondsMarkerColor = other.millisecondsMarkerColor;
    }

    /**
     * @return height of the zoom window indicator in pixels
     */
    public int getZoomWindowIndicatorHeight() {
        return zoomWindowIndicatorHeight;
    }

    /**
     * Set the height of the zoom window indicator in pixels
     */
    public void setZoomWindowIndicatorHeight(
            final int zoomWindowIndicatorHeight) {
        assert zoomWindowIndicatorHeight >= 0;
        this.zoomWindowIndicatorHeight = zoomWindowIndicatorHeight;
    }

    /**
     * @return height of the transition from the zoom window to the track
     */
    public int getZoomWindowToTrackTransitionHeight() {
        return zoomWindowToTrackTransitionHeight;
    }

    /**
     * Set the height of the transition from teh zoom window to the track
     */
    public void setZoomWindowToTrackTransitionHeight(
            final int zoomWindowToTrackTransitionHeight) {
        assert zoomWindowToTrackTransitionHeight >= 0;
        this.zoomWindowToTrackTransitionHeight =
                zoomWindowToTrackTransitionHeight;
    }

    /**
     * @return height of the timescale area
     */
    public int getTimescaleHeight() {
        return getHeight() - getZoomWindowIndicatorHeight() - getZoomWindowToTrackTransitionHeight();
    }

    /**
     * @return height of the timescale track
     */
    public int getHeight() {
        return height;
    }

    /**
     * Set the height of the timescale track
     */
    public void setHeight(final int height) {
        assert height > 0;
        this.height = height;
    }

    public Color getZoomWindowIndicatorColor() {
        return zoomWindowIndicatorColor;
    }

    public void setZoomWindowIndicatorColor(
            final Color zoomWindowIndicatorColor) {
        this.zoomWindowIndicatorColor = zoomWindowIndicatorColor;
    }

    public Color getTimescaleBackgroundColor() {
        return timescaleBackgroundColor;
    }

    public void setTimescaleBackgroundColor(
            final Color timescaleBackgroundColor) {
        this.timescaleBackgroundColor = timescaleBackgroundColor;
    }

    public Color getHoursMarkerColor() {
        return hoursMarkerColor;
    }

    public void setHoursMarkerColor(final Color hoursMarkerColor) {
        this.hoursMarkerColor = hoursMarkerColor;
    }

    public Color getMinutesMarkerColor() {
        return minutesMarkerColor;
    }

    public void setMinutesMarkerColor(final Color minutesMarkerColor) {
        this.minutesMarkerColor = minutesMarkerColor;
    }

    public Color getSecondsMarkerColor() {
        return secondsMarkerColor;
    }

    public void setSecondsMarkerColor(final Color secondsMarkerColor) {
        this.secondsMarkerColor = secondsMarkerColor;
    }

    public Color getMillisecondsMarkerColor() {
        return millisecondsMarkerColor;
    }

    public void setMillisecondsMarkerColor(
            final Color millisecondsMarkerColor) {
        this.millisecondsMarkerColor = millisecondsMarkerColor;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {

        if (this == obj)
            return true;

        if (obj == null)
            return false;

        if (getClass() != obj.getClass())
            return false;

        TimescaleModel other = (TimescaleModel) obj;

        if (height != other.height)
            return false;

        if (hoursMarkerColor == null) {

            if (other.hoursMarkerColor != null)
                return false;
        } else if (!hoursMarkerColor.equals(other.hoursMarkerColor))
            return false;

        if (millisecondsMarkerColor == null) {

            if (other.millisecondsMarkerColor != null)
                return false;
        } else if (
                !millisecondsMarkerColor.equals(other.millisecondsMarkerColor))
            return false;

        if (minutesMarkerColor == null) {

            if (other.minutesMarkerColor != null)
                return false;
        } else if (!minutesMarkerColor.equals(other.minutesMarkerColor))
            return false;

        if (secondsMarkerColor == null) {

            if (other.secondsMarkerColor != null)
                return false;
        } else if (!secondsMarkerColor.equals(other.secondsMarkerColor))
            return false;

        if (timescaleBackgroundColor == null) {

            if (other.timescaleBackgroundColor != null)
                return false;
        } else if (
                !timescaleBackgroundColor.equals(other.timescaleBackgroundColor))
            return false;

        if (zoomWindowIndicatorColor == null) {

            if (other.zoomWindowIndicatorColor != null)
                return false;
        } else if (
                !zoomWindowIndicatorColor.equals(other.zoomWindowIndicatorColor))
            return false;

        if (zoomWindowIndicatorHeight != other.zoomWindowIndicatorHeight)
            return false;

        if (zoomWindowToTrackTransitionHeight
                != other.zoomWindowToTrackTransitionHeight)
            return false;

        return true;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + height;
        result = (prime * result)
                + ((hoursMarkerColor == null) ? 0 : hoursMarkerColor.hashCode());
        result = (prime * result)
                + ((millisecondsMarkerColor == null)
                ? 0 : millisecondsMarkerColor.hashCode());
        result = (prime * result)
                + ((minutesMarkerColor == null) ? 0 : minutesMarkerColor.hashCode());
        result = (prime * result)
                + ((secondsMarkerColor == null) ? 0 : secondsMarkerColor.hashCode());
        result = (prime * result)
                + ((timescaleBackgroundColor == null)
                ? 0 : timescaleBackgroundColor.hashCode());
        result = (prime * result)
                + ((zoomWindowIndicatorColor == null)
                ? 0 : zoomWindowIndicatorColor.hashCode());
        result = (prime * result) + zoomWindowIndicatorHeight;
        result = (prime * result) + zoomWindowToTrackTransitionHeight;

        return result;
    }

    public TimescaleModel copy() {
        return new TimescaleModel(this);
    }
}
