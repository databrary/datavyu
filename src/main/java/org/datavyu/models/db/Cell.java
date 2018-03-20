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
package org.datavyu.models.db;

/**
 * Represents a cell in the OpenSHAPA spreadsheet.
 */
public interface Cell {

    /**
     * @return the offset timestamp in a HH:mm:ss:SSS format, where HH is 24 hour mm is minutes in an hour, ss is
     * seconds in a minute and SSS is milliseconds in a second.
     */
    String getOffsetString();

    /**
     * @return The offset timestamp in milliseconds. Returns -1 if the offset cannot be resolved.
     */
    long getOffset();

    /**
     * Sets the offset for this cell.
     *
     * @param newOffset The new offset timestamp in milliseconds to use for this cell.
     */
    void setOffset(final long newOffset);

    /**
     * Sets the offset for this cell.
     *
     * @param newOffset The new onset timestamp for this cell in string in the
     *                  format "HH:MM:SS:mmm" where HH = hours, MM = minutes, SS = seconds and
     *                  mmm = milliseconds.
     */
    void setOffset(final String newOffset);

    /**
     * @return The onset timestamp in milliseconds. Returns -1 if the onset cannot be resolved
     */
    Cell getFreshCell();

    /**
     *
     * @return
     */
    long getOnset();

    /**
     * Sets the onset for this cell.
     *
     * @param newOnset The new onset timestamp for this cell in string in the
     *                 format "HH:MM:SS:mmm" where HH = hours, MM = minutes, SS = seconds and
     *                 mmm = milliseconds.
     */
    void setOnset(final String newOnset);

    /**
     * Sets the onset for this cell.
     *
     * @param newOnset The new onset timestamp in milliseconds to use for this
     *                 cell.
     */
    void setOnset(final long newOnset);

    /**
     * @return the onset timestamp in a HH:mm:ss:SSS format, where HH is 24 hour
     * mm is minutes in an hour, ss is seconds in a minute and SSS is
     * milliseconds in a second.
     */
    String getOnsetString();

    /**
     * @return The value stored in the cell as a string. Returns null if the
     * string value cannot be resolved.
     */
    String getValueAsString();

    /**
     *
     * @return The value of the cell.
     */
    CellValue getCellValue();

    /**
     *
     * @return
     */
    Variable getVariable();

    /**
     * @return The unique identifier of the cell
     */
    String getCellId();

    /**
     * @return True if the cell is selected, false otherwise
     */
    boolean isSelected();

    /**
     * Select this cell
     *
     * @param selected True if this cell is selected, false if unselected
     */
    void setSelected(final boolean selected);

    /**
     * @return True if the cell is highlighted, false otherwise.
     */
    boolean isHighlighted();

    /**
     * Highlights the cell.
     *
     * @param highlighted True if this cell is highlighted, false otherwise.
     */
    void setHighlighted(final boolean highlighted);

    /**
     * Adds a new argument to a matrix variable.
     *
     * @param index index - the index of the argument in childArguments to change
     * @param value - The value to set the argument to
     */
    void setMatrixValue(final int index, final String value);

    /**
     *
     * @param index
     * @return
     */
    CellValue getMatrixValue(final int index);

    /**
     * Removes an argument from a matrix variable.
     *
     * @param index - the index of argument to clear from the matrix
     */
    void clearMatrixValue(final int index);

    /**
     * Adds a new argument to a matrix variable.
     *
     * @param type - the type of argument to add to the matrix
     */
    void addMatrixValue(final Argument type);

    /**
     * Moves an argument from one index to another in a matrix.
     *
     * @param oldIndex - the index in childArguments of argument to move
     * @param oldIndex - the index in childArguments of where to move to
     */
    void moveMatrixValue(final int oldIndex, final int newIndex);

    /**
     * Removes an argument from a matrix variable.
     *
     * @param index - the index in childArguments of argument to remove from the matrix
     */
    void removeMatrixValue(final int index);

    /**
     * Adds a cellListener that needs to be notified when the cell changes.
     */
    void addListener(final CellListener cellListener);

    /**
     * Removes a cellListener from the list of things that need to be notified when
     * the cell changes.
     */
    void removeListener(final CellListener cellListener);

    /**
     *
     * @param time
     * @return
     */
    boolean isInTimeWindow(long time);

    /**
     *
     * @param time
     * @return
     */
    boolean isPastTimeWindow(long time);
}
