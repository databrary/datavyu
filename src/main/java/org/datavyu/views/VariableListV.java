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

import com.google.common.collect.HashBiMap;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.Datavyu;
import org.datavyu.models.db.DataStore;
import org.datavyu.models.db.DataStoreListener;
import org.datavyu.models.db.UserWarningException;
import org.datavyu.models.db.Variable;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 * The dialog to list database variables.
 */
public final class VariableListV extends DatavyuDialog
        implements TableModelListener, DataStoreListener {

    /**
     * The column for if a variable is visible or not.
     */
    private static final int VCOLUMN = 1;
    /**
     * The column for a variables name.
     */
    private static final int NCOLUMN = 2;
    /**
     * The column for order index of variable
     */
    private static final int OCOLUMN = 0;
    /**
     * The total number of columns in the variables list.
     */
    private static final int TOTAL_COLUMNS = 3;
    /**
     * The logger for this class.
     */
    private static Logger LOGGER = LogManager.getLogger(VariableListV.class);
    /**
     * DataStore for holding all the information in the variable list.
     */
    private DataStore dataStore;

    /**
     * The table model of the JTable that lists the actual variables.
     */
    private VListTableModel tableModel;

    /**
     * Visual components for the dialog.
     */
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable variableList;
    private javax.swing.JLabel changeInstructions;

    /**
     * Mapping between database column identifier - to the table model.
     */
    private HashBiMap<Variable, Integer> dbToTableMap;

    /**
     * Resource map holding all the string information.
     */
    private ResourceMap rMap = Application.getInstance(Datavyu.class)
            .getContext()
            .getResourceMap(VariableListV.class);

    /**
     * Creates new form ListVariablesView.
     *
     * @param parent The parent frame for this dialog.
     * @param modal  Is this dialog to be modal (true), or not.
     * @param ds     The DataStore containing the variables you wish to list.
     */
    public VariableListV(final java.awt.Frame parent,
                         final boolean modal,
                         final DataStore ds) {
        super(parent, modal);
        tableModel = new VListTableModel();
        dbToTableMap = HashBiMap.create();
        initComponents();
        setName(this.getClass().getSimpleName());
        dataStore = ds;

        // Set the names of the columns.
        tableModel.addColumn(rMap.getString("Table.orderColumn"));
        tableModel.addColumn(rMap.getString("Table.visibleColumn"));
        tableModel.addColumn(rMap.getString("Table.nameColumn"));
        
        //fix visible and ordering columns to exact size, let last one grow with table
        variableList.getColumnModel().getColumn(VCOLUMN).setMinWidth(60);
        variableList.getColumnModel().getColumn(VCOLUMN).setMaxWidth(60);    
        variableList.getColumnModel().getColumn(OCOLUMN).setMinWidth(40);
        variableList.getColumnModel().getColumn(OCOLUMN).setMaxWidth(40);
        variableList.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        
        //Use JTextfield to edit variable name cells
        variableList.getColumnModel().getColumn(NCOLUMN)
                .setCellEditor(new DefaultCellEditor(makeTextFieldWithHoverMessage()));
        
        populateTable();
    }
    
    private JTextField makeTextFieldWithHoverMessage()
    {
        JTextField ans = new JTextField();
        ans.setToolTipText("Click to edit");
        return ans;
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        jScrollPane1 = new javax.swing.JScrollPane();
        variableList = new javax.swing.JTable();
        changeInstructions = new JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(rMap.getString("variableListDialog.title"));
        setName("variableListDialog"); 
        
        changeInstructions.setText(rMap.getString("changeInstructions.text"));

        jScrollPane1.setName("jScrollPane1"); 

        variableList.setModel(tableModel);
        variableList.setMinimumSize(new java.awt.Dimension(400, 200));
        variableList.setName("variableList");
        variableList.setAutoCreateRowSorter(true);
        jScrollPane1.setViewportView(variableList);
        

       

        MigLayout layout = new MigLayout("wrap");
        getContentPane().setLayout(layout);
        getContentPane().add(jScrollPane1);
        getContentPane().add(changeInstructions);

        pack();
    }

    public void populateTable() {
        for (Variable var : dataStore.getAllVariables()) {
            insertRow(var, rMap);
        }
    }

    public void recreateMap() {
        dbToTableMap.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            dbToTableMap.put(dataStore.getVariable((String) tableModel.getValueAt(i, NCOLUMN)), i);
        }
    }

    /**
     * Registers this as a listener for all the stuff that we need to be
     * notified about when things change in the UserInterface.
     */
    public void registerListeners() {
        tableModel.addTableModelListener(this);
        dataStore.addListener(this);
    }

    /**
     * Cleans up this dialog by removing this from the list of stuff that needs
     * to notify us when things change in the user interface.
     */
    public void deRegisterListeners() {
        tableModel.removeTableModelListener(this);
        dataStore.removeListener(this);
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        int row = e.getFirstRow();
        int column = e.getColumn();

        if (row >= 0 && column >= 0) {
            TableModel model = (TableModel) e.getSource();
            String columnName = model.getColumnName(column);
            Object data = model.getValueAt(row, column);

            Variable var = dbToTableMap.inverse().get(row);

            if (columnName.equals(rMap.getString("Table.visibleColumn"))) {
                LOGGER.info("Editied Variable Visibility from VariableList");

                if (var.isHidden() == (Boolean) data) {
                    var.setHidden(!(Boolean) data);
                    var.setSelected(false);
                }

            } else if (columnName.equals(rMap.getString("Table.nameColumn"))) {
                LOGGER.info("Editied Variable Name from VariableList");

                if (!var.getName().equals(data)) {
                    try {
                        if ((!var.getName().equals((String) data))) {
                            var.setName((String) data);
                        }
                    } catch (UserWarningException uwe) {
                        Datavyu.getApplication().showWarningDialog(uwe);
                        tableModel.setValueAt(var.getName(), row, column);
                    }
                }
            }

            Datavyu.getProjectController().getDataStore().markAsChanged();
            Datavyu.getView().getComponent().revalidate();
        }
    }

    /**
     * Inserts a row at the end of the variable list.
     *
     * @param variable The variable to add to the list.
     * @param rMap     The resource map - used to find localised string bundles
     */
    public void insertRow(final Variable variable, final ResourceMap rMap) {
        insertRow(variable, rMap, -1);
    }

    /**
     * Add a new row to the variable list.
     *
     * @param variable The Variable being added to the list.
     * @param rMap     The resource map - used to find localised string bundles.
     * @param position The position to place the new row.
     */
    public void insertRow(final Variable variable,
                          final ResourceMap rMap,
                          int position) {
        Object[] vals = new Object[TOTAL_COLUMNS];

        vals[VCOLUMN] = !variable.isHidden();
        vals[NCOLUMN] = variable.getName();
        //vals[OCOLUMN] = variable.getOrderIndex() +1;
            //sometimes getOrderIndex doesn't begin at 0, or there are dupes, etc. is this a big concern?

        // Add new row to table model.
        int rowId = tableModel.getRowCount();
        if ((position >= 0) && (position <= tableModel.getRowCount())) {
            rowId = position;
        }
        vals[OCOLUMN] = rowId + 1;

        tableModel.insertRow(rowId, vals);
        dbToTableMap.put(variable, rowId);
    }

    @Override
    public void variableAdded(final Variable newVariable) {
        insertRow(newVariable, rMap);
    }

    @Override
    public void variableRemoved(final Variable deletedVariable) {
        tableModel.removeRow(dbToTableMap.get(deletedVariable));
        recreateMap();
    }

    @Override
    public void variableOrderChanged() {
        tableModel.setRowCount(0);
        dbToTableMap.clear();
        populateTable();
    }

    @Override
    public void variableHidden(final Variable hiddenVariable) {
        tableModel.setValueAt(!hiddenVariable.isHidden(),
                dbToTableMap.get(hiddenVariable),
                VCOLUMN);
    }

    @Override
    public void variableVisible(final Variable visibleVariable) {
        tableModel.setValueAt(!visibleVariable.isHidden(),
                dbToTableMap.get(visibleVariable),
                VCOLUMN);
    }

    @Override
    public void variableNameChange(final Variable editedVariable) {
        tableModel.setValueAt(editedVariable.getName(),
                dbToTableMap.get(editedVariable),
                NCOLUMN);
    }

    private class VListTableModel extends DefaultTableModel {
        @Override
        public Class<?> getColumnClass(int column) {
            if (column == VCOLUMN) {
                return Boolean.class;
            }
            return Object.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            if (column == VCOLUMN || column == NCOLUMN) {
                return true;
            }
            return false;
        }
    }
}
