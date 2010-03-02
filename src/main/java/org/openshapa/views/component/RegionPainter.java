package org.openshapa.views.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;

import javax.swing.JComponent;

import org.openshapa.models.component.RegionModel;
import org.openshapa.models.component.ViewableModel;

/**
 * This class paints the custom playback region.
 */
public class RegionPainter extends JComponent {

    /**
     * Auto generated by Eclipse
     */
    private static final long serialVersionUID = 3570489696805853386L;
    /** Polygon region for the start marker */
    private Polygon startMarkerPolygon;
    /** Polygon region for the end marker */
    private Polygon endMarkerPolygon;

    private RegionModel regionModel;
    private ViewableModel viewableModel;

    public RegionPainter() {
        super();
    }

    public RegionModel getRegionModel() {
        return regionModel;
    }

    public void setRegionModel(final RegionModel regionModel) {
        this.regionModel = regionModel;
        this.repaint();
    }

    public ViewableModel getViewableModel() {
        return viewableModel;
    }

    public void setViewableModel(final ViewableModel viewableModel) {
        this.viewableModel = viewableModel;
        this.repaint();
    }

    public Polygon getEndMarkerPolygon() {
        return endMarkerPolygon;
    }

    public Polygon getStartMarkerPolygon() {
        return startMarkerPolygon;
    }

    @Override
    public boolean contains(final Point p) {
        return startMarkerPolygon.contains(p) || endMarkerPolygon.contains(p);
    }

    @Override
    public boolean contains(final int x, final int y) {
        return startMarkerPolygon.contains(x, y)
                || endMarkerPolygon.contains(x, y);
    }

    @Override
    public void paint(final Graphics g) {
        if (regionModel == null || viewableModel == null) {
            return;
        }
        Dimension size = this.getSize();

        final float ratio =
                viewableModel.getIntervalWidth()
                        / viewableModel.getIntervalTime();

        // If the left region marker is visible, paint the marker
        final long regionStart = regionModel.getRegionStart();
        final long regionEnd = regionModel.getRegionEnd();
        final int paddingTop = regionModel.getPaddingTop();

        final int markerHeight = 38;

        // If the left region marker is visible, paint the marker
        if (regionStart >= viewableModel.getZoomWindowStart()) {
            g.setColor(new Color(15, 135, 0, 100)); // Semi-transparent green
            // The polygon tip
            int pos =
                    Math.round(regionModel.getRegionStart() * ratio
                            - viewableModel.getZoomWindowStart() * ratio)
                            + regionModel.getPaddingLeft();

            // Make an arrow
            startMarkerPolygon = new Polygon();
            startMarkerPolygon.addPoint(pos - 10, paddingTop);
            startMarkerPolygon.addPoint(pos, 19 + paddingTop);
            startMarkerPolygon.addPoint(pos, markerHeight + paddingTop);
            startMarkerPolygon.addPoint(pos - 10, markerHeight + paddingTop);
            g.fillPolygon(startMarkerPolygon);

            // Draw outline
            g.setColor(new Color(15, 135, 0));
            g.drawPolygon(startMarkerPolygon);

            // Draw drop down line
            g.drawLine(pos, markerHeight, pos, size.height);
        }

        // If the right region marker is visible, paint the marker
        if (regionEnd <= viewableModel.getZoomWindowEnd()) {
            g.setColor(new Color(15, 135, 0, 100)); // Semi-transparent green

            // The polygon tip
            int pos =
                    Math.round(regionModel.getRegionEnd() * ratio
                            - viewableModel.getZoomWindowStart() * ratio)
                            + regionModel.getPaddingLeft();
            endMarkerPolygon = new Polygon();
            endMarkerPolygon.addPoint(pos + 1, 19 + paddingTop);
            endMarkerPolygon.addPoint(pos + 11, paddingTop);
            endMarkerPolygon.addPoint(pos + 11, markerHeight + paddingTop);
            endMarkerPolygon.addPoint(pos + 1, markerHeight + paddingTop);

            g.fillPolygon(endMarkerPolygon);

            // Draw outline
            g.setColor(new Color(15, 135, 0));
            g.drawPolygon(endMarkerPolygon);

            // Draw drop down line
            g.drawLine(pos + 1, markerHeight, pos + 1, size.height);
        }

        /*
         * Check if the selected region is not the maximum viewing window, if it
         * is not the maximum, dim the unplayed regions.
         */
        if (regionStart > 0) {
            long endTimePos =
                    regionStart > viewableModel.getZoomWindowEnd()
                            ? viewableModel.getZoomWindowEnd() : regionStart;

            long endXPos =
                    Math.round(endTimePos * ratio
                            - viewableModel.getZoomWindowStart() * ratio)
                            + regionModel.getPaddingLeft();

            int startPos = regionModel.getPaddingLeft();

            // Gray
            g.setColor(new Color(63, 63, 63, 100));
            g.fillRect(startPos, markerHeight + 1, (int) (endXPos - startPos),
                    size.height);
        }
        if (regionEnd < viewableModel.getZoomWindowEnd()) {
            long startTimePos =
                    regionEnd <= viewableModel.getZoomWindowEnd() ? regionEnd
                            : viewableModel.getZoomWindowEnd();

            int startXPos =
                    Math.round(startTimePos * ratio
                            - viewableModel.getZoomWindowStart() * ratio)
                            + regionModel.getPaddingLeft();

            int endXPos =
                    Math.round(viewableModel.getZoomWindowEnd() * ratio
                            - viewableModel.getZoomWindowStart() * ratio)
                            + regionModel.getPaddingLeft() + 4;

            g.setColor(new Color(63, 63, 63, 100));
            g.fillRect(startXPos, markerHeight + 1, endXPos - startXPos,
                    size.height);
        }

    }
}
