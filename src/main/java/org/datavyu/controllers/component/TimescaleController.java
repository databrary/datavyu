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

import org.datavyu.event.component.TimescaleEvent;
import org.datavyu.event.component.TimescaleListener;
import org.datavyu.models.component.MixerModel;
import org.datavyu.models.component.TimescaleConstants;
import org.datavyu.models.component.TimescaleModel;
import org.datavyu.models.component.ViewportState;
import org.datavyu.views.VideoController;
import org.datavyu.views.component.TimescaleComponent;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * Timescale controller is responsible for managing a TimescaleComponent
 */
public final class TimescaleController implements PropertyChangeListener {

    private final TimescaleComponent timescaleComponent;
    private final TimescaleModel timescaleModel;
    private final MixerModel mixerModel;

    /** Listeners interested in needle painter events */
    private final EventListenerList listenerList;

    TimescaleController(final MixerModel mixerModel) {
        timescaleComponent = new TimescaleComponent();

        timescaleModel = new TimescaleModel();
        timescaleModel.setZoomWindowIndicatorHeight(8);
        timescaleModel.setZoomWindowToTrackTransitionHeight(20);
        timescaleModel.setHeight(50
                + timescaleModel.getZoomWindowIndicatorHeight()
                + timescaleModel.getZoomWindowToTrackTransitionHeight());

        timescaleModel.setZoomWindowIndicatorColor(new Color(192, 192, 192));
        timescaleModel.setTimescaleBackgroundColor(new Color(237, 237, 237));

        timescaleModel.setHoursMarkerColor(TimescaleConstants.HOURS_COLOR);
        timescaleModel.setMinutesMarkerColor(TimescaleConstants.MINUTES_COLOR);
        timescaleModel.setSecondsMarkerColor(TimescaleConstants.SECONDS_COLOR);
        timescaleModel.setMillisecondsMarkerColor(
                TimescaleConstants.MILLISECONDS_COLOR);

        final TimescaleEventListener listener = new TimescaleEventListener();
        timescaleComponent.addMouseListener(listener);
        timescaleComponent.addMouseMotionListener(listener);

        this.mixerModel = mixerModel;

        timescaleComponent.setMixerView(mixerModel);
        timescaleComponent.setTimescaleModel(timescaleModel);

        mixerModel.getViewportModel().addPropertyChangeListener(this);

        listenerList = new EventListenerList();
    }

    public TimescaleModel getTimescaleModel() {
        return timescaleModel;
    }

    /**
     * @return View used by the controller
     */
    public JComponent getTimescaleComponent() {
        return timescaleComponent;
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        if (evt.getSource() == mixerModel.getViewportModel()) {
            timescaleComponent.repaint();
        }
    }

    public void addTimescaleEventListener(final TimescaleListener listener) {
        synchronized (this) {
            listenerList.add(TimescaleListener.class, listener);
        }
    }

    public void removeTimescaleEventListener(final TimescaleListener listener) {
        synchronized (this) {
            listenerList.remove(TimescaleListener.class, listener);
        }
    }

    public void jumpToTime(final long jumpTime, final boolean togglePlaybackMode) {
        fireJumpEvent(jumpTime, togglePlaybackMode);
    }

    /**
     * Used to fire a new event informing listeners about the new needle time.
     *
     * @param jumpTime
     * @param togglePlaybackMode
     */
    private void fireJumpEvent(final long jumpTime, final boolean togglePlaybackMode) {

        synchronized (this) {
            TimescaleEvent e = new TimescaleEvent(this, jumpTime,
                    togglePlaybackMode);
            Object[] listeners = listenerList.getListenerList();

            /*
             * The listener list contains the listening class and then the
             * listener instance.
             */
            for (int i = 0; i < listeners.length; i += 2) {

                if (listeners[i] == TimescaleListener.class) {
                    ((TimescaleListener) listeners[i + 1]).jumpToTime(e);
                }
            }
        }
    }

    /**
     * Intercepts events
     */
    private class TimescaleEventListener extends MouseInputAdapter {
        private ViewportState viewport;
        private final Cursor crosshairCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        private final Cursor defaultCursor = Cursor.getDefaultCursor();
        private boolean isDraggingOnTimescale = false;
        private boolean isDraggingOnZoomWindowIndicator = false;

        @Override
        public void mouseEntered(final MouseEvent e) {
            JComponent source = (JComponent) e.getSource();
            source.setCursor(crosshairCursor);
        }

        @Override
        public void mouseExited(final MouseEvent e) {
            JComponent source = (JComponent) e.getSource();
            source.setCursor(defaultCursor);
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            mouseEntered(e);

            viewport = mixerModel.getViewportModel().getViewport();
            if (timescaleComponent.isPointInTimescale(e.getX(), e.getY())) {
                timescaleComponent.setToolTipText(VideoController.formatTime(calculateNewNeedlePositionOnTimescale(e)));
            } else {
                timescaleComponent.setToolTipText(null);
            }
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            viewport = mixerModel.getViewportModel().getViewport();

            if (timescaleComponent.isPointInTimescale(e.getX(), e.getY())) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    fireJumpEvent(calculateNewNeedlePositionOnTimescale(e), false);
                    isDraggingOnTimescale = true;
                }
            } else if (timescaleComponent.isPointInZoomWindowIndicator(e.getX(), e.getY())) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    fireJumpEvent(calculateNewNeedlePositionOnZoomWindow(e), false);
                    isDraggingOnZoomWindowIndicator = true;
                }
            }
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            isDraggingOnTimescale = false;
            isDraggingOnZoomWindowIndicator = false;
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            if ((e.getButton() == MouseEvent.BUTTON1) && ((e.getClickCount() % 2) == 0)) {
                if (timescaleComponent.isPointInTimescale(e.getX(), e.getY())) {
                    fireJumpEvent(calculateNewNeedlePositionOnTimescale(e), true);
                } else if (timescaleComponent.isPointInZoomWindowIndicator(e.getX(), e.getY())) {
                    fireJumpEvent(calculateNewNeedlePositionOnZoomWindow(e), true);
                }
            }
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (isDraggingOnTimescale) {
                    fireJumpEvent(calculateNewNeedlePositionOnTimescale(e), false);
                } else if (isDraggingOnZoomWindowIndicator) {
                    fireJumpEvent(calculateNewNeedlePositionOnZoomWindow(e), false);
                }
            }
        }

        private long calculateNewNeedlePositionOnTimescale(final MouseEvent e) {
            final int dx = Math.min(Math.max(e.getX(), 0), timescaleComponent.getSize().width);
            final long newTime = viewport.computeTimeFromXOffset(dx) + viewport.getViewStart();
            return Math.min(Math.max(newTime, viewport.getViewStart()), viewport.getViewEnd());
        }

        private long calculateNewNeedlePositionOnZoomWindow(final MouseEvent e) {
            final int dx = Math.min(Math.max(e.getX(), 0), timescaleComponent.getSize().width);
            final long newTime = Math.round((double) dx * viewport.getMaxEnd() / timescaleComponent.getSize().width);
            return Math.min(Math.max(newTime, 0), viewport.getMaxEnd());
        }
    }

}
