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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.Datavyu;
import org.datavyu.controllers.DeleteColumnController;
import org.datavyu.models.db.Argument;
import org.datavyu.models.db.DataStore;
import org.datavyu.models.db.UserWarningException;
import org.datavyu.models.db.Variable;
import org.datavyu.undoableedits.AddVariableEdit;
import org.datavyu.undoableedits.RemoveVariableEdit;
import org.datavyu.views.discrete.datavalues.vocabelements.FormalArgEditor;
import org.datavyu.views.discrete.datavalues.vocabelements.VocabElementV;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import javax.swing.*;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * A view for editing the database vocab.
 */
public final class VocabEditorV extends DatavyuDialog {

    /** The logger for this class */
    private static Logger LOGGER = LogManager.getLogger(VocabEditorV.class);

    /** Data store for the annotations of streams */
    private DataStore dataStore;

    /** All the vocab views displayed in the editor */
    private List<VocabElementV> veViews;

    /** The currently selected vocab element */
    private VocabElementV selectedVocabElement;

    /** The currently selected formal argument */
    private FormalArgEditor selectedArgument;

    /** Index of the currently selected formal argument within the element */
    private int selectedArgumentI;

    /** Vertical frame for holding the current listing of Vocab elements */
    private JPanel verticalFrame;

    /** The handler for all keyboard shortcuts */
    private KeyEventDispatcher ked;

    /** Swing components */
    private JButton addCodeButton;
    private JButton addColumnButton;
    private JButton closeButton;
    private JScrollPane currentVocabList;
    private JButton deleteButton;
    private JLabel jLabel1;
    private JScrollPane jScrollPane1;
    private JTextArea jTextArea1;
    private JButton moveCodeLeftButton;
    private JButton moveCodeRightButton;
    private JLabel statusBar;
    private JSeparator statusSeperator;
    private JLabel nameWarningsLabel;

    private java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/datavyu/views/resources/VocabEditorV");


    /**
     * Constructor.
     *
     * @param parent The parent frame for the vocab editor.
     * @param modal  Is this dialog to be modal or not?
     */
    public VocabEditorV(final Frame parent, final boolean modal) {
        super(parent, modal);

        LOGGER.info("vocEd - show");
        dataStore = Datavyu.getProjectController().getDataStore();

        initComponents();
        componentListnersInit();
        setName(this.getClass().getSimpleName());
        selectedVocabElement = null;
        selectedArgument = null;
        selectedArgumentI = -1;

        // manage keyboard inputs
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kfm.addKeyEventDispatcher(
                ked = new KeyEventDispatcher() {
                    @Override
                    public boolean dispatchKeyEvent(final KeyEvent ke) {

                        boolean result = false;
                        //determine what key was pressed
                        if (ke.getID() == KeyEvent.KEY_RELEASED) {
                            switch (ke.getKeyCode()) {

                                case KeyEvent.VK_ESCAPE:
                                    closeWindow();
                                    break;
                            }
                        }
                        if (ke.isControlDown() && (ke.getID() == KeyEvent.KEY_RELEASED)) {
                            switch (ke.getKeyCode()) {
                                case KeyEvent.VK_M:
                                    addColumn();
                                    break;
                                case KeyEvent.VK_A:
                                    if (selectedVocabElement != null) {
                                        addCode();
                                    }
                                    break;
                                case KeyEvent.VK_S:
                                    applyChanges();
                                    break;
                                case KeyEvent.VK_DELETE:
                                    delete();
                                    break;
                                case KeyEvent.VK_LEFT:
                                    if (moveCodeLeftButton.isEnabled())
                                        moveArgumentLeft(); //on mac may want to advise ctrl+shift+left (ctrl+left switches detsktops)
                                    break;
                                case KeyEvent.VK_RIGHT:
                                    if (moveCodeRightButton.isEnabled()) moveArgumentRight();
                                    break;
                                default:
                                    result = false;
                            }
                        }

                        if (result)
                            ke.consume();

                        return result;
                    }
                });

        addWindowFocusListener(
                new java.awt.event.WindowAdapter() {
                    public void windowGainedFocus(java.awt.event.WindowEvent e) {
                        if (!isCurrent()) makeElements();
                    }

                    public void windowLostFocus(java.awt.event.WindowEvent e) {
                        //applyChanges(); //update spreadsheet here?
                    }
                });

        makeElements();
    }

