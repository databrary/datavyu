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
import org.datavyu.undoableedits.ChangeCellEdit;
import org.datavyu.undoableedits.ChangeOffsetCellEdit;

import javax.swing.undo.UndoableEdit;
import java.util.List;

/**
 * Controller for setting all selected cells to have the specified stop time / offset.
 */
public class SetSelectedCellStopTimeController {

    /** The logger instance for this class */
    private static Logger logger = LogManager.getLogger(SetSelectedCellStopTimeController.class);

    /**
     * Sets all selected cells to have the specified stop time / offset.
     *
     * @param milliseconds The time in milliseconds to use for all selected cells offset / stop time.
     */
    public SetSelectedCellStopTimeController(final long milliseconds) {
        logger.info("Set selected cell offset: " + milliseconds);

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
            UndoableEdit edit = new ChangeOffsetCellEdit(cell, cell.getOffset(), milliseconds,
                    ChangeCellEdit.Granularity.FINEGRAINED);
            Datavyu.getView().getUndoSupport().postEdit(edit);
            cell.setOffset(milliseconds);
        }
    }
}
