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
package org.datavyu.views.discrete.datavalues.vocabelements;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.models.db.Argument;
import org.datavyu.models.db.Variable;
import org.datavyu.views.discrete.EditorComponent;

import javax.swing.text.JTextComponent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

/**
 * This class is the character editor of a NominalDataValue.
 */
public final class FormalArgEditor extends EditorComponent {

    /**
     * String holding the reserved characters.
     */
    private static final String RESERVED_CHARS = ")(<>|,;\t\r\n";
    /**
     * The logger for this class.
     */
    private static Logger LOGGER = LogManager.getLogger(FormalArgEditor.class);
    /**
     * Parent Vocab Variable.
     */
    private Variable parentVariable;
    /**
     * Index of the formal arg.
     */
    private int argIndex;
    /**
     * The parent editor window that this argument belongs too.
     */
    private VocabElementV parentView;

    private boolean defaultVal;

    /**
     * @param ta    The JTextComponent that this virtual editor floats on top.
     * @param var    The parent that this argument belongs too.
     * @param index The index of the argument within the parent vocabelement
     *              that this Editor will represent.
     * @param pv    The parent vocab element view that this editor belongs too.
     */
    public FormalArgEditor(final JTextComponent ta,
                           final Variable var,
                           final int index,
                           final VocabElementV pv) {
        super(ta);
        setEditable(true);
        argIndex = index;
        parentView = pv;
        parentVariable = var;
        defaultVal = true;
        resetValue();
    }

    /**
     * Resets the text value of this editor.
     */
    public void resetValue() {
        Argument model = parentVariable.getRootNode().childArguments.get(argIndex);
        String argName = "";
        if (model != null) {
            argName = model.name;
        }
        setText(argName);
    }

    /**
     * @return the model.
     */
    public Argument getModel() {
        return parentVariable.getRootNode().childArguments.get(argIndex);
    }

    /**
     * @return the argument index.
     */
    public int getArgPos() {
        return argIndex;
    }

    public void updateArgName(String newValue) {
        Argument current_arg = parentVariable.getRootNode();
        current_arg.childArguments.get(argIndex).name = newValue;
        parentVariable.setRootNode(current_arg);
        defaultVal = false;
    }

    /**
     * Action to invoke when focus is gained.
     *
     * @param e The FocusEvent that triggered this action.
     */
    @Override
    public void focusGained(final FocusEvent e) {
        this.parentView.getParentDialog().updateDialogState();
    }

    /**
     * The action to invoke when a key is typed.
     *
     * @param e The KeyEvent that triggered this action.
     */
    @Override
    public void keyTyped(KeyEvent e) {
        if (!this.isReserved(e.getKeyChar()) && !e.isControlDown()) {

            removeSelectedText();
            StringBuilder currentValue = new StringBuilder(getText());
            currentValue.insert(getCaretPosition(), e.getKeyChar());

            updateArgName(currentValue.toString());

            // Advance caret over the top of the new char.
            int pos = this.getCaretPosition() + 1;
            this.setText(currentValue.toString());
            this.setCaretPosition(pos);

            parentView.getParentDialog().updateDialogState();
        }

        e.consume();
    }

    /**
     * @param aChar Character to test
     * @return true if the character is a reserved character.
     */
    public boolean isReserved(final char aChar) {
        return (RESERVED_CHARS.indexOf(aChar) >= 0);
    }

    /**
     * Action to take by this editor when a key is pressed.
     *
     * @param e The KeyEvent that triggered this action.
     */
    @Override
    public void keyPressed(final KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_BACK_SPACE:
                removeBehindCaret();

                updateArgName(getText());
                //vocabElement.replaceFormalArg(model, argIndex);

                parentView.getParentDialog().updateDialogState();

                e.consume();
                break;
            case KeyEvent.VK_DELETE:
                removeAheadOfCaret();

                updateArgName(getText());
                //vocabElement.replaceFormalArg(model, argIndex);

                parentView.getParentDialog().updateDialogState();
                e.consume();
                break;
            default:
                break;
        }
    }

    /**
     * Action to take by this editor when a key is released.
     *
     * @param e The KeyEvent that triggered this action.
     */
    @Override
    public void keyReleased(final KeyEvent e) {
    }

    @Override
    public boolean canSubSelect() {
        return !defaultVal;
    }
}
