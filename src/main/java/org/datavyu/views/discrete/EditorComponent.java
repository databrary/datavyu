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
import org.datavyu.util.ConfigProperties;

import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

/**
 * EditorComponent - Abstract class for editing a segment of text within a
 * JTextComponent. Subclasses of this abstract class are combined and used by an
 * EditorTracker to manage editing of the JTextComponent.
 */
public abstract class EditorComponent implements ClipboardOwner {

    /**
     * The logger for this class.
     */
    private static Logger LOGGER = LogManager.getLogger(EditorComponent.class);
    /**
     * JTextComponent containing this EditorComponent.
     */
    private JTextComponent parentComp;
    /**
     * Character position in the JTextComponent where this editor begins.
     */
    private int startPos;
    /**
     * Local copy of this editor's text.
     */
    private String editorText;
    /**
     * Is the editorComponent editable?  Used by EditorTracker.
     */
    private boolean editable;
    /**
     * Does the editorComponent allow Return characters to be input?
     */
    private boolean acceptsReturnKey;
    /**
     * A list of characters that can not be removed from this view.
     */
    private String preservedChars;
    /**
     * Are we deleting characters, or replacing them with a substitute?
     */
    private boolean isDeletingChar;
    /**
     * The character to use as a substitute if we are doing replacement.
     */
    private char replaceChar;

    /**
     * Default Constructor.
     */
    public EditorComponent() {
        startPos = 0;
        editorText = "";
        editable = false;
        parentComp = null;
        preservedChars = "";
        isDeletingChar = true;
    }

    /**
     * Constructor.
     *
     * @param tc JTextComponent this editor works with.
     */
    public EditorComponent(final JTextComponent tc) {
        this();
        parentComp = tc;
        parentComp.setBackground(ConfigProperties.getInstance().getSpreadSheetBackgroundColor());
    }

    /**
     * Constructor.
     *
     * @param tc   JTextComponent this editor works with.
     * @param text text to initialise the editor to.
     */
    public EditorComponent(final JTextComponent tc, final String text) {
        this(tc);
        editorText = text;
    }

    /**
     * Action to invoke when a key is pressed.
     *
     * @param e The KeyEvent that triggered this action.
     */
    public abstract void keyPressed(final KeyEvent e);

    /**
     * Action to invoke when a key is typed.
     *
     * @param e The KeyEvent that triggered this action.
     */
    public abstract void keyTyped(final KeyEvent e);

    /**
     * Action to invoke when a key is released.
     *
     * @param e The KeyEvent that triggered this action.
     */
    public abstract void keyReleased(final KeyEvent e);

    /**
     * Action to invoke when focus is gained.
     *
     * @param fe The FocusEvent that triggered this action.
     */
    public abstract void focusGained(final FocusEvent fe);

    /**
     * Action to invoke when focus is lost.
     *
     * @param fe The FocusEvent that triggered this action.
     */
    public void focusLost(final FocusEvent fe) {
        if (parentComp != null) {
            parentComp.select(0, 0);
        }
    }

    /**
     * Determine if the editor tracker permits sub selections within the
     * component.
     *
     * @return True if permitted to perform sub selection, false otherwise.
     */
    public boolean canSubSelect() {
        return true;
    }

    /**
     * @return is the editorcomponent "editable".
     */
    public final boolean isEditable() {
        return editable;
    }

    /**
     * @param canEdit set true if the editorcomponent is editable.
     */
    public final void setEditable(final boolean canEdit) {
        editable = canEdit;
    }

    /**
     * @param canAccept set true if the editorcomponent uses return character.
     */
    public final void setAcceptReturnKey(final boolean canAccept) {
        acceptsReturnKey = canAccept;
    }

    /**
     * @return is the return character used by this editor.
     */
    public final boolean isReturnKeyAccepted() {
        return acceptsReturnKey;
    }

    /**
     * @return pos the start location in the JTextComponent for this editor.
     */
    public final int getStartPos() {
        return startPos;
    }

    /**
     * @param pos the start location in the JTextComponent for this editor.
     */
    public final void setStartPos(final int pos) {
        startPos = pos;
    }

    /**
     * @return the current text of this editor.
     */
    public final String getText() {
        return editorText;
    }

    /**
     * Set the text of the editorcomponent and update the text segment of the
     * JTextComponent.
     *
     * @param text new text to set.
     */
    public final void setText(final String text) {
        int prevlength = editorText.length();
        editorText = text;
        int localPos = getCaretPosition();
        replaceRange(text, startPos, startPos + prevlength);
        setCaretPosition(localPos);
    }

    /**
     * Set the text without updating the associated JTextComponent.
     *
     * @param text new text to set.
     */
    public final void resetText(final String text) {
        editorText = text;
    }

