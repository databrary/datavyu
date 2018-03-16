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
package org.datavyu.views.component;

import org.datavyu.models.component.MixerModel;
import org.datavyu.models.component.TimescaleModel;
import org.datavyu.models.component.ViewportState;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.HashSet;
import java.util.Set;


/**
 * This class is used to paint a timescale for a given range of times.
 */
public final class TimescaleComponent extends JComponent {

    private static final long serialVersionUID = 1514199704893523855L;
    private TimescaleModel timescaleModel;
    private MixerModel mixerModel;

    public void setTimescaleModel(final TimescaleModel timescaleModel) {
        this.timescaleModel = timescaleModel;
        repaint();
    }

    public void setMixerView(final MixerModel mixerModel) {
        this.mixerModel = mixerModel;
        repaint();
    }

    @Override
    public final boolean contains(final Point p) {
        return contains(p.x, p.y);
    }

    @Override
    public final boolean contains(final int x, final int y) {
        return isPointInTimescale(x, y) || isPointInZoomWindowIndicator(x, y);
    }

    public boolean isPointInTimescale(final int x, final int y) {
        return (x >= 0) && (x < getSize().width) &&
                (y >= 0 && y < timescaleModel.getTimescaleHeight());
    }

    public boolean isPointInZoomWindowIndicator(final int x, final int y) {
        return (x >= 0) && (x < getSize().width) &&
                (y >= (timescaleModel.getHeight() - timescaleModel.getZoomWindowIndicatorHeight()));
    }


