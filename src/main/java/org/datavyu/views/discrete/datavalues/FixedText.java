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

import org.datavyu.views.discrete.EditorComponent;

import javax.swing.text.JTextComponent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

/**
 * Leaf item in the Editor Component for fixed text like brackets and commas.
 * Stubs the abstract methods to nothing. FixedTexts are not "editable" so
 * EditorTracker will never set them to be the current editor, so these stubs
 * will never be called.
 */
public class FixedText extends EditorComponent {

    /**
     * Constructor.
     *
     * @param ta   The Parent JTextComponent that this FixedText editor is nested
     *             within.
     * @param text The initial text to use for this Fixedtext component.
     */
    public FixedText(final JTextComponent ta, final String text) {
        super(ta, text);
    }

    @Override
    public void keyPressed(final KeyEvent e) {
    }

    @Override
    public void keyTyped(final KeyEvent e) {
    }

    @Override
    public void keyReleased(final KeyEvent e) {
    }

    @Override
    public void focusGained(final FocusEvent fe) {
    }
}
