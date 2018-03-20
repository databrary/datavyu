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
import org.datavyu.models.db.Cell;
import org.datavyu.models.db.CellListener;
import org.datavyu.models.db.DataStore;
import org.datavyu.models.db.CellValue;
import org.datavyu.util.ClockTimer;
import org.datavyu.util.ConfigurationProperties;
import org.datavyu.views.discrete.datavalues.MatrixRootView;
import org.datavyu.views.discrete.datavalues.TimeStampDataValueEditor.TimeStampSource;
import org.datavyu.views.discrete.datavalues.TimeStampTextField;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import javax.swing.*;
import javax.swing.Box.Filler;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Locale;


/**
 * Visual representation of a spreadsheet cell.
 */
public class SpreadsheetCell extends JPanel
        implements MouseListener, FocusListener, CellListener, ClockTimer.ClockListener {

    /**
     * Border to use if cell overlaps with another.
     */
    public static final Border OVERLAP_BORDER = new CompoundBorder(
            new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, ConfigurationProperties.DEFAULT_BORDER_COLOUR),
                    new MatteBorder(0, 0, 3, 0, ConfigurationProperties.getInstance().getSpreadSheetOverlapColor())),
            new MatteBorder(3,
                    3, 0, 3, ConfigurationProperties.getInstance().getSpreadSheetBackgroundColor()));

    /**
     * Width of spacer between onset and offset timestamps.
     */
    private static final int TIME_SPACER = 5;
    /**
     * Border to use when a cell is highlighted.
     */
    private static final Border HIGHLIGHT_BORDER = new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, ConfigurationProperties.DEFAULT_BORDER_COLOUR),
            new MatteBorder(3, 3, 3, 3, ConfigurationProperties.getInstance().getSpreadSheetSelectedColor()));
    /**
     * Border to use when a cell is highlighted and overlapping cell.
     */
    private static final Border HIGHLIGHT_OVERLAP_BORDER = new CompoundBorder(
            new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, ConfigurationProperties.DEFAULT_BORDER_COLOUR),
                    new MatteBorder(0, 0, 3, 0, ConfigurationProperties.getInstance().getSpreadSheetOverlapColor())),
            new MatteBorder(3,
                    3, 0, 3, ConfigurationProperties.getInstance().getSpreadSheetSelectedColor()));
    /**
     * Border to use when a cell is selected.
     */
    private static final Border FILL_OVERLAP_BORDER = HIGHLIGHT_OVERLAP_BORDER;
    /**
     * Border to use when a cell is selected.
     */
    private static final Border FILL_BORDER = new CompoundBorder(
            new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, ConfigurationProperties.DEFAULT_BORDER_COLOUR),
                    new MatteBorder(0, 0, 3, 0, ConfigurationProperties.getInstance().getSpreadSheetSelectedColor())),
            new MatteBorder(3,
                    3, 0, 3, ConfigurationProperties.getInstance().getSpreadSheetSelectedColor()));
    /**
     * Border to use for normal cell. No extra information to show.
     */
    private static final Border NORMAL_BORDER = new CompoundBorder(
            new CompoundBorder(
                    new MatteBorder(1, 0, 1, 0, ConfigurationProperties.DEFAULT_BORDER_COLOUR),
                    new MatteBorder(0, 0, 3, 0, ConfigurationProperties.getInstance().getSpreadSheetBackgroundColor())),
            new MatteBorder(3, 3, 0, 3,
                    ConfigurationProperties.getInstance().getSpreadSheetBackgroundColor()));
    /**
     * The logger for this class.
     */
    private static Logger logger = LogManager.getLogger(SpreadsheetCell.class);
    boolean isLaid = false;
    private boolean brandNew;
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
    private MatrixRootView dataPanel;
    /**
     * The Ordinal display component.
     */
    private JLabel ord;
    private Color timeHighlightColor = new Color(116, 242, 190);
    private Color pastTimeHighlightColor = new Color(247, 153, 153);
    /**
     * The Onset display component.
     */
    private TimeStampTextField onset;
    /**
     * The Offset display component.
     */
    private TimeStampTextField offset;
    /**
     * The cell that this SpreadsheetCell represents.
     */
    private Cell model;
    /**
     * Component that sets the width of the cell.
     */
    private Filler stretcher;
    /**
     * strut creates the gap between this cell and the previous cell.
     */
    private Filler strut;
    /**
     * Does this cell overlap another?
     */
    private boolean cellOverlap = false;
    /**
     * The spreadsheet cell selection listener.
     */
    private CellSelectionListener cellSelL;
    /**
     * Onset has been processed and layout position calculated.
     */
    private boolean onsetProcessed = false;
    private boolean beingProcessed = false;

    private SpreadsheetColumn parentColumn = null;

    public SpreadsheetCell(final DataStore cellDB,
                           final Cell cell,
                           final CellSelectionListener listener) {

        model = cell;
        setName(this.getClass().getSimpleName());

        ResourceMap rMap = Application.getInstance(Datavyu.class).getContext()
                .getResourceMap(SpreadsheetCell.class);

        // Check the selected state of the datacell
        // If it is already selected in the database, we need to inform
        // the selector, but not trigger a selection change or deselect others.
        cellSelL = listener;

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
        ord = new JLabel();
        ord.setFont(ConfigurationProperties.getInstance().getSpreadSheetLabelFont());
        ord.setForeground(ConfigurationProperties.getInstance().getSpreadSheetOrdinalForegroundColor());
        ord.setToolTipText(rMap.getString("ord.tooltip"));
        ord.addMouseListener(this);
        ord.setFocusable(true);

        onset = new TimeStampTextField(model, TimeStampSource.Onset);
        onset.setFont(ConfigurationProperties.getInstance().getSpreadSheetLabelFont());
        onset.setForeground(ConfigurationProperties.getInstance().getSpreadSheetTimeStampForegroundColor());
        onset.setToolTipText(rMap.getString("onset.tooltip"));
        onset.addFocusListener(this);
        onset.addMouseListener(this);
        onset.setName("onsetTextField");

        offset = new TimeStampTextField(model, TimeStampSource.Offset);
        offset.setFont(ConfigurationProperties.getInstance().getSpreadSheetLabelFont());
        offset.setForeground(ConfigurationProperties.getInstance().getSpreadSheetTimeStampForegroundColor());
        offset.setToolTipText(rMap.getString("offset.tooltip"));
        offset.addFocusListener(this);
        offset.addMouseListener(this);
        offset.setName("offsetTextField");

        dataPanel = new MatrixRootView(model, cell.getCellValue());
        dataPanel.setFont(ConfigurationProperties.getInstance().getSpreadSheetDataFont());
        dataPanel.setForeground(ConfigurationProperties.getInstance().getSpreadSheetForegroundColor());

        dataPanel.setOpaque(false);
        dataPanel.addFocusListener(this);
        dataPanel.addMouseListener(this);
        dataPanel.setName("cellValue");

        // Set the appearance of the spreadsheet cell.
        cellPanel.setBackground(ConfigurationProperties.getInstance().getSpreadSheetBackgroundColor());
        // Cell is highlighted by default.
        cellPanel.setBorder(NORMAL_BORDER);
        cellPanel.setLayout(new BorderLayout());

        // Set the apperance of the top panel and add child elements (ord, onset
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

        // Set the apperance of the data panel - add elements for dis6playing
        // the actual data of the panel.
        cellPanel.add(dataPanel, BorderLayout.CENTER);

        Dimension d = new Dimension(229, 0);
        stretcher = new Filler(d, d, d);
        cellPanel.add(stretcher, BorderLayout.SOUTH);

        Datavyu.getVideoController().getClockTimer().registerListener(this);

        brandNew = true;
    }

    public boolean isBeingProcessed() {
        return beingProcessed;
    }

    public void setBeingProcessed(boolean beingProcessed) {
        this.beingProcessed = beingProcessed;
    }

    public void forceCellRefresh() {
        model = model.getFreshCell();
    }

    /**
     * @return True if onset been processed and the layout position calculated.
     * false otherwise.
     */
    public boolean isOnsetProcessed() {
        return onsetProcessed;
    }

    /**
     * Set if onset has been processed. Used in the temporal layout algorithm.
     *
     * @param onsetProcessed true to mark that the onset has been processed.
     *                       False otherwise.
     */
    public void setOnsetProcessed(final boolean isOnsetProcessed) {
        onsetProcessed = isOnsetProcessed;
    }

    public boolean isLaid() {
        return isLaid;
    }

    public void setLaid(final boolean newStatus) {
        isLaid = newStatus;
    }

    public int getTemporalTop(final double ratio) {
        return (int) (getOnsetTicks() * ratio);
    }

    public int getTemporalBottom(final double ratio) {
        return (int) (getOffsetTicks() * ratio);
    }

    public int getTemporalSize(final double ratio) {
        return Math.max((int) ((getOffsetTicks() - getOnsetTicks()) * ratio), 0);
    }

    /**
     * @return The cell that this view element represents.
     */
    public Cell getCell() {
        model = model.getFreshCell();
        return model;
    }

    /**
     * @return onset view
     */
    public TimeStampTextField getOnset() {
        return onset;
    }

    /**
     * @return offset view
     */
    public TimeStampTextField getOffset() {
        return offset;
    }

    /**
     * Set the ordinal value.
     *
     * @param ordinal The new ordinal value to use with this cell.
     */
    public void setOrdinal(final Integer ordinal) {
        ord.setText(ordinal.toString());
    }

    /**
     * Get the onset ticks
     *
     * @return Onset time as a long.
     */
    public long getOnsetTicks() {
        return model.getOnset();
    }

    /**
     * Get the offset ticks
     *
     * @return Offset ticks as a long.
     */
    public long getOffsetTicks() {
        if (model.getOffset() < model.getOnset()) {
            return model.getOnset();
        }
        return model.getOffset();
    }

    /**
     * Get the actual offset ticks (even if less than onset).
     *
     * @return Offset ticks as a long.
     */
    public long getOffsetTicksActual() { return model.getOffset(); }
    
    public boolean isUpsideDown() {
        return model.getOffset() < model.getOnset() && model.getOffset() != 0;  
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
     * Mark the cell as selected in the database.
     *
     * @param sel The selection state to use when marking the cell. True if the
     *            cell is selected, false otherwise.
     * @Deprecated should manipulate the model directly, not the view.
     */
    @Deprecated
    public void selectCellInDB(final boolean sel) {
        model = model.getFreshCell();

        // Set the selection within the database.
        model.setSelected(sel);

        if (sel) {
            // method names don't reflect usage - we didn't really create
            // this cell just now.
            Datavyu.getProjectController().setLastCreatedCell(model);
            Datavyu.getProjectController().setLastSelectedCell(model);
            Datavyu.getVideoController().setOnsetField(model.getOnset());
            Datavyu.getVideoController().setOffsetField(model.getOffset());
        }
    }

    /**
     * Set the border of the cell.
     *
     * @param overlap true if the cell overlaps with the following cell, false
     *                otherwise.
     */
    public void setOverlapBorder(final boolean overlap) {
        cellOverlap = overlap;

        if (cellOverlap) {

            if (model.isHighlighted()) {
                cellPanel.setBorder(HIGHLIGHT_OVERLAP_BORDER);
            } else {
                cellPanel.setBorder(OVERLAP_BORDER);
            }
        } else {
            if (model.isHighlighted()) {
                cellPanel.setBorder(HIGHLIGHT_BORDER);
            } else {
                cellPanel.setBorder(NORMAL_BORDER);
            }
        }
    }

    /**
     * Get overlap.
     */
    public boolean getOverlapBorder(){
        return cellOverlap;
    }

    /**
     * @return The MatrixRootView of this cell.
     */
    public final MatrixRootView getDataView() {
        return dataPanel;
    }

    public void selectOnset() {
        onset.selectAll();
        offset.select(0, 0);
        dataPanel.select(0, 0);
        onset.requestFocusInWindow();
    }

    public void selectOffset() {
        offset.selectAll();
        onset.select(0, 0);
        dataPanel.select(0, 0);
        offset.requestFocusInWindow();
    }

    public void selectVal() {
        dataPanel.selectAll();
        onset.select(0, 0);
        offset.select(0, 0);
        dataPanel.requestFocusInWindow();
    }

    public void updateSelectionDisplay() {
        if (model.isHighlighted() && !Datavyu.getVideoController().getCellHighlightAndFocus()) {
            if (cellOverlap) {
                cellPanel.setBorder(HIGHLIGHT_OVERLAP_BORDER);
            } else {
                cellPanel.setBorder(HIGHLIGHT_BORDER);
            }
        } else if (model.isSelected()) {
            if (cellOverlap) {
                cellPanel.setBorder(FILL_OVERLAP_BORDER);
            } else {
                cellPanel.setBorder(FILL_BORDER);
            }
        } else {
            if (cellOverlap) {
                cellPanel.setBorder(OVERLAP_BORDER);
            } else {
                cellPanel.setBorder(NORMAL_BORDER);
            }
        }

        if (Datavyu.getVideoController().getCellHighlightAndFocus()) {
            if (model.isPastTimeWindow(Datavyu.getVideoController().getCurrentTime())) {
                cellPanel.setBackground(pastTimeHighlightColor);
            } else if (cellPanel.getBackground() == pastTimeHighlightColor) {
                cellPanel.setBackground(ConfigurationProperties.getInstance().getSpreadSheetBackgroundColor());
            }
        } else if (Datavyu.getVideoController().getCellHighlighting()) {
            if(model.isPastTimeWindow(Datavyu.getVideoController().getCurrentTime())) {
                cellPanel.setBackground(pastTimeHighlightColor);
            } else if(model.isInTimeWindow(Datavyu.getVideoController().getCurrentTime())) {
                cellPanel.setBackground(timeHighlightColor);
            } else {
                cellPanel.setBackground(ConfigurationProperties.getInstance().getSpreadSheetBackgroundColor());
            }
        } else {
            cellPanel.setBackground(ConfigurationProperties.getInstance().getSpreadSheetBackgroundColor());
        }





    }

    // *************************************************************************
    // VariableListener Overrides
    // *************************************************************************
    @Override
    public void offsetChanged(final long newOffset) {
        offset.setValue();
        if (model.isSelected()) {
            // Update the find windows to the newly selected cell's values
            Datavyu.getVideoController().setOnsetField(model.getOnset());
            Datavyu.getVideoController().setOffsetField(model.getOffset());
        }
    }

    @Override
    public void onsetChanged(final long newOnset) {
        onset.setValue();
        if (model.isSelected()) {
            Datavyu.getVideoController().setOnsetField(model.getOnset());
            Datavyu.getVideoController().setOffsetField(model.getOffset());
        }
    }

    @Override
    public void highlightingChange(final boolean isHighlighted) {
        updateSelectionDisplay();
        if (model.isSelected()) {
            // Update the find windows to the newly selected cell's values
            Datavyu.getVideoController().setOnsetField(model.getOnset());
            Datavyu.getVideoController().setOffsetField(model.getOffset());
        }
    }

    @Override
    public void selectionChange(final boolean isSelected) {
        updateSelectionDisplay();
        if (model.isSelected()) {
            // Update the find windows to the newly selected cell's values
            Datavyu.getVideoController().setOnsetField(model.getOnset());
            Datavyu.getVideoController().setOffsetField(model.getOffset());
        }
    }

    @Override
    public void valueChange(final CellValue newCellValue) {
        dataPanel.setMatrix(newCellValue);
        revalidate();
    }

    // *************************************************************************
    // MouseListener Overrides
    // *************************************************************************
    @Override
    public void mouseEntered(final MouseEvent me) {
    }

    @Override
    public void mouseExited(final MouseEvent me) {
    }

    @Override
    public void mousePressed(final MouseEvent me) {
        int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        boolean groupSel = (me.getModifiers() & keyMask) != 0;
        boolean contSel = (me.getModifiers() & ActionEvent.SHIFT_MASK) != 0;

        Class source = me.getSource().getClass();
        boolean isEditorSrc = (source.equals(TimeStampTextField.class)
                || (source.equals(MatrixRootView.class)));

        model = model.getFreshCell();

        // User has clicked in magic spot, without modifier. Clear
        // currently selected cells and select this cell.
        if (!isEditorSrc && !groupSel && !contSel) {
            ord.requestFocus();
            cellSelL.clearCellSelection();
            model.setHighlighted(!model.isSelected());

            if (model.isSelected()) {
                cellSelL.addCellToSelection(this);
            }

            // User has clicked on editor or magic spot with modifier. Add
            // this cell to the current selection.
        } else if (groupSel && !contSel) {
            ord.requestFocus();
            model.setHighlighted(!model.isSelected());

            if (model.isSelected()) {
                cellSelL.addCellToSelection(this);
            }

            // User has clicked on editor or magic spot with shift modifier.
            // Add this cell and everything in between the current selection.
        } else if (contSel) {
            ord.requestFocus();
            cellSelL.addCellToContinousSelection(this);

            // User has clicked somewhere in the cell without modifier. This
            // cell needs to be highlighted.
        } else {
            // Only change selection if not selected.
            if (!model.isHighlighted()) {
                // BugzID:320 - Deselect cells before selected cell contents.
                cellSelL.clearCellSelection();
                model.setHighlighted(true);
                cellSelL.setHighlightedCell(this);
            }
        }

        // Update the find windows to the newly selected cell's values
        Datavyu.getVideoController().setOnsetField(model.getOnset());
        Datavyu.getVideoController().setOffsetField(model.getOffset());
    }

    @Override
    public void mouseReleased(final MouseEvent me) {
    }

    @Override
    public void mouseClicked(final MouseEvent me) {
    }

    // *************************************************************************
    // FocusListener Overrides
    // *************************************************************************
    @Override
    public void focusGained(final FocusEvent e) {
        model = model.getFreshCell();

//        if (model.isHighlighted() && (cellPanel.getBorder().equals(NORMAL_BORDER)
//                || cellPanel.getBorder().equals(OVERLAP_BORDER))) {
            model.setSelected(true);
//        }
        Datavyu.getView().getSpreadsheetPanel().validate();
        Datavyu.getView().getSpreadsheetPanel().reorientView(this);

        if(parentColumn == null) {
            for (SpreadsheetColumn col : Datavyu.getView().getSpreadsheetPanel().getColumns()) {
                if (col.getVariable() == model.getVariable()) {
                    parentColumn = col;
                    break;
                }
            }
        }
        parentColumn.setSelected(true);
    }

    @Override
    public void focusLost(final FocusEvent e) {
        if (brandNew) model.setSelected(false);
        brandNew = false;

        if(parentColumn == null) {
            for (SpreadsheetColumn col : Datavyu.getView().getSpreadsheetPanel().getColumns()) {
                if (col.getVariable() == model.getVariable()) {
                    parentColumn = col;
                    break;
                }
            }
        }
        model.setSelected(false);
        parentColumn.setSelected(false);
    }

    @Override
    public boolean isFocusOwner() {
        return (onset.isFocusOwner() || offset.isFocusOwner() || dataPanel.isFocusOwner());
    }

    @Override
    public void requestFocus() {
        dataPanel.requestFocus();
    }

    // *************************************************************************
    // Parent Class Overrides
    // *************************************************************************
    @Override
    public void paint(final Graphics g) {
        // BugzID:474 - Set the size at paint time - somewhere else may have
        // altered the font.
        dataPanel.setFont(ConfigurationProperties.getInstance().getSpreadSheetDataFont());
        super.paint(g);
    }

    @Override
    public void clockForceSync(double clockTime) {}

    @Override
    public void clockPeriodicSync(double clockTime) {
        updateSelectionDisplay();
    }

    @Override
    public void clockStop(double clockTime) {

    }

    @Override
    public void clockStart(double clockTime) {

    }

    @Override
    public void clockRate(float rate) {

    }

    @Override
    public void clockBoundaryCheck(double clockTime) {

    }

    @Override
    public void clockSeekPlayback(double clockTime) {

    }
}