    /**
     * Utility function for replacing a segment of a string with another.
     *
     * @param text  new segment of text to set.
     * @param start start position of the new text.
     * @param end   length of the segment being replaced.
     */
    private void replaceRange(final String text,
                              final int start,
                              final int end) {
        String fullText = parentComp.getText();
        parentComp.setText(fullText.substring(0, start) + text
                + fullText.substring(end));
    }

    /**
     * @return The parent Swing component for this EditorComponent.
     */
    public final JTextComponent getParentComponent() {
        return this.parentComp;
    }

    /**
     * @return The caret location within the text segment, relative to the this
     * editor component.
     */
    public final int getCaretPosition() {
        int pos = 0;

        if (parentComp != null) {
            pos = Math.max(0, parentComp.getCaretPosition() - startPos);
            pos = Math.min(pos, editorText.length());
        }

        return pos;
    }

    /**
     * Set the caret position of the parentComponent given a local value to
     * set within the editor.
     *
     * @param localPos Position of caret relative to the start of this editor.
     */
    public final void setCaretPosition(final int localPos) {
        if (parentComp != null) {
            int pos = Math.max(0, localPos);
            pos = Math.min(Math.min(pos, editorText.length()), parentComp.getText().length());

            parentComp.setCaretPosition(startPos + pos);
        }
    }

    /**
     * @return the selection start within the segment as a local value.
     */
    public final int getSelectionStart() {
        int pos = 0;
        if (parentComp != null) {
            pos = Math.max(0, parentComp.getSelectionStart() - startPos);
            pos = Math.min(pos, editorText.length());
        }

        return pos;
    }

    /**
     * @return the selection end within the segment as a local value.
     */
    public final int getSelectionEnd() {
        int pos = 0;
        if (parentComp != null) {
            pos = Math.max(0, parentComp.getSelectionEnd() - startPos);
            pos = Math.min(pos, editorText.length());
        }

        return pos;
    }

    /**
     * Select all of this segments text in the JTextComponent.
     */
    public final void selectAll() {
        if (parentComp != null) {
            parentComp.select(startPos, startPos + editorText.length());
        }
    }

    /**
     * @return True if the all of the text in this editor component is selected,
     * false otherwise.
     */
    public final boolean isAllSelected() {
        int selectionLength = this.getSelectionEnd() - this.getSelectionStart();
        return (selectionLength == this.getText().length());
    }

    /**
     * Given a startClick position and endClick position, select the text in
     * the JTextComponent.
     *
     * @param startClick character position of the start of the click.
     * @param endClick   character position of the end of the click.
     */
    public void select(final int startClick, final int endClick) {
        if (parentComp != null) {
            int start = Math.max(startPos, startClick);
            start = Math.min(startPos + editorText.length(), start);

            int end = Math.max(startPos, endClick);
            end = Math.min(startPos + editorText.length(), end);
            parentComp.select(start, end);
        }
    }

    /**
     * Empty implementation of the ClipboardOwner interface.
     */
    @Override
    public void lostOwnership(Clipboard aClipboard, Transferable aContents) {
        //do nothing
    }

    /**
     * Cut the contents of the current selection into the clipboard.
     */
    public void cut() {
        // Copy the selection into the clipboard.
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        String aString = this.getText()
                .substring(this.getSelectionStart(),
                        this.getSelectionEnd());
        StringSelection stringSelection = new StringSelection(aString);
        clipboard.setContents(stringSelection, this);

        // Pass in a backspace character to delete the current selection.
        KeyEvent ke = new KeyEvent(this.getParentComponent(), 0, 0,
                0, 0, '\u0008');
        this.keyPressed(ke);
        this.keyTyped(ke); //@todo All delete and backspace code should be in keyPressed, not keyTyped
    }

    /**
     * Paste the contents of the clipboard into the contents of the
     * EditorComponent.
     */
    public void paste() {
        // Get the contents of the clipboard.
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        boolean hasText = (contents != null)
                && contents.isDataFlavorSupported(DataFlavor.stringFlavor);

        // No valid text in clipboard. Bail.
        if (!hasText) {
            return;
        }

        // Valid text in clipboard
        try {
            // Get the text from the clipboard
            String text = (String) contents
                    .getTransferData(DataFlavor.stringFlavor);

            for (int i = 0; i < text.length(); i++) {
                KeyEvent ke = new KeyEvent(this.getParentComponent(), 0, 0,
                        0, 0, text.charAt(i));
                this.keyTyped(ke);
            }

        } catch (Exception ex) {
            LOGGER.error("Unable to get clipboard contents", ex);
        }
    }

    /**
     * Rather than delete characters.
     *
     * @param c The character to use when deleting (rather than deleting - the
     *          supplied character is used to replace).
     */
    public final void setDeleteChar(final char c) {
        isDeletingChar = false;
        replaceChar = c;
    }

