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
import org.datavyu.models.component.NeedleConstants;
import org.datavyu.models.component.NeedleModelImpl;
import org.datavyu.models.component.ViewportState;
import org.datavyu.views.VideoController;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This class paints a timing needle.
 */
public final class NeedleComponent extends JComponent implements PropertyChangeListener {

    private static final long serialVersionUID = -6157748998316240030L;

    private GeneralPath needleMarker; // polygon
    private NeedleModelImpl needleModel;
    private MixerModel mixer;

    public NeedleComponent(final NeedleModelImpl needleModelImpl) {
        this.needleModel = needleModelImpl;
        this.mixer = needleModel.getMixerModel();
        needleModel.addPropertyChangeListener(this);
    }

    @Override
    public boolean contains(final Point p) {
        return contains(p.x, p.y);
    }

    @Override
    public boolean contains(int x, int y) {
        return needleMarker != null && needleMarker.contains(x, y);
    }

    public double getDataTrackTopY() {
        return NeedleConstants.NEEDLE_HEAD_HEIGHT;
    }

    public double getZoomWindowIndicatorTopY() {
        return getSize().height - needleModel.getZoomIndicatorHeight();
    }

    public double getTransitionAreaTopY() {
        return getZoomWindowIndicatorTopY() - needleModel.getTimescaleTransitionHeight();
    }

    @Override
    public void paintComponent(final Graphics g) {

        if ((needleModel == null) || (mixer == null)) {
            return;
        }

        ViewportState viewport = mixer.getViewportModel().getViewport();

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final double needleHeadWidth = NeedleConstants.NEEDLE_HEAD_WIDTH;

        final long currentTime = needleModel.getCurrentTime();
        double needlePositionX = viewport.computePixelXOffset(currentTime) + needleHeadWidth;
        final boolean isNeedleInViewport = viewport.isTimeInViewport(currentTime);

        if (isNeedleInViewport) {
            // draw the triangular needle head
            needleMarker = new GeneralPath();
            needleMarker.moveTo((float) (needlePositionX - needleHeadWidth), 0); // top-left
            needleMarker.lineTo((float) (needlePositionX + needleHeadWidth), 0); // top-right
            needleMarker.lineTo((float) needlePositionX, (float) getDataTrackTopY()); // bottom
            needleMarker.closePath();

            g2d.setColor(needleModel.getNeedleColor());
            g2d.fill(needleMarker);

            g2d.setColor(needleModel.getNeedleColor().darker());
            g2d.draw(needleMarker);

            // Draw the needle
            float x1 = (float) needlePositionX;
            float y1 = (float) getDataTrackTopY();
            float x2 = (float) needlePositionX;
            float y2 = (float) getTransitionAreaTopY();

            GeneralPath line = new GeneralPath();
            line.moveTo(x1, y1);
            line.lineTo(x2, y2);

            g2d.setStroke(new BasicStroke((float) NeedleConstants.NEEDLE_WIDTH));
            g2d.draw(line);
        }

        g2d.setColor(needleModel.getNeedleColor().darker());

        // paint the needle transition area
        final double zoomWindowIndicatorX = ((double) currentTime * viewport.getViewWidth() / viewport.getMaxEnd()) + Math.ceil(NeedleConstants.NEEDLE_HEAD_WIDTH);

        final int transitionCurveBottomWeight = 10;

        GeneralPath shape = new GeneralPath();
        shape.moveTo(needlePositionX, getTransitionAreaTopY());
        shape.curveTo(needlePositionX,
                (getTransitionAreaTopY()
                        + (getZoomWindowIndicatorTopY() * transitionCurveBottomWeight))
                        / (transitionCurveBottomWeight + 1), zoomWindowIndicatorX,
                getTransitionAreaTopY(), zoomWindowIndicatorX, getZoomWindowIndicatorTopY());
        final float strokeWidth = (float) NeedleConstants.NEEDLE_WIDTH / (isNeedleInViewport ? 2.0f : 5.0f);
        g2d.setStroke(new BasicStroke(strokeWidth));
        g2d.draw(shape);

        // paint the needle in the zoom window indicator
        GeneralPath needleMarker = new GeneralPath();
        needleMarker.moveTo(zoomWindowIndicatorX, getZoomWindowIndicatorTopY());
        needleMarker.lineTo(zoomWindowIndicatorX, getSize().height);

        g2d.setStroke(new BasicStroke((float) NeedleConstants.NEEDLE_WIDTH));
        g2d.draw(needleMarker);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == needleModel) {
            repaint();
            updateToolTipText();
        }
    }

    private void updateToolTipText() {
        setToolTipText(VideoController.formatTime(needleModel.getCurrentTime()));
    }
}
