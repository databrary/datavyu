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
package org.datavyu.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.Datavyu;
import org.datavyu.models.db.Cell;
import org.datavyu.models.db.DataStore;
import org.datavyu.models.db.Variable;
import org.datavyu.undoableedits.AddCellEdit;
import org.datavyu.undoableedits.ChangeCellEdit;
import org.datavyu.undoableedits.ChangeOffsetCellEdit;
import org.datavyu.util.ArrayDirection;
import org.datavyu.views.discrete.SpreadSheetPanel;

import javax.swing.undo.UndoableEdit;
import java.util.List;


/**
 * Controller for creating new cell.
 */
public final class CreateNewCellController {

    /** The logger instance for this class */
    private static Logger logger = LogManager.getLogger(CreateNewCellController.class);

    /** The view (the spreadsheet) for this controller */
    private SpreadSheetPanel view; // TODO: Check if we can remove 'view' from here?

    /** The model (the database) for this controller */
    private DataStore model;

    /**
     * Default constructor.
     */
    public CreateNewCellController() {
        // The spreadsheet is the view for this controller.
        view = (SpreadSheetPanel) Datavyu.getView().getComponent();
        model = Datavyu.getProjectController().getDataStore();
    }

    /**
     * Create New Cell Controller - creates new cells in columns adjacent to the
     * supplied cells. If no column is adjacent in the specified direction, no
     * cell will be created.
     *
     * @param sourceCells The list of source cells that we wish to create cells
     *                    adjacent too.
     * @param direction   The direction in which we wish to create adjacent cells.
     */
    public CreateNewCellController(final List<Cell> sourceCells,
                                   final ArrayDirection direction) {
        view = (SpreadSheetPanel) Datavyu.getView().getComponent();
        model = Datavyu.getProjectController().getDataStore();

        Cell newCell = null;

        logger.info("create adjacent cells:" + direction);

        // Get the column that is the parent of the source cell.
        for (Cell sourceCell : sourceCells) {

            Variable sourceColumn = model.getVariable(sourceCell);
            //long sourceColumn = sourceCell.getItsColID();
            //Vector<Long> columnOrder = modelAsLegacyDB().getColOrderVector();

            for (int i = 0; i < model.getVisibleVariables().size(); i++) {

                // Found the source column in the order column.
                if (model.getVisibleVariables().get(i).equals(sourceColumn)) {
                    i = i + direction.getModifier();

                    // Only create the cell if a valid column exists.
                    if ((i >= 0) && (i < model.getVisibleVariables().size())) {
                        Variable var = model.getVisibleVariables().get(i);
                        newCell = var.createCell();
                        newCell.setOnset(sourceCell.getOnset());
                        newCell.setOffset(sourceCell.getOffset());
                        Datavyu.getProjectController().setLastCreatedCell(newCell);

                        // Add the undoable action
                        UndoableEdit edit = new AddCellEdit(var.getName(), newCell);

                        // notify the listeners
                        Datavyu.getView().getUndoSupport().postEdit(edit);
                        break;
                    }

                    break;
                }
            }
        }

        model.deselectAll();
        newCell.setHighlighted(true);
    }