    /**
     * Adds characters to the list that must be preserved by the editor
     * (characters that can not be deleted).
     *
     * @param pChars The characters to be preserved.
     */
    public final void addPreservedChars(final String pChars) {
        preservedChars = preservedChars.concat(pChars);
    }

    /**
     * @param aChar Character to test
     * @return true if the character is a reserved character.
     */
    public final boolean isPreserved(final char aChar) {
        return (preservedChars.indexOf(aChar) >= 0);
    }

    /**
     * Removes characters from ahead of the caret if they are not in the
     * preservedChars parameter. If the character is to be preserved, this
     * method will simple shift the caret forward one spot.
     */
    public final void removeAheadOfCaret() {
        // Underlying text field has selection no caret, remove everything that
        // is selected.
        if ((getSelectionEnd() - getSelectionStart()) > 0) {
            removeSelectedText();

            // Underlying Text field has no selection, just a caret. Go ahead and
            // manipulate it as such.
        } else if (getText() != null && getText().length() > 0
                && getCaretPosition() < getText().length()) {
            // Check ahead of caret to see if it is a preserved character. If
            // the character is preserved - simply move the caret ahead one spot
            // and leave the preserved character untouched.
            if (isPreserved(getText().charAt(getCaretPosition()))) {
                setCaretPosition(getCaretPosition() + 1);
            }

            // Delete next character.
            StringBuilder currentValue = new StringBuilder(getText());
            currentValue.deleteCharAt(getCaretPosition());

            if (!isDeletingChar) {
                currentValue.insert(getCaretPosition(), replaceChar);
            }

            int cPosition = getCaretPosition();
            this.setText(currentValue.toString());
            setCaretPosition(cPosition);
        }
    }

    /**
     * Removes characters from behind the caret if they are not in the
     * preservedChars parameter. If the character is to be preserved, this
     * method will simply shift the caret back one spot.
     */
    public final void removeBehindCaret() {
        // Underlying text field has selection and no caret, simply remove
        // everything that is selected.
        if ((getSelectionEnd() - getSelectionStart()) > 0) {
            removeSelectedText();

            // Underlying text field has no selection, just a caret. Go ahead and
            // manipulate it as such.
        } else if (getText() != null && getText().length() > 0) {
            // Check behind the caret to see if it is a preserved character. If
            // the character is preserved - simply move the caret back one spot
            // and leave the preserved character untouched.
            int carPosMinusOne = Math.max(0, getCaretPosition() - 1);
            if (isPreserved(getText().charAt(carPosMinusOne))) {
                setCaretPosition(carPosMinusOne);
                carPosMinusOne = Math.max(0, getCaretPosition() - 1);
            }

            // Delete previous character.
            StringBuilder currentValue = new StringBuilder(getText());
            currentValue.deleteCharAt(carPosMinusOne);
            if (!isDeletingChar) {
                currentValue.insert(carPosMinusOne, replaceChar);
            }

            int cPosition = carPosMinusOne;
            this.setText(currentValue.toString());
            setCaretPosition(cPosition);
        }
    }

    /**
     * This method will remove any characters that have been selected in the
     * underlying text field and that don't exist in the preservedChars
     * parameter. If no characters have been selected, the underlying text field
     * is unchanged.
     */
    public final void removeSelectedText() {
        // Get the current value of the visual representation of this DataValue.
        StringBuilder cValue = new StringBuilder(getText());

        // Obtain the start and finish of the selected text.
        int start = this.getSelectionStart();
        int end = this.getSelectionEnd();
        int pos = start;

        for (int i = start; i < end; i++) {

            // Current character is not reserved - either delete or replace it.
            if (!isPreserved(cValue.charAt(pos))) {
                cValue.deleteCharAt(pos);

                // Replace the character rather than remove it, we then need to
                // skip to the next position to delete a character.
                if (!isDeletingChar) {
                    cValue.insert(pos, replaceChar);
                    pos++;
                }

                // Current character is reserved, skip over current position.
            } else {
                pos++;
            }
        }

        // BugzID:747 - If all we have is preserved chars clear everything.
        String newValue = cValue.toString();
        boolean foundNonPreserved = false;
        for (int i = 0; i < newValue.length(); i++) {
            if (!isPreserved(newValue.charAt(i))) {
                foundNonPreserved = true;
                break;
            }
        }

        // Set the text for this data value to the new string.
        if (foundNonPreserved) {
            this.setText(newValue);
            this.setCaretPosition(start);
        } else {
            this.setText("");
            this.setCaretPosition(0);
        }
    }

    /**
     * @return true if this editor has the focus.
     */
    public final boolean hasFocus() {
        return parentComp.hasFocus();
    }
}
