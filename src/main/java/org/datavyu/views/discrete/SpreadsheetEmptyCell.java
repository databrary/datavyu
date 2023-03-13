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
package org.datavyu.views.discrete;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.Datavyu;
import org.datavyu.controllers.CreateNewCellController;
import org.datavyu.models.db.Variable;
import org.datavyu.util.ConfigProperties;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import javax.swing.*;
import javax.swing.Box.Filler;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


/**
 * Visual representation of a spreadsheet cell.
 */
public class SpreadsheetEmptyCell extends JPanel implements MouseListener {

    /**
     * Width of spacer between onset and offset timestamps.
     */
    private static final int TIME_SPACER = 5;

    private static final int ALPHA = 70;

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LogManager.getLogger(SpreadsheetEmptyCell.class);

    /**
     * Border to use for normal cell. No extra information to show.
     */
    private static final Border NORMAL_BORDER = new MatteBorder(0, 0, 1, 1, ConfigProperties.DEFAULT_BORDER_COLOUR);

    /**
     * The panel that displays the cell.
     */
    private JPanel cellPanel;

    /**
     * A panel for holding the header to the cell.
     */
    private JPanel topPanel;

    /**
     * A panel for holding the value of the cell.
     */
    private JLabel dataPanel;

    /**
     * The Ordinal display component.
     */
    private JLabel ord;

    /**
     * The Onset display component.
     */
    private JLabel onset;

    /**
     * The Offset display component.
     */
    private JLabel offset;

    /**
     * Component that sets the width of the cell.
     */
    private Filler stretcher;

    /**
     * strut creates the gap between this cell and the previous cell.
     */
    private Filler strut;

    /**
     * Variable model for what we are adding cells too.
     */
    private Variable model;

    /**
     * The height of the visible portion of the cell requested by the active
     * SheetLayout.
     */
    private int layoutPreferredHeight;

    /**
     * Creates new Empty SpreadsheetCell stub.
     */
    public SpreadsheetEmptyCell(final Variable newCol) {
        setName(this.getClass().getSimpleName());

        ResourceMap rMap = Application.getInstance(Datavyu.class).getContext()
                .getResourceMap(SpreadsheetCell.class);

        cellPanel = new JPanel();
        cellPanel.addMouseListener(this);
        strut = new Filler(new Dimension(0, 0),
                new Dimension(0, 0),
                new Dimension(Short.MAX_VALUE, 0));

        setLayout(new BorderLayout());
        this.add(strut, BorderLayout.NORTH);
        this.add(cellPanel, BorderLayout.CENTER);

        // Build components used for the spreadsheet cell.
        topPanel = new JPanel();
        topPanel.addMouseListener(this);
        ord = new JLabel("+");
        ord.setFont(ConfigProperties.getInstance().getSpreadSheetLabelFont());
        ord.setForeground(addAlpha(ConfigProperties.getInstance().getSpreadSheetOrdinalForegroundColor(), ALPHA));
        ord.setToolTipText(rMap.getString("ord.tooltip"));
        ord.addMouseListener(this);
        ord.setFocusable(true);

        onset = new JLabel("--:--:--:---");
        onset.setFont(ConfigProperties.getInstance().getSpreadSheetLabelFont());
        onset.setForeground(addAlpha(ConfigProperties.getInstance().getSpreadSheetTimeStampForegroundColor(), ALPHA));
        onset.setToolTipText(rMap.getString("onset.tooltip"));
        onset.addMouseListener(this);
        onset.setName("onsetTextField");

        offset = new JLabel("--:--:--:---");
        offset.setFont(ConfigProperties.getInstance().getSpreadSheetLabelFont());
        offset.setForeground(addAlpha(ConfigProperties.getInstance().getSpreadSheetTimeStampForegroundColor(), ALPHA));
        offset.setToolTipText(rMap.getString("offset.tooltip"));
        offset.addMouseListener(this);
        offset.setName("offsetTextField");

        dataPanel = new JLabel(rMap.getString("empty.text"));
        dataPanel.setFont(ConfigProperties.getInstance().getSpreadSheetLabelFont());
        dataPanel.setForeground(addAlpha(ConfigProperties.getInstance().getSpreadSheetForegroundColor(), ALPHA));

        dataPanel.setOpaque(false);
        dataPanel.addMouseListener(this);
        dataPanel.setName("cellValue");

        // Set the appearance of the spreadsheet cell.
        cellPanel.setBackground(addAlpha(ConfigProperties.getInstance().getSpreadSheetBackgroundColor(), ALPHA));
        cellPanel.setBorder(NORMAL_BORDER);
        cellPanel.setLayout(new BorderLayout());

        // Set the appearance of the top panel and add child elements (ord, onset
        // and offset).
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        cellPanel.add(topPanel, BorderLayout.NORTH);
        topPanel.add(ord);

        Component strut1 = Box.createHorizontalStrut(TIME_SPACER);
        topPanel.add(strut1);

        Component glue = Box.createGlue();
        topPanel.add(glue);

        topPanel.add(onset);

        Component strut2 = Box.createHorizontalStrut(TIME_SPACER);
        topPanel.add(strut2);
        topPanel.add(offset);

        // Set the appearance of the data panel - add elements for dis6playing
        // the actual data of the panel.
        cellPanel.add(dataPanel, BorderLayout.CENTER);

        Dimension d = new Dimension(229, 0);
        stretcher = new Filler(d, d, d);
        cellPanel.add(stretcher, BorderLayout.SOUTH);

        model = newCol;
    }

