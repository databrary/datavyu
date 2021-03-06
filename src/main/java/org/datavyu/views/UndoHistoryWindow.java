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
package org.datavyu.views;

import org.datavyu.Datavyu;
import org.datavyu.undoableedits.SpreadSheetEdit;
import org.datavyu.undoableedits.SpreadsheetUndoManager;

import java.awt.*;

/**
 * A window that displays the history of actions undertaken by the person using
 * Datavyu.
 */
public class UndoHistoryWindow extends DatavyuDialog {
    private Frame parent;
    private SpreadsheetUndoManager undomanager;
    private boolean firstClickOnItem;

    public UndoHistoryWindow(final java.awt.Frame parent,
                             final boolean modal,
                             final SpreadsheetUndoManager undomanager) {
        super(parent, modal);
        this.parent = parent;
        this.undomanager = undomanager;
        initComponents();
        this.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width - this.getSize().width, this.getLocation().y);
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        undoHistoryScrollPane = new javax.swing.JScrollPane();
        undoHistoryList = new javax.swing.JList();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Undo History");
        setBounds(new java.awt.Rectangle(4, 22, 400, 800));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setName("undoHistoryWindow");
        addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {
                formWindowGainedFocus(evt);
            }

            public void windowLostFocus(java.awt.event.WindowEvent evt) {
            }
        });

        undoHistoryList.setModel((SpreadsheetUndoManager) undomanager);
        undoHistoryList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        undoHistoryList.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        undoHistoryList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                undoHistoryListMouseClicked(evt);
            }
        });
        undoHistoryList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                undoHistoryListValueChanged(evt);
            }
        });
        undoHistoryScrollPane.setViewportView(undoHistoryList);

        getContentPane().add(undoHistoryScrollPane, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void undoHistoryListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_undoHistoryListValueChanged
        // TODO add your handling code here:
        if (evt.getValueIsAdjusting() == false) {
            if (undoHistoryList.getSelectedIndex() > -1) {
                go();
                this.firstClickOnItem = true;
            }

        }
    }//GEN-LAST:event_undoHistoryListValueChanged

    private void undoHistoryListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_undoHistoryListMouseClicked
        if (!this.firstClickOnItem) {
            go();
        }
        this.firstClickOnItem = false;
    }//GEN-LAST:event_undoHistoryListMouseClicked

    private void go() {
        parent.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        this.undoHistoryList.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        undomanager.goTo((SpreadSheetEdit) undoHistoryList.getSelectedValue());
        Datavyu.getView().refreshUndoRedo();
        this.rootPane.revalidate();
        this.rootPane.repaint();
        this.undoHistoryList.requestFocus();
        parent.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        this.undoHistoryList.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void formWindowGainedFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
        // TODO add your handling code here:
        int selected = undoHistoryList.getSelectedIndex();
        if (selected == -1) {
            undoHistoryList.ensureIndexIsVisible(undoHistoryList.getModel().getSize() - 1);
        } else {
            undoHistoryList.ensureIndexIsVisible(selected);
        }
    }//GEN-LAST:event_formWindowGainedFocus

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList undoHistoryList;
    private javax.swing.JScrollPane undoHistoryScrollPane;
    // End of variables declaration//GEN-END:variables
}
