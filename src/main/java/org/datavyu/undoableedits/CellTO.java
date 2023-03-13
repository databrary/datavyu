/*
 * Copyright (c) 2011 Datavyu Foundation, http://datavyu.org
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
package org.datavyu.undoableedits;

import org.datavyu.models.db.Argument;
import org.datavyu.models.db.Cell;
import org.datavyu.models.db.Variable;

/**
 * Cell Transfer Object for holding the changes that need to be transferred from
 * undo / redo states to the datastore.
 */
public final class CellTO {
    private Cell cell;
    private Variable variable;

    private long onset;

    private long offset;

    private String value;
    
    private String variableName;

    /**
     * Constructor.
     * 
     * @param newCell The cell that you using to transfer around undo / redo
     * states.
     */
    public CellTO(final Cell newCell, final Variable parentVariable) {
        variableName = parentVariable.getName();
        onset = newCell.getOnset();
        offset = newCell.getOffset();
        value = newCell.getValueAsString();
        if(newCell.getCellValue().getArgument().type == Argument.Type.MATRIX) {
            value = value.substring(1, value.length() - 1);
        }

        cell = newCell;
        variable = parentVariable;
        //Remove ()
//        if (newCell.getCellValue().isEmpty()) {
//            value = "<" + variableName + ">";
//        }
    }

    /**
     * @return The onset this object is transferring.
     */
    public long getOnset() {
        return onset;
    }

    /**
     * @return The offset this object is transferring.
     */
    public long getOffset() {
        return offset;
    }

    /**
     * @return The value this object is transferring.
     */
    public String getValue() {
        return value;
    }

    /**
     * @return The parent variable name for this transfer object.
     */
    public String getParentVariableName() {
        return variableName;
    }

    public Cell getCell() {
        return cell;
    }

    public Variable getVariable() {
        return variable;
    }
}
