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

/**
 * Provides parameters to determine what can be viewed on the tracks
 */
public final class ViewportStateImpl implements ViewportState {

    /** The end time of the longest track (offset included) in milliseconds */
    private final long maxEnd;

    /** The pixel width of a time interval */
    private final double viewWidth;

    /** The start time of the zoomed window */
    private final long viewStart;

    /** The end time of the zoomed window */
    private final long viewEnd;

    public final static long MINIMUM_MAX_END = 60000;

    ViewportStateImpl(final long maxEnd, final double viewWidth,
                             final long viewStart, final long viewEnd) {
        this.maxEnd = Math.max(maxEnd, MINIMUM_MAX_END);
        this.viewWidth = viewWidth;
        this.viewStart = viewStart;
        this.viewEnd = viewEnd;
        validateConstraints(this);
    }

    static void validateConstraints(final ViewportState viewport) {
        assert viewport.getViewStart() <= viewport.getViewEnd();
        assert viewport.getMaxEnd() > 0; // this simplifies calculations in many places
        assert viewport.getViewWidth() >= 0;

        if (viewport.getViewStart() > viewport.getViewEnd()) {
            throw new IllegalArgumentException("viewStart must be <= viewEnd");
        }

        if (viewport.getMaxEnd() <= 0) {
            throw new IllegalArgumentException("maxEnd must be > 0");
        }

        if (viewport.getViewWidth() < 0) {
            throw new IllegalArgumentException("viewWidth must be >= 0");
        }
    }

    /**
     * @return The end time of the zoomed window
     */
    @Override
    public long getViewEnd() {
        return viewEnd;
    }

    /**
     * Set the start time of the zoomed window.
     *
     * @return The start time of the zoomed window
     */
    @Override
    public long getViewStart() {
        return viewStart;
    }

    @Override
    public long getMaxEnd() {
        return maxEnd;
    }

    @Override
    public double getResolution() {
        return (viewWidth > 0) ? ((viewEnd - viewStart) / viewWidth)
                : Double.NaN;
    }

    @Override
    public double getViewWidth() {
        return viewWidth;
    }

    @Override
    public long getViewDuration() {
        return getViewEnd() - getViewStart() + 1;
    }

    @Override
    public double computePixelXOffset(final long time) {
        return (double) (time - getViewStart()) / getResolution();
    }

    @Override
    public long computeTimeFromXOffset(final double offset) {
        return (long) (Math.round(offset * getResolution()));
    }

    @Override
    public boolean isTimeInViewport(final long time) {
        return (getViewStart() <= time) && (time <= getViewEnd());
    }

    @Override
    public double getZoomLevel() {
        return getZoomSettingFor(getResolution());
    }

    public ViewportStateImpl zoomViewport(final double zoomLevel,
                                          final long centerTime) {
        final double millisecondsPerPixel = getMillisecondsPerPixelFor(zoomLevel);

        // preserve the needle position
        long centerPositionTime = Math.min(Math.max(centerTime, 0), getMaxEnd());

        long zoomCenterTime;
        double dxZoomCenterRatio = 0.5;

        if ((centerTime >= 0) && isTimeInViewport(centerTime)) {

            long zoomWindowStart = getViewStart();

            dxZoomCenterRatio = (double) (centerPositionTime - zoomWindowStart) / getViewDuration();

            zoomCenterTime = centerPositionTime;
        } else {
            zoomCenterTime = (getViewStart() + getViewEnd()) / 2;
        }

        dxZoomCenterRatio = Math.min(Math.max(dxZoomCenterRatio, 0.0), 1.0);

        long newZoomWindowTimeRange = Math.round(millisecondsPerPixel * getViewWidth());
        newZoomWindowTimeRange = Math.max(newZoomWindowTimeRange, 1);
        newZoomWindowTimeRange = Math.min(newZoomWindowTimeRange, getMaxEnd() + 1);

        assert (newZoomWindowTimeRange >= 1) && (newZoomWindowTimeRange <= (getMaxEnd() + 1));

        long newStart = Math.round(zoomCenterTime - (dxZoomCenterRatio * newZoomWindowTimeRange));

        if ((newStart + newZoomWindowTimeRange) > getMaxEnd()) {
            newStart = getMaxEnd() - newZoomWindowTimeRange + 1;
        }

        newStart = Math.max(0, newStart);

        assert (newStart <= getMaxEnd());

        long newEnd = newStart + newZoomWindowTimeRange - 1;

        assert (0 <= newEnd) && (newEnd <= getMaxEnd());

        return new ViewportStateImpl(getMaxEnd(), getViewWidth(), newStart, newEnd);
    }

    private double getMillisecondsPerPixelFor(final double zoomSettingValue) {
        double value = (lowerMsPerPixel()
                * Math.exp(
                Math.log(upperMsPerPixel() / lowerMsPerPixel())
                        * (1.0 - zoomSettingValue)));

        return Math.min(Math.max(value, lowerMsPerPixel()), upperMsPerPixel());
    }

    /**
     * @return Lower bound on the number of milliseconds per pixel.
     */
    private double lowerMsPerPixel() {
        return 1.0;
    }

    /**
     * @return Upper bound on the number of milliseconds per pixel.
     */
    private double upperMsPerPixel() {
        final long maxTimeMilliseconds = (getMaxEnd() > 0) ? getMaxEnd() : (24 * 60 * 60 * 1000);

        return Math.ceil((double) maxTimeMilliseconds / getViewWidth());
    }

    private double getZoomSettingFor(final double millisecondsPerPixel) {

        if (millisecondsPerPixel >= upperMsPerPixel()) {
            return 0;
        }

        final double value = 1
                - (Math.log(millisecondsPerPixel / lowerMsPerPixel())
                / Math.log(upperMsPerPixel() / lowerMsPerPixel()));

        return Math.min(Math.max(value, 0), 1.0);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName() + " [getMaxEnd()=");
        builder.append(getMaxEnd());
        builder.append(", getResolution()=");
        builder.append(getResolution());
        builder.append(", getViewDuration()=");
        builder.append(getViewDuration());
        builder.append(", getViewEnd()=");
        builder.append(getViewEnd());
        builder.append(", getViewStart()=");
        builder.append(getViewStart());
        builder.append(", getViewWidth()=");
        builder.append(getViewWidth());
        builder.append(", getZoomLevel()=");
        builder.append(getZoomLevel());
        builder.append("]");

        return builder.toString();
    }

}