    /**
     * This method paints the timing scale.
     */
    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);

        if (mixerModel == null) {
            return;
        }

        ViewportState viewport = mixerModel.getViewportModel().getViewport();

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        final long end = viewport.getMaxEnd();

        final int zoomWindowIndicatorBottom = timescaleModel.getHeight() - 1;
        final int zoomWindowIndicatorTop = zoomWindowIndicatorBottom
                - timescaleModel.getZoomWindowIndicatorHeight() + 1;

        final int transitionAreaBottom = zoomWindowIndicatorTop;
        final int transitionAreaTop = zoomWindowIndicatorTop
                - timescaleModel.getZoomWindowToTrackTransitionHeight() + 1;
        final float transitionAreaLeft = 0;
        final float transitionAreaRight = getWidth();

        final float minimumZoomIndicatorWidth = 1f;
        float zoomIndicatorWidth =
                (float) (viewport.getViewEnd() - viewport.getViewStart())
                        * getWidth() / end;
        zoomIndicatorWidth = Math.min(Math.max(zoomIndicatorWidth,
                minimumZoomIndicatorWidth), getWidth());

        float zoomWindowIndicatorX = (float) ((double) viewport.getViewStart()
                * getWidth() / end);

        if ((zoomWindowIndicatorX + zoomIndicatorWidth) >= getWidth()) {
            zoomWindowIndicatorX = getWidth() - zoomIndicatorWidth;
            zoomWindowIndicatorX = Math.max(zoomWindowIndicatorX, 0);
            assert zoomWindowIndicatorX >= 0;
        }

        // draw the background of the time scale
        final Color backgroundColor =
                timescaleModel.getTimescaleBackgroundColor();
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, getWidth(), transitionAreaTop);

        g2d.fillRect(0, zoomWindowIndicatorTop, getWidth(),
                timescaleModel.getZoomWindowIndicatorHeight());

        // draw the current zoom indicator window
        g2d.setColor(timescaleModel.getZoomWindowIndicatorColor());

        GeneralPath zoomWindowIndicator = new GeneralPath();
        zoomWindowIndicator.moveTo(zoomWindowIndicatorX,
                zoomWindowIndicatorTop);
        zoomWindowIndicator.lineTo(zoomWindowIndicatorX,
                zoomWindowIndicatorTop
                        + timescaleModel.getZoomWindowIndicatorHeight());
        zoomWindowIndicator.lineTo(zoomWindowIndicatorX + zoomIndicatorWidth,
                zoomWindowIndicatorTop
                        + timescaleModel.getZoomWindowIndicatorHeight());
        zoomWindowIndicator.lineTo(zoomWindowIndicatorX + zoomIndicatorWidth,
                zoomWindowIndicatorTop);
        zoomWindowIndicator.closePath();
        g2d.fill(zoomWindowIndicator);

        g2d.setColor(Color.darkGray);

        // adjusts the shape of the curve from the zoom window indicator to the time scale (larger values will extend the curve downwards)
        final int transitionCurveBottomWeight = 10;
        assert transitionCurveBottomWeight >= 0;

        g2d.setColor(backgroundColor);

        GeneralPath shape = new GeneralPath();
        shape.moveTo(transitionAreaLeft, transitionAreaTop);
        shape.curveTo(transitionAreaLeft,
                (transitionAreaTop
                        + (transitionAreaBottom * transitionCurveBottomWeight))
                        / (transitionCurveBottomWeight + 1), zoomWindowIndicatorX,
                transitionAreaTop, zoomWindowIndicatorX, transitionAreaBottom);
        shape.lineTo(zoomWindowIndicatorX + zoomIndicatorWidth,
                transitionAreaBottom);
        shape.curveTo(zoomWindowIndicatorX + zoomIndicatorWidth,
                transitionAreaTop, transitionAreaRight,
                (transitionAreaTop
                        + (transitionAreaBottom * transitionCurveBottomWeight))
                        / (transitionCurveBottomWeight + 1), transitionAreaRight,
                transitionAreaTop);
        shape.closePath();
        g2d.fill(shape);

        // plot the time interval markers
        final float majorMarkerWidth = 3f;
        final float minorMarkerWidth = 0.75f;

        final Set<Long> plottedMarkers = new HashSet<Long>(1000);
        final int maxMarkerHeight = timescaleModel.getHeight()
                - timescaleModel.getZoomWindowIndicatorHeight()
                - timescaleModel.getZoomWindowToTrackTransitionHeight();

        final Color hoursColor = timescaleModel.getHoursMarkerColor();
        final Color minutesColor = timescaleModel.getMinutesMarkerColor();
        final Color secondsColor = timescaleModel.getSecondsMarkerColor();
        final Color millisecondsColor =
                timescaleModel.getMillisecondsMarkerColor();

        final long hourIntervalsInMS = 60 * 60 * 1000;
        final long minuteIntervalsInMS = 60 * 1000;
        final long secondIntervalsInMS = 1000;

        final int hourIntervalsMarkerHeight = maxMarkerHeight;
        final int minuteIntervalsMarkerHeight = hourIntervalsMarkerHeight - 10;
        final int secondIntervalsMarkerHeight = hourIntervalsMarkerHeight - 20;
        final int tenMillisecondIntervalsMarkerHeight =
                hourIntervalsMarkerHeight - 25;

        paintMarkers(g2d, viewport, plottedMarkers, hourIntervalsInMS,
                majorMarkerWidth, hourIntervalsMarkerHeight, 150000, 130000,
                hoursColor, backgroundColor);
        paintMarkers(g2d, viewport, plottedMarkers, 10 * minuteIntervalsInMS,
                minorMarkerWidth, hourIntervalsMarkerHeight, 140000, 30000,
                hoursColor, backgroundColor);
        paintMarkers(g2d, viewport, plottedMarkers, minuteIntervalsInMS,
                majorMarkerWidth, minuteIntervalsMarkerHeight, 15000, 2000,
                minutesColor, backgroundColor);
        paintMarkers(g2d, viewport, plottedMarkers, 10 * secondIntervalsInMS,
                minorMarkerWidth, minuteIntervalsMarkerHeight, 4000, 900,
                minutesColor, backgroundColor);
        paintMarkers(g2d, viewport, plottedMarkers, secondIntervalsInMS,
                majorMarkerWidth, secondIntervalsMarkerHeight, 400, 1, secondsColor,
                backgroundColor);
        paintMarkers(g2d, viewport, plottedMarkers, secondIntervalsInMS / 10,
                minorMarkerWidth, secondIntervalsMarkerHeight, 60, 1, secondsColor,
                backgroundColor);
        paintMarkers(g2d, viewport, plottedMarkers, secondIntervalsInMS / 100,
                minorMarkerWidth, tenMillisecondIntervalsMarkerHeight, 5, 1,
                millisecondsColor, backgroundColor);
    }

    /**
     * Draws the time interval markers for a specific time interval (hours,
     * minutes, or seconds, etc).
     *
     * @param g2d              graphics object that the markers will be drawn with
     * @param viewport
     * @param plottedMarkers   set of markers (time in milliseconds) that have already been
     *                         drawn, and therefore should be omitted from being drawn again
     * @param intervalTime     time interval (milliseconds) between markers to be drawn, e.g.
     *                         1000 for seconds markers
     * @param width            width of the marker lines in pixels
     * @param intervalHeight   height of the marker lines in pixels (bottom-aligned)
     * @param startFadeMsPerPx time scale resolution at which the fading begins (the markers
     *                         will not be drawn when the time scale resolution has a higher
     *                         milliseconds/pixel value than this value)
     * @param stopFadeMsPerPx  time scale resolution at which the interval markers are fully
     *                         visible (when the time scale resolution is between
     *                         startFadeMsPerPx and stopFadeMsPerPx, the markers will become
     *                         progressively more visible)
     * @param foregroundColor  color of the markers
     * @param backgroundColor  background color that the markers will fade in from
     */
    private void paintMarkers(final Graphics2D g2d, final ViewportState viewport,
                              final Set<Long> plottedMarkers, final long intervalTime,
                              final float width, final int intervalHeight,
                              final double startFadeMsPerPx, final double stopFadeMsPerPx,
                              final Color foregroundColor, final Color backgroundColor) {
        final int markerBottom = timescaleModel.getHeight()
                - timescaleModel.getZoomWindowIndicatorHeight()
                - timescaleModel.getZoomWindowToTrackTransitionHeight();

        final double currentMsPerPx = viewport.getResolution();

        if (viewport.getResolution() > startFadeMsPerPx) {

            // the time scale is not zoomed in sufficiently for the markers to be visible yet
            return;
        }

        float lineWidthRatio = 1;

        if (currentMsPerPx >= stopFadeMsPerPx) {

            // the markers are still being faded in at this resolution - a cubic function is used to smoothly fade in the interval lines by making them progressively thicker
            double x = (currentMsPerPx - startFadeMsPerPx)
                    / (stopFadeMsPerPx - startFadeMsPerPx);
            x = Math.pow(x, 3);
            lineWidthRatio = (float) x;
        }

        g2d.setColor(foregroundColor);
        g2d.setStroke(new BasicStroke(width * lineWidthRatio,
                BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

        long leftPadTime = (intervalTime
                - (viewport.getViewStart() % intervalTime)) % intervalTime;

        for (long plotTime = viewport.getViewStart() + leftPadTime;
             plotTime <= viewport.getViewEnd(); plotTime += intervalTime) {

            if (plottedMarkers.contains(plotTime)) {

                // don't plot over existing markers
                continue;
            }

            plottedMarkers.add(plotTime);

            // GeneralPath is used here to plot anti-aliased lines at sub-pixel resolution for the fading in/out effect
            final float x = (float) viewport.computePixelXOffset(plotTime);
            GeneralPath line = new GeneralPath();
            line.moveTo(x, markerBottom - intervalHeight - 1);
            line.lineTo(x, markerBottom);
            g2d.draw(line);
        }
    }

}