    /**
     * Constructor - creates new controller.
     *
     * @param milliseconds The milliseconds to use for the onset for the new cell.
     * @param setPrevOffset Determine whether or not to set previous offset to milliseconds-1
     */
    public CreateNewCellController(final long milliseconds, boolean setPrevOffset) {
        // The spreadsheet is the view for this controller.
        view = (SpreadSheetPanel) Datavyu.getView().getComponent();
        model = Datavyu.getProjectController().getDataStore();

        // BugzID:758 - Before creating a new cell and setting onset. We need
        // the last created cell and need to set the previous cells offset...
        // But only if it is not 0.
        Cell lastCreatedCell = Datavyu.getProjectController().getLastCreatedCell();

        //To set the previous offset -- NOTE THAT THIS IS NOT UNDOABLE
        if (setPrevOffset && lastCreatedCell != null) {
            // BugzID:1285 - Only update the last created cell if it is in
            // the same column as the newly created cell.
            for (Variable var : model.getSelectedVariables()) {
                if (var.contains(lastCreatedCell)) {
                    UndoableEdit edit = new ChangeOffsetCellEdit(lastCreatedCell, lastCreatedCell.getOffset(),
                        milliseconds - 1, ChangeCellEdit.Granularity.FINEGRAINED);
                    Datavyu.getView().getUndoSupport().postEdit(edit);
                    lastCreatedCell.setOffset(Math.max(0, (milliseconds - 1)));
                }
            }

            Variable lastCreated = Datavyu.getProjectController().getLastCreatedVariable();
            if (model.getSelectedVariables().isEmpty() && lastCreated != null) {
                if (lastCreated.contains(lastCreatedCell)) {
                    UndoableEdit edit = new ChangeOffsetCellEdit(lastCreatedCell, lastCreatedCell.getOffset(),
                        milliseconds - 1, ChangeCellEdit.Granularity.FINEGRAINED);
                    Datavyu.getView().getUndoSupport().postEdit(edit);
                    lastCreatedCell.setOffset(Math.max(0, (milliseconds - 1)));
                }
            }
        }

        //If there is no last created cell, use time to determine appopriate cell in
        //FIRST selected variable or the variable belonging to the FIRST selected cell
        if (setPrevOffset && lastCreatedCell == null){
              Variable v = null;
              if(!Datavyu.getProjectController().getDataStore().getSelectedVariables().isEmpty()){
                  Datavyu.getProjectController().getDataStore().getSelectedVariables().get(0);
              }
              else{
                  if(!Datavyu.getProjectController().getDataStore().getSelectedCells().isEmpty()){
                          Cell selectedC = Datavyu.getProjectController().getDataStore().getSelectedCells().get(0);
                          for(Variable v1 : Datavyu.getProjectController().getDataStore().getVisibleVariables()){
                                  if(v1.contains(selectedC)){
                                      v = v1;
                                      break;
                                  }
                          }
                  }
              }

              if(v != null){
                    Cell oneBefore = null;
                    for(Cell c : v.getCellsTemporally()){
                          if(c.getOnset() > milliseconds){
                              break;
                          }
                          oneBefore = c;
                    }
                    if (oneBefore != null){
                        UndoableEdit edit = new ChangeOffsetCellEdit(oneBefore, oneBefore.getOffset(),
                            milliseconds - 1, ChangeCellEdit.Granularity.FINEGRAINED);
                        Datavyu.getView().getUndoSupport().postEdit(edit);
                        oneBefore.setOffset(Math.max(0, (milliseconds - 1)));
                    }
              }
        }

        // Create the new cell.
        createNewCell(milliseconds);
    }

    /**
     * Inserts a cell into the end of the supplied variable.
     *
     * @param v The variable that we want to add a cell too.
     * @return The cell that was just inserted.
     */
    public Cell createCell(final Variable v) {
        logger.info("create cell in selected column");

        // perform the operation
        List<Cell> cells = v.getCellsTemporally();

        long newOnset = 0;
        newOnset = Datavyu.getVideoController().getCurrentTime();

        Cell newCell = v.createCell();
        newCell.setOnset(newOnset);
        Datavyu.getProjectController().setLastCreatedCell(newCell);
        Datavyu.getProjectController().setLastCreatedVariable(v);

        return newCell;
    }

    /**
     * Create a default cell at the end of the nominated variable.
     *
     * @param v The variable we are adding a cell to the end of.
     */
    public void createDefaultCell(final Variable v) {
        model.deselectAll();
        Cell c = createCell(v);

        // record the effect
        UndoableEdit edit = new AddCellEdit(v.getName(), c);

        // Display any changes.
        Datavyu.getView().getComponent().revalidate();
        // notify the listeners
        Datavyu.getView().getUndoSupport().postEdit(edit);
    }

    /**
     * Create a default cell
     *
     * @param preferFirstSelected prefer the first selected variable
     */
    public void createDefaultCell(boolean preferFirstSelected) {
        Cell newCell = null;
        Variable v = Datavyu.getProjectController().getLastCreatedVariable();
        if (preferFirstSelected) {
            List<Variable> vlist = Datavyu.getProjectController().getDataStore().getSelectedVariables();
            if (!vlist.isEmpty()) {
                v = vlist.get(0);
            }
        }

        if (v != null) {
            newCell = createCell(v);

            // record the effect
            UndoableEdit edit = new AddCellEdit(v.getName(), newCell);
            Datavyu.getView().getComponent().revalidate();
            Datavyu.getView().getUndoSupport().postEdit(edit);
        }

        if (newCell != null) {
            model.deselectAll();
            newCell.setHighlighted(true);
        }
    }

    /**
     * Create a default cell
     */
    public void createDefaultCell() {
        createDefaultCell(false);
    }