    private static Color addAlpha(final Color col, final int alpha) {
        return new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha);
    }

    /**
     * Override Maximum size to return the preferred size or the active layouts
     * preferred height, whichever is greater.
     *
     * @return the maximum size of the cell.
     */
    @Override
    public final Dimension getMaximumSize() {
        Dimension mysize = super.getPreferredSize();

        if ((mysize != null) && (mysize.height < (layoutPreferredHeight + strut.getHeight()))) {
            mysize = new Dimension(mysize.width, (layoutPreferredHeight + strut.getHeight()));
        }

        return mysize;
    }

    /**
     * Override Preferred size to return the maximum size (which takes into
     * account the super.preferred size. See getMaximumSize.
     *
     * @return the preferred size of the cell.
     */
    @Override
    public final Dimension getPreferredSize() {
        return getMaximumSize();
    }

    /**
     * Override Minimum size to return the maximum size (which takes into
     * account the super.preferred size. See getMaximumSize.
     *
     * @return the minimum size of the cell.
     */
    @Override
    public final Dimension getMinimumSize() {
        return getMaximumSize();
    }

    /**
     * Set the width of the SpreadsheetCell.
     *
     * @param width New width of the SpreadsheetCell.
     */
    public void setWidth(final int width) {
        Dimension d = new Dimension(width, 0);
        stretcher.changeShape(d, d, d);
    }

    /**
     * The action to invoke when the mouse enters this component.
     *
     * @param me The mouse event that triggered this action.
     */
    @Override
    public void mouseEntered(final MouseEvent me) {
    }

    /**
     * The action to invoke when the mouse exits this component.
     *
     * @param me The mouse event that triggered this action.
     */
    @Override
    public void mouseExited(final MouseEvent me) {
    }

    /**
     * The action to invoke when a mouse button is pressed.
     *
     * @param me The mouse event that triggered this action.
     */
    @Override
    public void mousePressed(final MouseEvent me) {
    }

    /**
     * The action to invoke when a mouse button is released.
     *
     * @param me The mouse event that triggered this action.
     */
    @Override
    public void mouseReleased(final MouseEvent me) {
    }

    /**
     * The action to invoke when a mouse button is clicked.
     *
     * @param me The mouse event that triggered this action.
     */
    @Override
    public void mouseClicked(final MouseEvent me) {
        LOGGER.info("Pressed empty cell");
        CreateNewCellController controller = new CreateNewCellController();
        controller.createDefaultCell(model);
    }

    /**
     * Method to call when painting the component.
     *
     * @param g
     */
    @Override
    public void paint(final Graphics g) {
        // BugzID:474 - Set the size at paint time - somewhere else may have
        // altered the font.
        dataPanel.setFont(ConfigProperties.getInstance().getSpreadSheetDataFont());
        super.paint(g);
    }
}
