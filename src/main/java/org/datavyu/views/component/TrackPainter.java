/*
 * Copyright (c) 2011 OpenSHAPA Foundation, http://openshapa.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.datavyu.views.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;

import org.datavyu.models.component.MixerModel;
import org.datavyu.models.component.TrackModel;
import org.datavyu.models.component.ViewportState;


/**
 * This class is used to paint a track and its information.
 */
public abstract class TrackPainter extends JComponent
    implements PropertyChangeListener {

    /** Fill colour of a carriage in the unselected/normal state */
    private static final Color DEFAULT_NORMAL_CARRIAGE_COLOR = new Color(169,
            218, 248);

    /** Outline colour of a carriage in the unselected/normal state */
    private static final Color DEFAULT_NORMAL_OUTLINE_COLOR = new Color(129,
            167, 188);

    /** Fill colour of a carriage in the selected state */
    private static final Color DEFAULT_SELECTED_CARRIAGE_COLOR = new Color(
            138, 223, 162);

    /** Outline colour of a carriage in the selected state */
    private static final Color DEFAULT_SELECTED_OUTLINE_COLOR = new Color(105,
            186, 128);

    /** Color schemes. */
    /** Normal carriage color. */
    private Color normalCarriageColor;

    /** Normal carriage outline color. */
    private Color normalOutlineColor;

    /** Selected carriage color. */
    private Color selectedCarriageColor;

    /** Selected carriage outline color. */
    private Color selectedOutlineColor;

    /** Painted region of the carriage */
    private GeneralPath carriagePolygon;

    /** Model containing information specific to the track painter. */
    private TrackModel trackModel;

    /** Model containing information about visibility parameters. */
    protected MixerModel mixerModel;

    /**
     * Creates a new TrackPainter.
     */
    public TrackPainter() {
        super();

        normalCarriageColor = DEFAULT_NORMAL_CARRIAGE_COLOR;
        normalOutlineColor = DEFAULT_NORMAL_OUTLINE_COLOR;
        selectedCarriageColor = DEFAULT_SELECTED_CARRIAGE_COLOR;
        selectedOutlineColor = DEFAULT_SELECTED_OUTLINE_COLOR;
    }

    public final void setMixerView(final MixerModel mixerModel) {
        this.mixerModel = mixerModel;
        mixerModel.getViewportModel().addPropertyChangeListener(this);
    }

    /**
     * Set the track model.
     *
     * @param model The new track model to use.
     */
    public final void setTrackModel(final TrackModel model) {
        trackModel = model;
        repaint();
    }

    /**
     * @return The polygon representing the carriage.
     */
    public final GeneralPath getCarriagePolygon() {
        return carriagePolygon;
    }

    @Override public final boolean isOpaque() {
        return true;
    }

    @Override protected final void paintComponent(final Graphics g) {
        final ViewportState viewport = mixerModel.getViewportModel().getViewport();
        final Graphics2D g2d = (Graphics2D) g;

        Dimension size = getSize();

        // Paints the background
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, size.width, size.height);

        // If there is an error with track information, don't paint the carriage
        if (trackModel.isErroneous()) {
            g2d.setColor(Color.red);

            FontMetrics fm = g2d.getFontMetrics();
            String errorMessage =
                "Track timing information could not be calculated.";
            int width = fm.stringWidth(errorMessage);
            g2d.drawString(errorMessage, (size.width / 2) - (width / 2),
                (size.height / 2) - (fm.getAscent() / 2));

            return;
        }

        // paint the carriage
        final int carriageHeight = (int) (size.getHeight() * 7D / 10D);
        final int carriageYOffset = (int) (size.getHeight() * 2D / 10D);

        final double startXPos = viewport.computePixelXOffset(trackModel.getOffset());
        final double endXPos = viewport.computePixelXOffset(trackModel.getDuration() + trackModel.getOffset());

        carriagePolygon = new GeneralPath();
        carriagePolygon.moveTo(startXPos, carriageYOffset); // top left corner
        carriagePolygon.lineTo(endXPos, carriageYOffset); // top right
        carriagePolygon.lineTo(endXPos, carriageYOffset + carriageHeight); // bottom right
        carriagePolygon.lineTo(startXPos, carriageYOffset + carriageHeight); // bottom left
        carriagePolygon.closePath();

        final Color carriageColor = trackModel.isSelected() ? selectedCarriageColor : normalCarriageColor;
        final Color outlineColor = trackModel.isSelected() ? selectedOutlineColor : normalOutlineColor;

	    g2d.setColor(carriageColor);
        g2d.fill(carriagePolygon);

        g2d.setColor(outlineColor);
        g2d.draw(carriagePolygon);

        // Paint custom information, if any.
        Graphics g3 = g.create();
        try {
            paintCustom(g3);
        } finally {
            g3.dispose();
            g3 = null;
        }

        // paint the bookmarks
        for (Long bookmark : trackModel.getMarkers()) {
	        final double bookmarkXPos = viewport.computePixelXOffset(trackModel.getOffset() + bookmark);
	
	        GeneralPath bookmarkLine = new GeneralPath();
	        bookmarkLine.moveTo(bookmarkXPos, carriageYOffset);
	        bookmarkLine.lineTo(bookmarkXPos, carriageYOffset + carriageHeight);
	        g2d.draw(bookmarkLine);
	
	        final double diamondSize = 10;
	        GeneralPath bookmarkDiamond = new GeneralPath();
	        bookmarkDiamond.moveTo(bookmarkXPos, carriageYOffset - diamondSize - 1); // top
	        bookmarkDiamond.lineTo(bookmarkXPos + diamondSize / 2, carriageYOffset - diamondSize / 2 - 1); // right
	        bookmarkDiamond.lineTo(bookmarkXPos, carriageYOffset - 1); // bottom
	        bookmarkDiamond.lineTo(bookmarkXPos - diamondSize / 2, carriageYOffset - diamondSize / 2 - 1); // left
	        bookmarkDiamond.closePath();
	
	        g2d.setColor(carriageColor);
            g2d.fill(bookmarkDiamond);

            g2d.setColor(outlineColor);
            g2d.draw(bookmarkDiamond);
        }
    }
    
    /**
     * Additional painting over the track painter.
     *
     * @param g Graphics object.
     */
    protected abstract void paintCustom(final Graphics g);

    public void propertyChange(final PropertyChangeEvent evt) {
        if (evt.getSource() == mixerModel.getViewportModel()) {
            repaint();
        }
    }
}
