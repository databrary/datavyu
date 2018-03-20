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

import org.datavyu.controllers.component.MixerController;

public class ViewportModelImpl extends MixerComponentModelImpl implements ViewportModel {
    private volatile ViewportStateImpl viewport;

    public ViewportModelImpl(final MixerModel mixerModel) {
        super(mixerModel);
        resetViewport();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ViewportState getViewport() {
        return viewport;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resizeViewport(final long newStart, final double newWidth) {
        final long newEnd = (long) (Math.ceil(viewport.getResolution() * newWidth) + newStart);
        setViewport(newStart, newEnd, viewport.getMaxEnd(), newWidth);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setViewport(final long newViewStart,
                            final long newViewEnd,
                            final long newMaxEnd,
                            final double newWidth) {
        final ViewportState oldViewport;
        final ViewportState newViewport;

        synchronized (this) {
            // If no change in the viewport we are done here
            if (viewport != null
                    && viewport.getMaxEnd() == newMaxEnd
                    && viewport.getViewWidth() == newWidth
                    && viewport.getViewStart() == newViewStart
                    && viewport.getViewEnd() == newViewEnd) {
                return;
            }

            oldViewport = viewport;
            viewport = new ViewportStateImpl(newMaxEnd, newWidth, newViewStart, newViewEnd);
            newViewport = viewport;
        }

        firePropertyChange(ViewportState.NAME, oldViewport, newViewport);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setViewportMaxEnd(final long newMaxEnd, final boolean resetViewportWindow) {
        if (newMaxEnd <= 1) {
            resetViewport();
            return;
        }

        final ViewportState oldViewport;
        final ViewportState newViewport;

        synchronized (this) {
            final long viewStart = resetViewportWindow ? 0 : viewport.getViewStart();
            final long viewEnd = resetViewportWindow ? newMaxEnd : viewport.getViewEnd();

            if (viewport.getMaxEnd() == newMaxEnd && viewport.getViewStart() == viewStart && viewport.getViewEnd() == viewEnd) {
                return;
            }

            oldViewport = viewport;
            viewport = new ViewportStateImpl(newMaxEnd, viewport.getViewWidth(), viewStart, viewEnd);
            newViewport = viewport;
        }

        firePropertyChange(ViewportState.NAME, oldViewport, newViewport);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setViewportWindow(final long newStart, final long newEnd) {
        final ViewportState oldViewport;
        final ViewportState newViewport;

        synchronized (this) {
            if (viewport.getViewStart() == newStart && viewport.getViewEnd() == newEnd) {
                return;
            }

            oldViewport = viewport;
            viewport = new ViewportStateImpl(viewport.getMaxEnd(), viewport.getViewWidth(), newStart, newEnd);
            newViewport = viewport;
        }

        firePropertyChange(ViewportState.NAME, oldViewport, newViewport);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setViewportZoom(final double zoomLevel, final long centerTime) {
        final ViewportState oldViewport;
        final ViewportState newViewport;

        synchronized (this) {
            if (viewport.getZoomLevel() == zoomLevel) {
                return;
            }

            oldViewport = viewport;
            viewport = viewport.zoomViewport(zoomLevel, centerTime);
            newViewport = viewport;
        }

        firePropertyChange(ViewportState.NAME, oldViewport, newViewport);
    }

    /**
     * {@inheritDoc}
     */
    public void resetViewport() {
        final double newWidth;
        synchronized (this) {
            newWidth = (viewport != null) ? viewport.getViewWidth() : 0;
        }
        setViewport(0, MixerController.DEFAULT_DURATION, ViewportStateImpl.MINIMUM_MAX_END, newWidth);
    }
}
