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
package org.datavyu.views.discrete.datavalues;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.Datavyu;
import org.datavyu.models.db.Cell;
import org.datavyu.models.db.CellValue;
import org.datavyu.models.db.Variable;
import org.datavyu.undoableedits.ChangeCellEdit;
import org.datavyu.undoableedits.ChangeValCellEdit;
import org.datavyu.views.discrete.EditorComponent;
import org.datavyu.views.discrete.EditorTracker;

import javax.swing.*;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JTextArea view of the Matrix (database cell) data.
 */
public final class MatrixRootView extends JTextArea implements FocusListener {

    /** Logger for this class */
    private static Logger logger = LogManager.getLogger(MatrixRootView.class);

    /**
     * The parent cell for this JPanel.
     */
    private Cell parentCell = null;

    private String oldValue = "";

    /**
     * All the editors that make up the representation of the data.
     */
    private List<EditorComponent> allEditors;

    /**
     * The editor tracker responsible for the editor components.
     */
    private EditorTracker edTracker;

    /**
     * Creates a new instance of MatrixV.
     *
     * @param cell  The parent cell for this spread sheet cell.
     * @param cellValue The Matrix holding data values that this view label will represent.
     */
    public MatrixRootView(final Cell cell, final CellValue cellValue) {
        super();

        setLineWrap(true);
        setWrapStyleWord(true);

        parentCell = cell;
        allEditors = new ArrayList<>();
        edTracker = new EditorTracker(this, allEditors);

        setMatrix(cellValue);

        addFocusListener(this);
        addFocusListener(edTracker);

        // Funny behavior for the return key
        addKeyListener(new KeyListener() {
                   public void keyPressed(KeyEvent e) {
                       if ((Character.isLetter(e.getKeyChar()) || Character.isDigit(e.getKeyChar())
                               || Character.isSpaceChar(e.getKeyChar())) && !e.isAltDown() && !e.isControlDown() && !e.isMetaDown()) {
                           UndoableEdit edit = new ChangeValCellEdit(parentCell, oldValue, ChangeCellEdit.Granularity.FINEGRAINED);
                           Datavyu.getView().getUndoSupport().postEdit(edit);
                       }
                       if (e.getKeyCode() == KeyEvent.VK_ENTER) { //&& getSelectedText() != null) {
                           e.consume();
                       }
                    }
                   public void keyTyped(KeyEvent e) {}
                   public void keyReleased(KeyEvent e)
                   {
                        if (e.getKeyCode() == KeyEvent.VK_ENTER)
                        {
                            // Verbose debug output
                            if (!generateText().equals(getText())) {
                                logger.info("MatrixRootview rebuilt due to funny return key");
                                logger.info("\tGenerated contents" + generateText().length() + ": " + generateText()
                                        + "\n\tTextbox contents " + getText().length() + " : " + getText());
                                rebuildText();
                            }
                        }
                   }
                   
                });
        //regular editor tracker
        addKeyListener(edTracker);
        addMouseListener(edTracker);
    }

    /**
     * @return true if a viewport should force the Scrollables width to match
     * its own.
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    /**
     * Sets the value that this MatrixRootView will represent.
     *
     * @param v The CellValue to display.
     */
    public void setMatrix(final CellValue v) {
        // Determine selected editor, and internal caret position.
        int pos = getCaretPosition();
        EditorComponent comp = edTracker.findEditor(pos);
        int edPos = comp.getCaretPosition();

        if (v != null) {
            // No editors exist yet - build some to begin with.
            allEditors.clear();
            allEditors.addAll(DataValueEditorFactory.buildMatrix(this, parentCell));
        }

        rebuildText();

        // restore caret position inside current editor.
        comp.setCaretPosition(edPos);
    }

    /**
     * Recalculates and sets the text to display.
     */
    public void rebuildText() {
        setText(generateText());
    }
    
    public String generateText() {
        String ans = "";
        for (EditorComponent item : allEditors) {
            ans += item.getText();
        }
        ans = ans.replaceAll("\\\\[\\\\]*\\\\", "\\\\");
        return ans;
    }

    /**
     * Used in the UISpec4j tests.
     *
     * @return The editor tracker for this MatrixRootView.
     */
    public EditorTracker getEdTracker() {
        return edTracker;
    }

    // *************************************************************************
    // Focus Listener Overrides
    // *************************************************************************
    @Override
    public void focusGained(final FocusEvent fe) {
        // We need to remember which cell should be duplicated if the user
        // presses the enter key or selects New Cell from the menu.
        if (parentCell != null) {
            oldValue = parentCell.getValueAsString();
            // method names don't reflect usage - we didn't really create
            // this column just now.
            Variable v = Datavyu.getProjectController().getDataStore().getVariable(parentCell);
            Datavyu.getProjectController().setLastCreatedVariable(v);
            Datavyu.getProjectController().setLastSelectedCell(parentCell);
        }
        parentCell.setSelected(true);
    }

    @Override
    public void focusLost(final FocusEvent fe) {
        // do nothing
        parentCell.setSelected(false);
        if(!parentCell.getValueAsString().equals(oldValue)) {
            UndoableEdit edit = new ChangeValCellEdit(parentCell, oldValue, ChangeCellEdit.Granularity.COARSEGRAINED);
            Datavyu.getView().getUndoSupport().postEdit(edit);
        }
    }

    // *************************************************************************
    // Parent Class Overrides
    // *************************************************************************
    @Override
    public void paste() {
        edTracker.paste();
    }

    @Override
    public void cut() {
        edTracker.cut();
    }
}