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
import org.datavyu.views.component.NeedleComponent;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * NeedleController is responsible for managing a NeedleComponent
 */
public final class NeedleController implements PropertyChangeListener {

    private final NeedleComponent needleComponent;
    private final NeedleModelImpl needleModelImpl;
    private final MixerModel mixerModel;
    private final MixerController mixerController;

    public NeedleController(final MixerController mixerController, final MixerModel mixer) {
        this.mixerController = mixerController;
        // TODO: Fix this it's going to take a lot of effort because the design of the needle controller
        assert mixer.getNeedleModel() instanceof NeedleModelImpl; // UGLY HACK until this is fixed properly
        needleModelImpl = (NeedleModelImpl) mixer.getNeedleModel();

        this.mixerModel = mixer;

        needleComponent = new NeedleComponent(needleModelImpl);

        mixer.getViewportModel().addPropertyChangeListener(this);

        final NeedleListener needleListener = new NeedleListener();
        needleComponent.addMouseListener(needleListener);
        needleComponent.addMouseMotionListener(needleListener);
    }

    /**
     * Set the current time to be represented by the needle.
     *
     * @param currentTime
     */
    public void setCurrentTime(final long currentTime) {
        needleModelImpl.setCurrentTime(currentTime);
    }

    /**
     * @see NeedleModelImpl#setTimescaleTransitionHeight(int)
     */
    public void setTimescaleTransitionHeight(int newHeight) {
        needleModelImpl.setTimescaleTransitionHeight(newHeight);
        needleComponent.repaint();
    }

    /**
     * @see NeedleModelImpl#setZoomIndicatorHeight(int)
     */
    public void setZoomIndicatorHeight(int newHeight) {
        needleModelImpl.setZoomIndicatorHeight(newHeight);
        needleComponent.repaint();
    }

    public void resetNeedlePosition() {
        final RegionState region = mixerModel.getRegionModel().getRegion();
        setCurrentTime(region.getRegionStart());
    }

    public NeedleModel getNeedleModel() {
        return needleModelImpl;
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        if (evt.getSource() == mixerModel.getViewportModel()) {
            needleComponent.repaint();
        }
    }

    /**
     * @return View used by the controller
     */
    public JComponent getNeedleComponent() {
        return needleComponent;
    }

    /**
     * Inner class used to handle intercepted events.
     */
    private final class NeedleListener extends MouseInputAdapter {
        private final Cursor eastResizeCursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        private final Cursor defaultCursor = Cursor.getDefaultCursor();
        private ViewportState viewport = null;
        /**
         * offset in pixels from the needle position to where the needle head was "picked up" for dragging
         */
        private double needlePositionOffsetX = 0.0;

        @Override
        public void mouseEntered(final MouseEvent e) {
            JComponent source = (JComponent) e.getSource();
            source.setCursor(eastResizeCursor);
        }

        @Override
        public void mouseExited(final MouseEvent e) {
            JComponent source = (JComponent) e.getSource();
            source.setCursor(defaultCursor);
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            mouseEntered(e);
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            if (viewport != null) {
                final double dx = Math.min(Math.max(e.getX()
                        - NeedleConstants.NEEDLE_HEAD_WIDTH - needlePositionOffsetX, 0), needleComponent.getWidth());
                long newTime = viewport.computeTimeFromXOffset(dx) + viewport.getViewStart();
                newTime = Math.min(Math.max(newTime, viewport.getViewStart()), viewport.getViewEnd());
                mixerController.getTimescaleController().jumpToTime(newTime, false);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            viewport = mixerModel.getViewportModel().getViewport();
            final double currentNeedleX = viewport.computePixelXOffset(needleModelImpl.getCurrentTime())
                    + NeedleConstants.NEEDLE_HEAD_WIDTH;
            final int mousePressedX = e.getX();
            needlePositionOffsetX = mousePressedX - currentNeedleX;
        }

        @Override
        public void mouseReleased(MouseEvent e) { }
    }
}
