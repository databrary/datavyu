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
import org.datavyu.undoableedits.ChangeCellEdit.Granularity;
import org.datavyu.undoableedits.ChangeOnsetCellEdit;

import javax.swing.undo.UndoableEdit;
import java.util.List;

/**
 * Controller for setting all selected cells to have the specified start time / onset.
 */
public class SetSelectedCellStartTimeController {

    /** The logger instance for this class */
    private static Logger logger = LogManager.getLogger(SetSelectedCellStartTimeController.class);

    /**
     * Sets all selected cells to have the specified start time / onset.
     *
     * @param milliseconds The time in milliseconds to use for all selected cells onset / start time.
     */
    public SetSelectedCellStartTimeController(final long milliseconds) {
        logger.info("Set selected cell onset to: " + milliseconds);

        // Get the dataStore that we are manipulating.
        DataStore dataStore = Datavyu.getProjectController().getDataStore();
        List<Cell> selectedCells;

        if(dataStore.getSelectedCells().size() == 0){
          selectedCells = Datavyu.getProjectController().getLastSelectedCells();
        }else{
          selectedCells = dataStore.getSelectedCells();
        }

        for (Cell cell : selectedCells) {
            // record the effect
            UndoableEdit edit = new ChangeOnsetCellEdit(cell, cell.getOnset(), milliseconds, Granularity.FINEGRAINED);
            Datavyu.getView().getUndoSupport().postEdit(edit);
            cell.setOnset(milliseconds);
        }
    }
}
