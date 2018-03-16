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
import org.datavyu.models.db.Cell;
import org.datavyu.models.db.DataStore;
import org.datavyu.models.db.Variable;
import org.datavyu.util.Constants;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/**
 * ColumnDataPanel panel that contains the SpreadsheetCell panels.
 */
public final class ColumnDataPanel extends JPanel implements KeyEventDispatcher {
    /**
     * The logger for this class.
     */
    private static final Logger logger = LogManager.getLogger(ColumnDataPanel.class);
    /**
     * Width of the column.
     */
    private int columnWidth;
    /**
     * Height of the column.
     */
    private int columnHeight;
    /**
     * The model that this variable represents.
     */
    private Variable model;
    /**
     * The cell selection listener used for cells in this column.
     */
    private CellSelectionListener cellSelectionL;
    /**
     * Collection of the SpreadsheetCells held in by this data panel.
     */
    private List<SpreadsheetCell> cells;
    /**
     * The mapping between the database and the spreadsheet cells.
     */
    private Map<Cell, SpreadsheetCell> viewMap;
    /**
     * button for creating a new empty cell.
     */
    private SpreadsheetEmptyCell newCellButton;

    /**
     * Padding for the bottom of the column.
     */
    private JPanel padding;

    /**
     * Creates a new ColumnDataPanel.
     *
     * @param db       The datastore that this column data panel reflects.
     * @param width    The width of the new column data panel in pixels.
     * @param variable The Data Column that this panel represents.
     * @param cellSelL Spreadsheet cell selection listener.
     */
    public ColumnDataPanel(final DataStore db,
                           final int width,
                           final Variable variable,
                           final CellSelectionListener cellSelL) {
        super();

        // Store member variables.
        columnWidth = width;
        columnHeight = 0;
        cells = new ArrayList<>();
        viewMap = new HashMap<>();
        cellSelectionL = cellSelL;
        model = variable;

        setLayout(null);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, Constants.BORDER_SIZE,
                new Color(175, 175, 175)));

        newCellButton = new SpreadsheetEmptyCell(variable);
        this.add(newCellButton);

        padding = new JPanel();
        padding.setBackground(new Color(237, 237, 237));
        padding.setBorder(BorderFactory.createMatteBorder(0, 0, 0, Constants.BORDER_SIZE,
                new Color(175, 175, 175)));
        this.add(padding);

        // Populate the data column with spreadsheet cells.
        buildDataPanelCells(db, variable, cellSelL);
    }

    /**
     * Registers this column data panel with everything that needs to notify
     * this class of events.
     */
    public void registerListeners() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    }

    /**
     * Deregisters this column data panel with everything that is currently
     * notifying this class of events.
     */
    public void deregisterListeners() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
    }

    /**
     * Build the SpreadsheetCells and add to the DataPanel.
     *
     * @param db       The datastore holding cells that this column will represent.
     * @param variable The variable to display.
     * @param cellSelL Spreadsheet listener to notify about cell selection
     *                 changes.
     */
    private void buildDataPanelCells(final DataStore db, final Variable variable,
                                     final CellSelectionListener cellSelL) {

        // traverse and build the cells
        for (Cell cell : variable.getCellsTemporally()) {
            SpreadsheetCell sc = new SpreadsheetCell(db, cell, cellSelL);
            cell.addListener(sc);

            // add cell to the JPanel
            this.add(sc);

            // and add it to our reference list
            cells.add(sc);

            // Add the Identifier's to the mapping.
            viewMap.put(cell, sc);
            columnHeight += sc.getHeight();
        }

        this.add(newCellButton);
        this.setSize(columnWidth, columnHeight);
    }

    /**
     * Clears the cells stored in the column data panel.
     */
    public void clear() {
        for (SpreadsheetCell cell : cells) {
            cell.getCell().removeListener(cell);
            this.remove(cell);
        }

        cells.clear();
        viewMap.clear();
    }

    /**
     * Find and delete SpreadsheetCell by its Identifier.
     *
     * @param cell The cell to find and delete from the column data panel.
     */
    public void deleteCell(final Cell cell) {
        SpreadsheetCell sCell = viewMap.get(cell);
        cell.removeListener(sCell);
        this.remove(sCell);
        cells.remove(sCell);
        viewMap.remove(cell);
    }

    /**
     * Insert a new SpreadsheetCell for a given cell.
     *
     * @param ds       The database holding the cell that is being inserted into this
     *                 column data panel.
     * @param cell     The cell to create and insert into this column data panel.
     * @param cellSelL SpreadsheetCellSelectionListener to notify of changes in
     *                 selection.
     */
    public void insertCell(final DataStore ds, final Cell cell, final CellSelectionListener cellSelL) {

        SpreadsheetCell nCell = new SpreadsheetCell(ds, cell, cellSelL);
        nCell.setWidth(this.getWidth());
        cell.addListener(nCell);
//        cellSelectionL.clearColumnSelection();

        nCell.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.add(nCell);
        this.cells.add(nCell);
        viewMap.put(cell, nCell);
        nCell.requestFocus();
    }

    /**
     * Set the width of the SpreadsheetCell.
     *
     * @param width New width of the SpreadsheetCell.
     */
    public void setWidth(final int width) {
        columnWidth = width;
    }

    public void setHeight(final int height) {
        columnHeight = height;
        setMaximumSize(new Dimension(columnWidth, columnHeight * 2));
    }

    /**
     * Override Preferred size to fix the width.
     *
     * @return the preferred size of the data column.
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(columnWidth, columnHeight);
    }

    public SpreadsheetEmptyCell getNewCellButton() {
        return this.newCellButton;
    }

    public JPanel getPadding() {
        return this.padding;
    }

    public SpreadsheetCell getCellTemporally(final int index) {
        return viewMap.get(model.getCellTemporally(index));
    }

    /**
     * @return The SpreadsheetCells in this column temporally.
     */
    public List<SpreadsheetCell> getCellsTemporally() {
        ArrayList<SpreadsheetCell> result = new ArrayList<>();

        int ord = 1;
        for (Cell c : model.getCellsTemporally()) {
            SpreadsheetCell sc = viewMap.get(c);
            if (sc != null) {
                sc.setOrdinal(ord++);
                result.add(sc);
            }
        }
        return result;
    }

    /**
     * @return The number of cells stored in this column.
     */
    public int getNumCells() {
        return cells.size();
    }

    /**
     * @return The SpreadsheetCells in this column.
     */
    public List<SpreadsheetCell> getCells() {
        return cells;
    }

    /**
     * @return The selected spreadsheet cells in this column.
     */
    public AbstractList<SpreadsheetCell> getSelectedCells() {
        AbstractList<SpreadsheetCell> selectedCells = new ArrayList<SpreadsheetCell>();

        for (SpreadsheetCell c : selectedCells) {
            if (c.getCell().isSelected()) {
                selectedCells.add(c);
            }
        }

        return selectedCells;
    }

    public SpreadsheetCell getSelectedCell(){
        List<SpreadsheetCell> cells = getCellsTemporally();
        SpreadsheetCell selectedCell = null;
        for (SpreadsheetCell cell : cells){
            if(cell.getCell().isSelected()){
                selectedCell = cell;
                break;
            }
        }
        return selectedCell;
    }

    /**
     * Dispatches the key event to the desired components.
     *
     * @param e The key event to dispatch.
     * @return true if the event has been consumed by this dispatch, false
     * otherwise
     */
    @Override
    public boolean dispatchKeyEvent(final KeyEvent e) {
        if ((e.getID() == KeyEvent.KEY_PRESSED) && ((e.getKeyCode() == KeyEvent.VK_UP)
                || (e.getKeyCode() == KeyEvent.VK_DOWN))) {
            SpreadsheetCell selectedCell = getSelectedCell();
            if (getSelectedCell() != null) {

                int cellId = cells.indexOf(selectedCell);

                if (e.getKeyCode() == KeyEvent.VK_UP) {

                    if (0 <= cellId - 1 && cellId - 1 < cells.size()) {

                        selectedCell.getCell().setHighlighted(false);
                        selectedCell.getCell().setSelected(false);
                        requestFocus();

                        SpreadsheetCell cellUP = cells.get(cellId - 1);

                        cellUP.getCell().setHighlighted(true);
                        cellUP.requestFocus();
                        cellUP.getCell().setSelected(true);

                        e.consume();

                        return true;

                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {

                    if (0 <= cellId + 1 && cellId + 1 < cells.size()) {

                        selectedCell.getCell().setHighlighted(false);
                        selectedCell.getCell().setSelected(false);
                        requestFocus();

                        SpreadsheetCell cellDOWN = cells.get(cellId + 1);

                        cellDOWN.getCell().setHighlighted(true);
                        cellDOWN.requestFocus();
                        cellDOWN.getCell().setSelected(true);

                        e.consume();

                        return true;
                    }
                }
            }
        }
        return false;
    }
}