    private void makeElements() {
        // Populate current vocab list with vocab data from the database.
        dataStore = Datavyu.getProjectController().getDataStore();
        veViews = new ArrayList<VocabElementV>();
        verticalFrame = new JPanel();
        verticalFrame.setName("verticalFrame");
        verticalFrame.setLayout(new BoxLayout(verticalFrame, BoxLayout.Y_AXIS));

        for (Variable var : dataStore.getAllVariables()) {
            Argument argument = var.getRootNode();
            if (argument.type.equals(Argument.Type.MATRIX)) {
                VocabElementV matrixV = new VocabElementV(argument, var, this);
                verticalFrame.add(matrixV);
                veViews.add(matrixV);
            }
        }

        // Add a pad cell to fill out the bottom of the vertical frame.
        JPanel holdPanel = new JPanel();
        holdPanel.setBackground(Color.white);
        holdPanel.setLayout(new BorderLayout());
        holdPanel.add(verticalFrame, BorderLayout.NORTH);
        currentVocabList.setViewportView(holdPanel);
        refreshNameWarnings(true);
        updateDialogState();

        String titleName = Datavyu.getProjectController().getProjectNamePretty();
        setTitle(bundle.getString("window.title") + " - " + titleName);
    }

    private boolean isCurrent() {
        List<Variable> varList = Datavyu.getProjectController().getDataStore().getAllVariables();
        java.util.ListIterator<Variable> varIt = varList.listIterator();
        for (VocabElementV v : veViews) //wish i could map...
        {
            String curVocab = v.getNameComponent().getText();
            if (varIt.hasNext()) {
                String curDatastore = varIt.next().getName();
                if (!curVocab.equals(curDatastore)) return false;
            } else {
                return false;
            }
        }

        return !varIt.hasNext(); //return true iff both lists are exhausted
    }

    /**
     * The action to invoke when the user clicks on the add matrix button.
     */
    @Action
    public void addColumn() {
        String varName = "column" + getMatNameNum();
        try {
            LOGGER.info("vocEd - add column");

            // perform the action
            Variable v = dataStore.createVariable(varName, Argument.Type.MATRIX);
            // Need to get the template from the variable.
            //Matrix m = v.getCellValue();
            //m.createArgument(Argument.type.NOMINAL);

            VocabElementV matrixV = new VocabElementV(v.getRootNode(), v, this);
            verticalFrame.add(matrixV);
            veViews.add(matrixV);

            // record the effect
            UndoableEdit edit = new AddVariableEdit(varName, Argument.Type.MATRIX);
            Datavyu.getView().getUndoSupport().postEdit(edit);

            matrixV.requestFocus();
            matrixV.rebuildContents();
            matrixV.requestFocus();

            applyChanges();
            updateDialogState();

            // Whoops, user has done something strange - show warning dialog.
        } catch (UserWarningException fe) {
            Datavyu.getApplication().showWarningDialog(fe);
        }
    }

    public void disposeAll() {
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kfm.removeKeyEventDispatcher(ked);
        dispose();
    }

    /**
     * The action to invoke when the user clicks on the move arg left button.
     */
    @Action
    public void moveArgumentLeft() {
        LOGGER.error("vocEd - move code left");
        Argument va = selectedVocabElement.getModel().childArguments.get(selectedArgumentI);
        Variable var = selectedVocabElement.getVariable();
        var.moveArgument(va.name, var.getArgumentIndex(va.name) - 1);

        selectedVocabElement.rebuildContents();

        selectedVocabElement.requestFocus();

        selectedVocabElement.requestArgFocus(selectedVocabElement.getArgumentView(va));

        applyChanges();
        updateDialogState();
    }

