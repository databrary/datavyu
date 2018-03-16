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

import org.datavyu.models.component.*;
import org.datavyu.views.component.RegionView;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * RegionController is responsible for managing a RegionPainter.
 */
public final class RegionController implements PropertyChangeListener {
    private final RegionView view;
    private final MixerModel mixerModel;

    public RegionController(final MixerModel mixer) {
        this.mixerModel = mixer;
        mixer.getViewportModel().addPropertyChangeListener(this);

        view = new RegionView(mixer);

        final RegionMarkerMouseListener markerListener = new RegionMarkerMouseListener();
        view.addMouseListener(markerListener);
        view.addMouseMotionListener(markerListener);
    }

    public RegionModel getModel() {
        return mixerModel.getRegionModel();
    }

    /**
     * @return View used by the controller
     */
    public JComponent getView() {
        return view;
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        if (evt.getSource() == mixerModel.getViewportModel()) {
            view.repaint();
        }
    }

    /**
     * Inner class used to handle intercepted events.
     */
    private final class RegionMarkerMouseListener extends MouseInputAdapter {
        private boolean onStartMarker;
        private boolean onEndMarker;
        // offset in pixels from the region marker position to where the marker was "picked up" for dragging
        private double offset;

        private ViewportState viewport;

        private final Cursor eastResizeCursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        private final Cursor defaultCursor = Cursor.getDefaultCursor();

        public RegionMarkerMouseListener() {
            super();
            onStartMarker = false;
            onEndMarker = false;
        }

        @Override
        public void mouseEntered(final MouseEvent e) {
            final JComponent source = (JComponent) e.getSource();
            if (view.contains(e.getPoint())) {
                source.setCursor(eastResizeCursor);
            }
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            final Component source = (Component) e.getSource();
            final GeneralPath startMarker = view.getStartMarkerPolygon();
            final GeneralPath endMarker = view.getEndMarkerPolygon();
            final RegionState regionState = mixerModel.getRegionModel().getRegion();
            viewport = mixerModel.getViewportModel().getViewport();
            onStartMarker = startMarker.contains(e.getPoint());
            onEndMarker = endMarker.contains(e.getPoint());
            assert !(onStartMarker && onEndMarker); // can't be on both markers at the same time
            source.setCursor((onStartMarker || onEndMarker) ? eastResizeCursor : defaultCursor);
            offset = e.getX() - viewport.computePixelXOffset(onStartMarker ? regionState.getRegionStart() : regionState.getRegionEnd()) - RegionConstants.RMARKER_WIDTH;
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            if (onStartMarker || onEndMarker) {
                assert viewport != null;
                assert !(onStartMarker && onEndMarker);

                double x = Math.min(Math.max(e.getX() - offset, 0), view.getSize().width) - RegionConstants.RMARKER_WIDTH;
                double newTime = viewport.computeTimeFromXOffset(x) + viewport.getViewStart();
                newTime = Math.round(Math.min(Math.max(newTime, viewport.getViewStart()), viewport.getViewEnd()));

                final RegionModel regionModel = mixerModel.getRegionModel();
                final RegionState region = regionModel.getRegion();

                if (onStartMarker) {
                    newTime = Math.min(newTime, region.getRegionEnd());
                    regionModel.setPlaybackRegionStart((long) newTime);
                } else {
                    newTime = Math.max(newTime, region.getRegionStart());
                    regionModel.setPlaybackRegionEnd((long) newTime);
                }
            }
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            onStartMarker = false;
            onEndMarker = false;
            viewport = null;
            offset = 0;

            final JComponent source = (JComponent) e.getSource();
            source.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }
}