    /**
     * Create a new cell with given onset. Currently just appends to the
     * selected column or the column that last had a cell added to it.
     *
     * @param milliseconds The number of milliseconds since the origin of the
     *                     spreadsheet to create a new cell from.
     */
    public void createNewCell(final long milliseconds) {

        /*
         * Concept of operation: Creating a new cell.
         *
         * Situation 1: Spreadsheet has one or more selected columns For each
         * selected column do Create a new cell with the supplied onset and
         * insert into db.
         *
         * Situation 2: Spreadsheet has one or more selected cells For each
         * selected cell do Create a new cell with the selected cell onset and
         * offset and insert into the db.
         *
         * Situation 3: User has set focus on a particular cell in the
         * spreadsheet - the caret is or has been in one of the editable parts
         * of a spreadsheet cell. First check this request has not come from the
         * video controller. For the focussed cell do Create a new cell with the
         * focussed cell onset and offset and insert into the db.
         *
         * Situation 4: Request has come from the video controller and there is
         * no currently selected column. Create a new cell in the same column as
         * the last created cell or the last focussed cell.
         */
        long onset = milliseconds;

        // If not coming from video controller (milliseconds < 0) allow
        // multiple adds
        boolean multiadd = (milliseconds < 0);

        if (milliseconds < 0) {
            onset = 0;
        }

        Cell newCell = null;
        boolean newcelladded = false;

        // check for Situation 1: one or more selected columns
        model = Datavyu.getProjectController().getDataStore();

        for (Variable var : model.getSelectedVariables()) {
            logger.info("create cell in selected column");
            newCell = var.createCell();
            newCell.setOnset(onset);
            Datavyu.getProjectController().setLastCreatedCell(newCell);
            Datavyu.getProjectController().setLastCreatedVariable(var);

            // Add the undoable action
            UndoableEdit edit = new AddCellEdit(var.getName(), newCell);
            Datavyu.getView().getUndoSupport().postEdit(edit);

            newcelladded = true;

            if (!multiadd) {
                break;
            }
        }

        if (!newcelladded) {
            for (Cell cell : model.getSelectedCells()) {
                logger.info("create cell below selected cell");

                // reget the selected cell from the database using its identifier
                // in case a previous insert has changed its ordinal.
                // recasting to DataCell without checking as the iterator
                // only returns DataCells (no ref cells allowed so far)
                Variable var = model.getVariable(cell);
                newCell = var.createCell();
                newCell.setOnset(onset);
                Datavyu.getProjectController().setLastCreatedCell(newCell);
                Datavyu.getProjectController().setLastCreatedVariable(var);

                // Add the undoable action
                UndoableEdit edit = new AddCellEdit(var.getName(), newCell);
                Datavyu.getView().getUndoSupport().postEdit(edit);

                newcelladded = true;

                if (!multiadd) {
                    break;
                }
            }
        }

        // else check for Situation 3: User is or was editing an existing cell
        // and has requested a new cell
        if (!newcelladded && multiadd) {
            if (Datavyu.getProjectController().getLastSelectedCell() != null) {
                logger.info("create cell while editing existing cell");
                Variable var = model.getVariable(Datavyu.getProjectController().getLastCreatedCell());
                if (var != null) {
                    newCell = var.createCell();
                    newCell.setOnset(onset);
                    Datavyu.getProjectController().setLastCreatedCell(newCell);
                    Datavyu.getProjectController().setLastCreatedVariable(var);

                    // Add the undoable action
                    UndoableEdit edit = new AddCellEdit(var.getName(), newCell);
                    Datavyu.getView().getUndoSupport().postEdit(edit);

                    newcelladded = true;
                }
            }
        }

        // else go with Situation 4: Video controller requested - create in the
        // same column as the last created cell or the last focused cell.
        if (!newcelladded) {
            logger.info("create cell in same location as last created cell");

            // BugzID:779 - Check for presence of columns, else return
            if (model.getAllVariables().isEmpty()) {
                return;
            }

            if (Datavyu.getProjectController().getLastCreatedVariable() == null) {
                Datavyu.getProjectController().setLastCreatedVariable(model.getAllVariables().get(0));
            }

            newCell = Datavyu.getProjectController().getLastCreatedVariable().createCell();
            newCell.setOnset(onset);
            Datavyu.getProjectController().setLastCreatedCell(newCell);

            // Add the undoable action
            UndoableEdit edit = new AddCellEdit(Datavyu.getProjectController().getLastCreatedVariable().getName(), newCell);
            Datavyu.getView().getUndoSupport().postEdit(edit);
        }

        model.deselectAll();
        newCell.setHighlighted(true);
    }
}