    /**
     * The action to invoke when the user clicks on the move arg right button.
     */
    @Action
    public void moveArgumentRight() {
        LOGGER.error("vocEd - move code right");
        Argument va = selectedVocabElement.getModel().childArguments.get(selectedArgumentI);
        Variable var = selectedVocabElement.getVariable();
        var.moveArgument(va.name, var.getArgumentIndex(va.name) + 1);

        selectedVocabElement.rebuildContents();

        selectedVocabElement.requestFocus();

        selectedVocabElement.requestArgFocus(selectedVocabElement.getArgumentView(va));

        applyChanges();
        updateDialogState();
    }

    /**
     * The action to invoke when the user clicks on the add code button.
     */
    @Action
    public void addCode() {
        Variable var = selectedVocabElement.getVariable();
        Argument fa = var.addArgument(Argument.Type.NOMINAL);
        selectedVocabElement.setModel(var.getRootNode());

        String type = "Nominal"; //Hardcoded. Was previously (String) argTypeComboBox.getSelectedItem() but this form element is now removed
        LOGGER.info("vocEd - add argument:" + type);

        selectedVocabElement.rebuildContents();

        // Select the contents of the newly created formal argument.
        selectedVocabElement.requestFocus();
        FormalArgEditor faV = selectedVocabElement.getArgumentView(fa);
        selectedVocabElement.requestArgFocus(faV);

        applyChanges();
        updateDialogState();
    }

    /**
     * The action to invoke when the user presses the delete button.
     */
    @Action
    public void delete() {

        UndoableEdit edit = null;
        // User has vocab element selected - delete it from the editor.
        if (selectedVocabElement != null && selectedArgument == null) {
            if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the variable " +
                    selectedVocabElement.getVariable().getName() + "?", "Confirm delete variable",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                LOGGER.info("vocEd - delete element");
                // record the effect
                List<Variable> varsToDelete = new ArrayList<Variable>();
                varsToDelete.add(selectedVocabElement.getVariable());
                edit = new RemoveVariableEdit(varsToDelete);
                new DeleteColumnController(varsToDelete);

                deleteElementVFromView(selectedVocabElement.getVariable().getName());
                applyChanges();
            }

            // User has argument selected - delete it from the vocab element.
        } else if (selectedArgument != null) {
            LOGGER.info("vocEd - delete argument");
            //TODO no edit created for this!!!
            selectedVocabElement.getVariable().removeArgument(selectedArgument.getModel().name);
            selectedVocabElement.rebuildContents();
            applyChanges();
        }

