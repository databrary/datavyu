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
import org.datavyu.models.db.*;
import org.datavyu.undoableedits.ChangeNameVariableEdit;
import org.datavyu.util.ClockTimer;
import org.datavyu.util.ConfigurationProperties;
import org.datavyu.util.Constants;
import org.datavyu.util.DragAndDrop.TransparentPanel;
import org.jdesktop.application.Action;

import javax.swing.*;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * This class maintains the visual representation of the column in the
 * Spreadsheet window.
 */
public final class SpreadsheetColumn extends JLabel implements VariableListener, MouseListener, MouseMotionListener,
        ClockTimer.ClockListener {

    /** The logger for this class */
    private static Logger logger = LogManager.getLogger(SpreadsheetColumn.class);

    /**
     * Default column width.
     */
    public static final int DEFAULT_COLUMN_WIDTH = 230;

    /**
     * Default column height.
     */
    public static final int DEFAULT_HEADER_HEIGHT = 25;

    /**
     * The logger for this class.
     */
    private static Logger LOGGER = LogManager.getLogger(SpreadsheetColumn.class);

    /**
     * Database reference.
     */
    private DataStore dataStore;

    /**
     * Reference to the variable.
     */
    private Variable variable;

    /**
     * ColumnDataPanel this column manages.
     */
    private ColumnDataPanel datapanel;

    /**
     * Width of the column in pixels.
     */
    private int width = DEFAULT_COLUMN_WIDTH;

    /**
     * Selected state.
     */
    private boolean selected = false;

    /**
     * Background color of the header when unselected.
     */
    private Color backColor;

    /**
     * Can the column be dragged?
     */
    private boolean draggable;

    /**
     * Can the column be moved?
     */
    private boolean moveable;

    /**
     * cell selection listener to notify of cell selection changes.
     */
    private CellSelectionListener cellSelList;

    /**
     * column selection listener to notify of column selection changes.
     */
    private ColumnSelectionListener columnSelList;

    /**
     * column visibility listener to notify of column visibility changes.
     */
    private ColumnVisibilityListener columnVisList;

    /**
     * Layout state: The ordinal we are working on for this column.
     */
    private int workingOrd = 0;

    /**
     * Layout state: The height of the column data area in pixels.
     */
    private int dataHeight = 0;

    /**
     * Layout state: The padding to apply to the onset of the current working cell.
     */
    private int onsetPadding = 0;

    /**
     * Layout state: The padding to apply to the offset of the current working cell.
     */
    private int offsetPadding = 0;

    private TransparentPanel transparentPanel = (TransparentPanel) Datavyu.getView().getFrame().getGlassPane();

    private int previouslyFocusedCellIdx = -1;

    /**
     * Creates new SpreadsheetColumn.
     *
     * @param db       Database reference.
     * @param var    the variable this column displays.
     * @param cellSelL Spreadsheet cell selection listener to notify
     * @param colSelL  Column selection listener to notify.
     */
    public SpreadsheetColumn(final DataStore db,
                             final Variable var,
                             final CellSelectionListener cellSelL,
                             final ColumnSelectionListener colSelL,
                             final ColumnVisibilityListener colVisL) {
        this.dataStore = db;
        this.variable = var;
        this.cellSelList = cellSelL;
        this.columnSelList = colSelL;
        this.columnVisList = colVisL;

        setOpaque(true);
        setHorizontalAlignment(JLabel.CENTER);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, Constants.BORDER_SIZE, Color.black));
        backColor = getBackground();
        setMinimumSize(this.getHeaderSize());
        setPreferredSize(this.getHeaderSize());
        setMaximumSize(this.getHeaderSize());

        String typeString = "";
        if (var.getRootNode().type != Argument.Type.MATRIX) typeString = "  (" + var.getRootNode().type + ")";
        setText(var.getName() + typeString); //typeString for matrices is empty. Only displayed for non-matrix types (Text, Nominal)

        datapanel = new ColumnDataPanel(db, width, var, cellSelL);
        this.setVisible(!var.isHidden());
        datapanel.setVisible(!var.isHidden());

        Datavyu.getVideoController().getClockTimer().registerListener(this);
    }

    /**
     * @return The onset padding to use for the next cell you are laying in this
     * column
     */
    public int getWorkingOnsetPadding() {
        return onsetPadding;
    }

    /**
     * @param padding The working onset padding to use for cells in this column.
     */
    public void setWorkingOnsetPadding(final int padding) {
        onsetPadding = padding;
    }

    /**
     * @return The offset padding to use for the next cell you are laying in
     * this column.
     */
    public int getWorkingOffsetPadding() {
        return offsetPadding;
    }

    /**
     * @param padding The working offset padding to use for cells in this column.
     */
    public void setWorkingOffsetPadding(final int padding) {
        offsetPadding = padding;
    }

    /**
     * @return The height in pixels of the cells that have been laid in this
     * column
     */
    public int getWorkingHeight() {
        return dataHeight;
    }

    /**
     * @param newHeight The height in pixels of the cells that have been laid
     *                  in this column.
     */
    public void setWorkingHeight(final int newHeight) {
        dataHeight = newHeight;
    }

    /**
     * @return The ordinal of the current cell that is being laid in this
     * column.
     */
    public int getWorkingOrd() {
        return workingOrd;
    }

    /**
     * @param newOrd Set the ordinal value of the current cell that we are about
     *               to lay.
     */
    public void setWorkingOrd(final int newOrd) {
        workingOrd = newOrd;
    }

    /**
     * @return The next cell that needs to be laid in the column.
     */
    public SpreadsheetCell getWorkingTemporalCell() {
        if (workingOrd < datapanel.getNumCells()) {
            return datapanel.getCellTemporally(workingOrd);
        }

        return null;
    }

    public List<SpreadsheetCell> getOverlappingCells() {
        ArrayList<SpreadsheetCell> workingList = new ArrayList<SpreadsheetCell>();
        int currentOrd = workingOrd + 1;
        SpreadsheetCell currentCell = datapanel.getCellTemporally(workingOrd);

        while (currentOrd < datapanel.getNumCells() && currentCell.getOffsetTicks() > datapanel.getCellTemporally(currentOrd).getOnsetTicks()) {
            workingList.add(datapanel.getCellTemporally(currentOrd));
            currentOrd++;
        }

        return workingList;
    }

    public Variable getVariable() {
        return variable;
    }

    public void setAllCellsProcessed(boolean p) {
        for (SpreadsheetCell c : getCells()) {
            c.setBeingProcessed(p);
        }
    }

    /**
     * Registers this spreadsheet column with everything that needs to notify
     * this class of events.
     */
    public void registerListeners() {
        addMouseListener(this);
        addMouseMotionListener(this);
        variable.addListener(this);
        datapanel.registerListeners();
    }

    /**
     * Deregisters this spreadsheet column with everything that is currently
     * notiying it of events.
     */
    public void deregisterListeners() {
        removeMouseListener(this);
        removeMouseMotionListener(this);
        variable.removeListener(this);
        datapanel.deregisterListeners();
    }

    /**
     * Opens an input dialog to change a variable name.
     *
     * @throws HeadlessException
     */
    public void showChangeVarNameDialog() throws HeadlessException {
        //Edit variable name on double click
        String newName = "";

        boolean exitFlag = false;
        while (!exitFlag) {
            newName = (String) JOptionPane.showInputDialog(null, null, "New column name",
                    JOptionPane.PLAIN_MESSAGE, null, null, getColumnName());

            if (newName != null) {
                try {
                    setColumnName(newName);
                    exitFlag = true;

                } catch (UserWarningException ex) {
                    exitFlag = false;
                }

            } else {
                exitFlag = true;
            }
        }
    }

    /**
     * @return Column Header size as a dimension.
     */
    public Dimension getHeaderSize() {
        return new Dimension(getWidth(), DEFAULT_HEADER_HEIGHT);
    }

    /**
     * Clears the display components from the spreadsheet column.
     */
    public void clear() {
        datapanel.clear();
    }

    /**
     * @return Column Width in pixels.
     */
    @Override
    public int getWidth() {
        return width;
    }

    /**
     * @param colWidth Column width to set in pixels.
     */
    public void setWidth(final int colWidth) {
        LOGGER.info("set column width");
        width = colWidth;

        Dimension dim = getHeaderSize();
        setPreferredSize(dim);
        setMaximumSize(dim);
        revalidate();

        datapanel.setWidth(width);
        datapanel.revalidate();
    }

    /**
     * @return selected status of column
     */
    public boolean getSelected() {
        return selected;
    }

    /**
     * @return The datapanel.
     */
    public ColumnDataPanel getDataPanel() {
        return datapanel;
    }

    /**
     * Set the selected state for the DataColumn this displays and clear all other cells and columns.
     *
     * @param isSelected Selected state.
     */
    public void setExclusiveSelected(final boolean isSelected) {
        LOGGER.info("select column");
        cellSelList.clearCellSelection();
        columnSelList.clearColumnSelection();
        setSelected(isSelected);
    }

    /**
     * @return selection status of underlying variable
     */
    public boolean isSelected() {
        return variable.isSelected();
    }

    /**
     * Set the selected state for the DataColumn this displays.
     *
     * @param isSelected Selected state.
     */
    public void setSelected(final boolean isSelected) {
        LOGGER.info("select column");
        variable.setSelected(isSelected);
        this.selected = isSelected;

        if (selected) {
            setBackground(ConfigurationProperties.getInstance().getSpreadSheetSelectedColor());
        } else {
            setBackground(backColor);
        }
        repaint();
    }

    /**
     * Set the preferred size of the column.
     *
     * @param bottom Number of pixels to set.
     */
    public void setBottomBound(final int bottom) {

        if (bottom < 0) {
            datapanel.setPreferredSize(null);
        } else {
            datapanel.setPreferredSize(new Dimension(this.getWidth(), bottom));
        }
    }

    /**
     * @return The SpreadsheetCells in this column.
     */
    public List<SpreadsheetCell> getCells() {
        return datapanel.getCells();
    }

    /**
     * Gets a specific spreadsheet cell temporally.
     *
     * @param index The index of the spreadsheet cell you wish to retrieve.
     * @return The nth spreadsheet cell temporally.
     */
    public SpreadsheetCell getCellTemporally(final int index) {
        return datapanel.getCellTemporally(index);
    }

    /**
     * @return The Spreadsheet cells in this column temporally.
     */
    public List<SpreadsheetCell> getCellsTemporally() {
        return datapanel.getCellsTemporally();
    }

    public SpreadsheetCell getNearestCellTemporally(SpreadsheetCell sc) {
        SpreadsheetCell closestCell = null;
        for(SpreadsheetCell c : datapanel.getCellsTemporally()) {
            if(closestCell == null) {
                closestCell = c;
            } else {
                if(Math.abs(sc.getOnsetTicks() - c.getOnsetTicks()) < Math.abs(sc.getOnsetTicks()
                        - closestCell.getOnsetTicks())) {
                    closestCell = c;
                }
            }
        }
        return closestCell;
    }

    @Action
    public void addNewCellToVar() {
//        new NewVariableController();
    }

    /**
     * @return The header name of this SpreadsheetColumn.
     */
    public String getColumnName() {
        return variable.getName();
    }

    private void focusNextCell() {
        long time = Datavyu.getVideoController().getCurrentTime();
        List<SpreadsheetCell> tempCells = getCellsTemporally();
        for(int i = 0; i < tempCells.size(); i++) {
            SpreadsheetCell c = tempCells.get(i);
            if(c.getCell().isInTimeWindow(time)) {
                if(!c.isFocusOwner()) {
                    if(c.getCell().getCellValue() instanceof MatrixCellValue) {
                        int firstEmpty = -1;
                        List<CellValue> args = ((MatrixCellValue) c.getCell().getCellValue()).getArguments();
                        for(int j = 0; j < args.size(); j++) {
                            if(args.get(j).isEmpty()) {
                                firstEmpty = j;
                                break;
                            }
                        }
                        if(firstEmpty > -1) {
                            c.requestFocus();
                            c.getDataView().getEdTracker().setEditor(c.getDataView().getEdTracker().getEditorAtArgIndex(firstEmpty));
                        } else {
                            c.requestFocus();
                        }
                    } else {
                        if(c.getCell().getCellValue().isEmpty()) {
                            c.requestFocus();
                        }
                    }
                }
                if(c.getCell().getOnset() > time) {
                    break;
                }
                break;
            }
        }
    }

    public void setColumnName(final String newName) throws UserWarningException {
        try {
            // Make sure this column name isn't already in the column
            for (Variable v : dataStore.getAllVariables()) {
                if (v.getName().equals(newName)) {
                    Datavyu.getApplication().showWarningDialog("Error: Column name already exists.");
                    return;
                }
            }
            variable.setName(newName);
            UndoableEdit edit = new ChangeNameVariableEdit(variable.getName(), newName);
            Datavyu.getView().getComponent().revalidate();
            Datavyu.getView().getUndoSupport().postEdit(edit);
        } catch (UserWarningException uwe) {
            Datavyu.getApplication().showWarningDialog(uwe);
            throw new UserWarningException();
        }

    }

    // *************************************************************************
    // Parent Class Overrides
    // *************************************************************************
    @Override
    public void requestFocus() {

        /**
         * Request focus for this column. It will request focus for the first
         * SpreadsheetCell in the column if one exists. If no cells exist it
         * will request focus for the datapanel of the column.
         */
        if (datapanel.getCells().size() > 0) {
            datapanel.getCells().get(0).requestFocusInWindow();
        } else {
            datapanel.requestFocusInWindow();
        }
    }

    // *************************************************************************
    // VariableListener Overrides
    // *************************************************************************
    @Override
    public void nameChanged(final String newName) {
        String headerText = newName;
        if (variable.getRootNode().type != Argument.Type.MATRIX) headerText += variable.getRootNode().type;
        this.setText(headerText);
    }

    @Override
    public void visibilityChanged(final boolean isHidden) {
        setVisible(!isHidden);
        this.datapanel.setVisible(!isHidden);
        this.setSelected(false);
        columnVisList.columnVisibilityChanged();
    }

    @Override
    public void cellInserted(final Cell newCell) {
        datapanel.insertCell(dataStore, newCell, cellSelList);
    }

    @Override
    public void cellRemoved(final Cell deletedCell) {
        datapanel.deleteCell(deletedCell);
        for(SpreadsheetCell c : getCellsTemporally()) {
            if(c.getOnsetTicks() >= deletedCell.getOnset()) {
                c.requestFocus();
                break;
            }
        }
    }

    // *************************************************************************
    // MouseListener Overrides
    // *************************************************************************
    @Override
    public void mouseEntered(final MouseEvent me) {}

    @Override
    public void mouseExited(final MouseEvent me) {
    }

    @Override
    public void mousePressed(final MouseEvent me) {
        if(moveable && !draggable) {
            System.out.println("Pressed X: " + me.getX());
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    @Override
    public void mouseReleased(final MouseEvent me) {
        // BugzID:301 - Fix dragging columns
        // Call column moving routine if user dragged mouse across columns.

        if (moveable) {

            Component c = me.getComponent();

            Point destination = (Point) me.getPoint().clone();
            SwingUtilities.convertPointToScreen(destination, c);
            SwingUtilities.convertPointFromScreen(destination, transparentPanel);

            transparentPanel.setDestination(destination);
            transparentPanel.setVisible(false);
            transparentPanel.setImage(null);

            int x = me.getX();
            logger.info("Released X: " + x);
            // Iterate over the spreadsheet columns, starting at current column, to figure out how many
            // positions to shift by.
            SpreadSheetPanel sp = (SpreadSheetPanel) Datavyu.getView().getComponent();
            List<SpreadsheetColumn> cols = sp.getVisibleColumns();
            ListIterator<SpreadsheetColumn> itr = cols.listIterator(cols.indexOf(this));

            final int columnWidth = this.getWidth();

            SpreadsheetColumn swapCol;
            if (x > columnWidth) {
                swapCol = itr.next();
                while (x > swapCol.getWidth()) {
                    x -= swapCol.getWidth();
                    if (itr.hasNext()) swapCol = itr.next();
                    else break;
                }
                sp.moveColumn(this.getVariable(), swapCol.getVariable());
            } else if (x < 0 && itr.hasPrevious()) {
                swapCol = itr.previous();
                while (x < 0 && itr.hasPrevious()) {
                    x += swapCol.getWidth();
                    if (itr.hasPrevious() && x < 0) swapCol = itr.previous();
                    else break;
                }
                sp.moveColumn(this.getVariable(), swapCol.getVariable());
            }

            // Update globals
            moveable = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    @Override
    public void mouseClicked(final MouseEvent me) {
        if (me.getClickCount() == 2) {
            showChangeVarNameDialog();
        }
        else {
            int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

            boolean groupSel = (((me.getModifiers() & ActionEvent.SHIFT_MASK)
                    != 0) || ((me.getModifiers() & keyMask) != 0));
            boolean curSelected = this.isSelected();

            if (!groupSel) {
                this.columnSelList.clearColumnSelection();
            }

            this.setSelected(!curSelected);
            this.columnSelList.addColumnToSelection(this);
        }

        me.consume();
    }

    // *************************************************************************
    // MouseMotionListener Overrides
    // *************************************************************************

    @Override
    public void mouseDragged(final MouseEvent me) {
        // BugzID:660 - Implements columns dragging.
        if (draggable) {
            int newWidth = me.getX();

            if (newWidth >= this.getMinimumSize().width) {
                this.setWidth(newWidth);
            }
        } else if (moveable) {
            if (transparentPanel.getImage() == null) {
                Component c = me.getComponent();

                BufferedImage image = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics g = image.getGraphics();
                c.paint(g);

                transparentPanel.setVisible(true);

                Point destination = (Point) me.getPoint().clone();
                SwingUtilities.convertPointToScreen(destination, c);
                SwingUtilities.convertPointFromScreen(destination, transparentPanel);

                transparentPanel.setDestination(destination);
                transparentPanel.setImage(image);
                transparentPanel.setBackground(Color.BLACK);
                transparentPanel.repaint();
            }

            Component c = me.getComponent();

            Point destination = (Point) me.getPoint().clone();
            SwingUtilities.convertPointToScreen(destination, c);
            SwingUtilities.convertPointFromScreen(destination, transparentPanel);
            transparentPanel.setDestination(destination);

            transparentPanel.repaint();
        }
    }

    @Override
    public void mouseMoved(final MouseEvent me) {
        final int xCoord = me.getX();
        final int componentWidth = this.getSize().width;
        final int RESIZE_BOUNDARY_SIZE = Math.max(10, (int)(componentWidth * 0.10));
//        final int rangeStart = Math.round(componentWidth / 4F);
//        final int rangeEnd = Math.round(3F * componentWidth / 4F);

        // BugzID:660 - Implements columns dragging.
        if ((componentWidth - xCoord) < RESIZE_BOUNDARY_SIZE) {
            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            draggable = true;

            // BugzID:128 - Implements moveable columns
        } else if (!draggable) {
            moveable = true;
        } else {
            draggable = false;
            moveable = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    @Override
    public void clockPeriodicSync(double clockTime) {
        if(isSelected() && Datavyu.getVideoController().getCellHighlightAndFocus()) {
            focusNextCell();
        }
    }

    @Override
    public void clockForceSync(double clockTime) {

    }

    @Override
    public void clockRate(float rate) {

    }

    @Override
    public void clockStart(double clockTime) {

    }

    @Override
    public void clockStop(double clockTime) {

    }

    @Override
    public void clockBoundaryCheck(double clockTime) {

    }

    @Override
    public void clockSeekPlayback(double clockTime) {

    }
}