        updateDialogState();
        if (edit != null) {
            // notify the listeners
            Datavyu.getView().getUndoSupport().postEdit(edit);
        }
    }

    private void deleteElementVFromView(String name) {
        for (VocabElementV i : veViews) {
            if (name.equals(i.getCurrentNameDisplay())) {
                veViews.remove(i);
                verticalFrame.remove(i);

                return;
            }
        }
    }

    /**
     * The action to invoke when the user presses the apply button.
     */
    @Action
    public int applyChanges() {
        LOGGER.info("vocEd - apply");

        int errors = 0;
        updateDialogState();
        Datavyu.getView().getSpreadsheetPanel().redrawCells();

        for (int i = veViews.size() - 1; i >= 0; i--) {
            VocabElementV vev = veViews.get(i);
            if (vev.isDeletable()) {
                //getLegacyDB().removeVocabElement(vev.getModel().getID());
            }
        }

        if (errors != 0) {
            switch (errors) {
                case 1:
                    JOptionPane.showMessageDialog(this, "Vocab Element name in use.", "Error adding vocab", 2);
                    break;
                case 2:
                    JOptionPane.showMessageDialog(this, "Code name in use.", "Duplicate code name", 2);
                    break;
            }

        }
        return errors;
    }

    /**
     * The action to invoke when the user presses the OK button.
     */
    @Action
    public void ok() {
        LOGGER.info("vocEd - ok");
        if (applyChanges() == 0) {
            try {
                dispose();
            } catch (Throwable e) {
                LOGGER.error("Unable to destroy vocab editor view.", e);
            }
        }
    }

    /**
     * The action to invoke when the user presses the cancel button.
     */
    @Action
    public void closeWindow() {
        LOGGER.info("vocEd - close");
        try {
            dispose();
        } catch (Throwable e) {
            LOGGER.error("Unable to destroy vocab editor view.", e);
        }
        applyChanges();
        updateDialogState();
    }

    /**
     * Returns vector of VocabElementVs
     *
     * @return veViews Vector of VocabElementVs
     */
    public List<VocabElementV> getVocabElements() {
        return veViews;
    }

    /**
     * Method to update the visual state of the dialog to match the underlying
     * model.
     */
    public void updateDialogState() {
        ResourceMap rMap = Application.getInstance(Datavyu.class).getContext()
                .getResourceMap(VocabEditorV.class);

        //boolean containsC = false;
//        selectedVocabElement = null;
//        selectedArgument = null;

        for (VocabElementV vev : veViews) {
            // A vocab element has focus - enable certain things.
            if (vev.hasFocus()) {
                selectedVocabElement = vev;
                selectedArgument = vev.getArgWithFocus();
                if (selectedArgument != null)
                    selectedArgumentI = vev.getArgWithFocus().getArgPos();
                else
                    selectedArgumentI = -1;
            }

            // A vocab element contains a change - enable certain things.
            //jc 2-26: not sure what this was intended for. if re-enabling, beware that .hasChanged() is gone.
            //if (vev.hasChanged() || vev.isDeletable()) {
            //    containsC = true;
            //}
        }
/*New
        if (containsC) {
            closeButton.setText(rMap.getString("closeButton.cancelText"));
            closeButton.setToolTipText(rMap.getString("closeButton.cancelTip"));

        } else {
            closeButton.setText(rMap.getString("closeButton.cancelText"));
            closeButton.setToolTipText(rMap.getString("closeButton.cancelTip"));
        }
*/

        // If we have a selected vocab element - we can enable additional
        // functionality.
/*
        if (selectedVocabElement != null) {
            addCodeButton.setEnabled(true);
            deleteButton.setEnabled(true);
        } else {
            addCodeButton.setEnabled(false);
            deleteButton.setEnabled(false);
        }
*/
        if (selectedArgument != null) {

            // W00t - argument is selected - populate the index so that the user
            // can shift the argument around.
            if (selectedArgumentI > 0) {
                moveCodeLeftButton.setEnabled(true);
            } else {
                moveCodeLeftButton.setEnabled(false);
            }

            if (selectedArgumentI < (selectedVocabElement.getModel().childArguments.size() - 1)) {
                moveCodeRightButton.setEnabled(true);
            } else {
                moveCodeRightButton.setEnabled(false);
            }
        } else {
            moveCodeLeftButton.setEnabled(false);
            moveCodeRightButton.setEnabled(false);
        }

        toFront(); //to fix Bug 120 - code editor losing focus on Windows
    }

    /**
     * This method is called from within the constructor to initialize the form.
     */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        addColumnButton = new javax.swing.JButton();
        moveCodeLeftButton = new javax.swing.JButton();
        moveCodeRightButton = new javax.swing.JButton();
        addCodeButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        currentVocabList = new javax.swing.JScrollPane();
        closeButton = new javax.swing.JButton();
        statusBar = new javax.swing.JLabel();
        statusSeperator = new javax.swing.JSeparator();
        jLabel1 = new javax.swing.JLabel();
        nameWarningsLabel = new javax.swing.JLabel();

        jScrollPane1.setName("jScrollPane1");

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setName("jTextArea1");
        jScrollPane1.setViewportView(jTextArea1);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(bundle.getString("window.title"));
        setName("Form");
        getContentPane().setLayout(new java.awt.GridBagLayout());

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(org.datavyu.Datavyu.class).getContext().getActionMap(VocabEditorV.class, this);

        addCodeButton.setAction(actionMap.get("addCode"));
        addCodeButton.setText(bundle.getString("addCodeButton.text"));
        addCodeButton.setToolTipText(bundle.getString("addCodeButton.tip"));
        addCodeButton.setName("addCodeButton");
        addCodeButton.setHorizontalAlignment(SwingConstants.LEFT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        //gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        getContentPane().add(addCodeButton, gridBagConstraints);


        addColumnButton.setAction(actionMap.get("addColumn"));
        addColumnButton.setText(bundle.getString("addColumnButton.text"));
        addColumnButton.setToolTipText(bundle.getString("addColumnButton.tip"));
        addColumnButton.setName("addColumnButton");
        addColumnButton.setMinimumSize(addCodeButton.getPreferredSize());
        addColumnButton.setPreferredSize(addCodeButton.getPreferredSize());
        addColumnButton.setHorizontalAlignment(SwingConstants.LEFT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(addColumnButton, gridBagConstraints);

        moveCodeLeftButton.setAction(actionMap.get("moveArgumentLeft"));
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.datavyu.Datavyu.class).getContext().getResourceMap(VocabEditorV.class);
        moveCodeLeftButton.setIcon(resourceMap.getIcon("moveCodeLeftButton.icon"));
        moveCodeLeftButton.setText(bundle.getString("moveCodeLeftButton.text"));
        moveCodeLeftButton.setToolTipText(bundle.getString("moveCodeLeftButton.tip"));
        moveCodeLeftButton.setName("moveCodeLeftButton");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        getContentPane().add(moveCodeLeftButton, gridBagConstraints);

        moveCodeRightButton.setAction(actionMap.get("moveArgumentRight"));
        moveCodeRightButton.setIcon(resourceMap.getIcon("moveCodeRightButton.icon"));
        moveCodeRightButton.setText(bundle.getString("moveCodeRightButton.text"));
        moveCodeRightButton.setToolTipText(bundle.getString("moveCodeRightButton.tip"));
        moveCodeRightButton.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        moveCodeRightButton.setName("moveCodeRightButton");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 5);
        getContentPane().add(moveCodeRightButton, gridBagConstraints);

        deleteButton.setAction(actionMap.get("delete"));
        deleteButton.setText(bundle.getString("deleteButton.text"));
        deleteButton.setToolTipText(bundle.getString("deleteButton.tip"));
        deleteButton.setMaximumSize(new java.awt.Dimension(100, 23));
        deleteButton.setMinimumSize(new java.awt.Dimension(100, 23));
        deleteButton.setName("deleteButton");
        deleteButton.setPreferredSize(new java.awt.Dimension(100, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 0, 5);
        getContentPane().add(deleteButton, gridBagConstraints);

        currentVocabList.setMinimumSize(new java.awt.Dimension(23, 200));
        currentVocabList.setName("currentVocabList");
        currentVocabList.setPreferredSize(new java.awt.Dimension(200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        //gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(currentVocabList, gridBagConstraints);

        nameWarningsLabel.setText("");
        nameWarningsLabel.setName("nameWarningLabel");
        nameWarningsLabel.setForeground(Color.RED);
        nameWarningsLabel.setHorizontalAlignment(SwingConstants.LEFT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 0);
        getContentPane().add(nameWarningsLabel, gridBagConstraints);

        closeButton.setAction(actionMap.get("closeWindow"));
        closeButton.setText(bundle.getString("closeButton.closeText"));
        closeButton.setToolTipText(bundle.getString("closeButton.closeTip"));
        closeButton.setName("closeButton");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        getContentPane().add(closeButton, gridBagConstraints);

        statusBar.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusBar.setText(resourceMap.getString("statusBar.text"));
        statusBar.setDoubleBuffered(true);
        statusBar.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        statusBar.setMaximumSize(new java.awt.Dimension(100, 14));
        statusBar.setMinimumSize(new java.awt.Dimension(10, 14));
        statusBar.setName("statusBar");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        getContentPane().add(statusBar, gridBagConstraints);
        statusBar.getAccessibleContext().setAccessibleName(resourceMap.getString("statusBar.AccessibleContext.accessibleName"));

        statusSeperator.setMinimumSize(new java.awt.Dimension(100, 10));
        statusSeperator.setName("statusSeperator");
        statusSeperator.setPreferredSize(new java.awt.Dimension(2, 2));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(statusSeperator, gridBagConstraints);

        jLabel1.setText(resourceMap.getString("jLabel1.text"));
        jLabel1.setMaximumSize(new java.awt.Dimension(85, 5));
        jLabel1.setMinimumSize(new java.awt.Dimension(80, 5));
        jLabel1.setName("jLabel1");
        jLabel1.setPreferredSize(new java.awt.Dimension(85, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        getContentPane().add(jLabel1, gridBagConstraints);

        pack();
    }

    /**
     * Initialization of mouse listeners on swing elements
     */
    private void componentListnersInit() {

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent me) {
                String component = me.getComponent().getName();
                if (component.equals("closeButton")) {
                    statusBar.setText("Close the editor");
                } else if (component.equals("addColumnButton")) {
                    statusBar.setText("Add a new column. Hotkey: ctrl + M");
                } else if (component.equals("undoButton")) {
                    statusBar.setText("Undo a series of changes. Hotkey: ctrl + Z");
                } else if (component.equals("redoButton")) {
                    statusBar.setText("Redo any undone changes. Hotkey: ctrl + Y");
                } else if (component.equals("addCodeButton")) {
                    statusBar.setText("Add a new code to a column. Hotkey: ctrl + A");
                } else if (component.equals("deleteButton")) {
                    statusBar.setText("Delete a code or column. Hotkey: ctrl + delete");
                } else if (component.equals("moveCodeLeftButton")) {
                    statusBar.setText("Move a code left within a column. Hotkey: ctrl + <-");
                } else if (component.equals("applyButton")) {
                    statusBar.setText("Apply changes to the vocab elements. Hotkey: ctrl + S");
                } else if (component.equals("moveCodeRightButton")) {
                    statusBar.setText("Move a code right within a column. Hotkey: ctrl + ->");
                } else if (component.equals("okButton")) {
                    statusBar.setText("Save changes and close the window.");
                }
            }

            @Override
            public void mouseExited(MouseEvent me) {
                statusBar.setText(" ");
            }
        };

        currentVocabList.addMouseListener(ma);
        addColumnButton.addMouseListener(ma);
        deleteButton.addMouseListener(ma);
        closeButton.addMouseListener(ma);
        addCodeButton.addMouseListener(ma);
        moveCodeLeftButton.addMouseListener(ma);
        moveCodeRightButton.addMouseListener(ma);

    }

    /**
     * Determine the number of the next matrix added to the vocab list
     */
    private int getMatNameNum() {
        int max = 0;
        for (VocabElementV vev : veViews) {
            if (vev.getModel().type.equals(Argument.Type.MATRIX)) {
                max += 1;
            }
        }

        return max + 1;
    }

    public void refreshNameWarnings() {
        refreshNameWarnings(false);
    }

    public void refreshNameWarnings(boolean first) {
        String s = "";
        for (VocabElementV i : veViews) {
            if (i.getInvalid()) {
                String curString = i.getCurrentNameDisplay();
                if (!s.isEmpty()) s += ", ";
                if (curString.isEmpty()) s += "<i>(empty string)</i>";
                s += "<i>" + escapeHtml4(i.getCurrentNameDisplay()) + "</i>  ";
                //s += "remains '"+i.getLastValid()+"'. "; //this would include a reminder of what the last valid name was
            }

        }

        if (s.isEmpty() && first != true) {
            nameWarningsLabel.setText("All changes applied.");
            nameWarningsLabel.setForeground(Color.BLACK);
        } else if (s.isEmpty() && first == true) {
            nameWarningsLabel.setText("");
            nameWarningsLabel.setForeground(Color.BLACK);
        } else {
            nameWarningsLabel.setText("<html><b><u>Invalid names:</u></b> " + s + "<br />Names must begin with a letter, be non-empty, and underscore is the ONLY permitted special character. Repeated names are not allowed.</html>");
            nameWarningsLabel.setForeground(Color.RED);
        }
    }

    /**
     * Determine what action to perform when a VocabElement is removed from the vocab list
     *
     * @param db the current database
     * @param VEID the identifier of the vocab element to be deleted
     *//*
    @Override
    public void VLDeletion(Database db, long VEID) {
        try{
            if(db.vocabElementExists(VEID)){
                VocabElement ve = db.getVocabElement(VEID);
                for(int i=ve.getNumFormalArgs()-1; i>= 0; i--){
                    ve.deleteFormalArg(i);
                }
                int delIndex=0;
                for(VocabElementV view: veViews){
                    long vID = view.getModel().getID();
                    if(vID == VEID){
                        verticalFrame.remove(delIndex);
                        verticalFrame.revalidate();
                        veViews.remove(delIndex);
                        break;  // only ever delete one element & avoid breaking loop
                    }
                    delIndex++;
                }
            }
        }catch(Exception e){
            LOGGER.error("could not delete VE from DB" +e);
        }
    }*/

    /**
     * Determine what action to perform when a VocabElement is replaced in the vocab list
     *
     * @param db the database currently being used
     * @param VEID the identifier of the vocab element that has changed
     *//*
    @Override
    public void VLReplace(Database db, long VEID) {
        try{
        for(int i=0; i < veViews.size()-1;i++){
            if(VEID == veViews.get(i).getModel().getID()){
                verticalFrame.remove(i);
                veViews.remove(i);
                VocabElement ve = db.getVocabElement(VEID);
                VocabElementV vev = new VocabElementV(ve, this);
                verticalFrame.add(vev, i);
                verticalFrame.revalidate();
                veViews.add(i, vev);
            }
        }
        }catch(Exception e){
            LOGGER.error("problem replacing vocab element"+e);
        }
    }*/

    /**
     * Determine what action to perform when a VocabElement is added to the vocab list
     *
     * @param db the current database
     * @param VEID the identifier of the vocab element being inserted
     *//*
    @Override
    public void VLInsertion(Database db, long VEID) {
        try {
            if(db.getVocabElement(VEID) instanceof PredicateVocabElement||
                    db.getMatrixVE(VEID).getType().compareTo(MatrixType.MATRIX)==0){

                boolean exists = false;
                for(int i = 0; i< veViews.size();i++){
                    if(veViews.get(i).getModel().getName().equals(
                            db.getVocabElement(VEID).getName())){
                        exists = true;
                    }
                }

                if(!exists){
                    // if the vocab element is new, give it the default argument
                    VocabElement ve = db.getVocabElement(VEID);
                    if(!(db.getVocabElement(VEID) instanceof PredicateVocabElement)){
                        ve.deleteFormalArg(0);
                        ve.appendFormalArg(new NominalFormalArg(db, "<arg0>"));
                    }
                    // add the vocab element to the appropriate lists
                    VocabElementV vev = new VocabElementV(ve, this);
                    verticalFrame.add(vev);
                    verticalFrame.revalidate();
                    veViews.add(vev);
                }
            }
        } catch (SystemErrorException ex) {
                LOGGER.error("could not add vocab element"+ex);
        }
    }*/
}
