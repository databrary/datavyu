/*
 * VocabElementEditor.java
 *
 * Created on February 20, 2008, 4:34 PM
 */

package au.com.nicta.openshapa.disc.editors;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;

import au.com.nicta.openshapa.db.*;

/**
 *
 * @author  Felix
 */
public class VocabElementEditor
        extends javax.swing.JPanel
        implements ExternalDataColumnListener
{
  Column column;

  /** Creates new form VocabElementEditor */
  public VocabElementEditor(Column variable)
      throws SystemErrorException
  {
    initComponents();
    Database db = variable.getDB();
    db.registerDataColumnListener(variable.getID(), this);
    this.setVariable(variable);
    
    // Add self as a listener
  }

  public void setVariable(Column column)
      throws SystemErrorException
  {
    this.column = column;

    DefaultTreeModel dtm = (DefaultTreeModel)this.variableTree.getModel();
    
    DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(column);

    if (column instanceof DataColumn) {
      ODBCDatabase db = (ODBCDatabase)column.getDB();
System.out.println(this.column.toDBString());
      DataColumn dc = (DataColumn)db.getDataColumn(column.getID());
System.out.println(dc.toDBString());
      MatrixVocabElement mve = db.getMatrixVE(dc.getItsMveID());
System.out.println(mve.toDBString());

      dtm.setRoot(dmtn);
      if ((mve.getType() == MatrixVocabElement.matrixType.MATRIX) ||
          (mve.getType() == MatrixVocabElement.matrixType.PREDICATE)) {
      System.out.println("Count of formal args: " + mve.getNumFormalArgs());
        for (int i=0; i<mve.getNumFormalArgs(); i++) {
          DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(mve.getFormalArg(i));
          dmtn.add(childNode);
        }
      }
    }
  }

  public void DColCellDeletion(Database db,
                               long colID,
                               long cellID)
  {
     
  }
       
  public void DColCellInsertion(Database db,
                                long colID,
                                long cellID)
  {
  }
    
    
  public void DColConfigChanged(Database db,
                                long colID,
                                boolean nameChanged,
                                String oldName,
                                String newName,
                                boolean hiddenChanged,
                                boolean oldHidden,
                                boolean newHidden,
                                boolean readOnlyChanged,
                                boolean oldReadOnly,
                                boolean newReadOnly,
                                boolean varLenChanged,
                                boolean oldVarLen,
                                boolean newVarLen,
                                boolean selectedChanged,
                                boolean oldSelected,
                                boolean newSelected)
  {
  }
    
    
  public void DColDeleted(Database db,
                          long colID)
  {
  }
  
  public final static void main(String[] argv)
  {
    try {
      ODBCDatabase db = new ODBCDatabase();

      DataColumn dc = new DataColumn(db, "TestColumn", MatrixVocabElement.matrixType.MATRIX);
      //DataColumn dc = (DataColumn)db.createColumn(db.COLUMN_TYPE_DATA);
      long colId = db.addColumn(dc);
      dc = (DataColumn)db.getDataColumn(colId);
      System.out.println(dc.toDBString());
      MatrixVocabElement mve = db.getMatrixVE(dc.getItsMveID());
      System.out.println(mve.toDBString());
      
      QuoteStringFormalArg qsfa = new QuoteStringFormalArg(db);
      QuoteStringFormalArg qsfa1 = new QuoteStringFormalArg(db);
      TextStringFormalArg tsfa = new TextStringFormalArg(db);
      TextStringFormalArg tsfa1 = new TextStringFormalArg(db);

      tsfa.setFargName("<TestArgument>");
      tsfa1.setFargName("<TestArgument1>");

      qsfa.setFargName("<QSTestArgument>");
      qsfa1.setFargName("<QSTestArgument1>");
      
      mve.appendFormalArg(tsfa);
      mve.appendFormalArg(tsfa1);
      mve.appendFormalArg(qsfa);
      mve.appendFormalArg(qsfa1);
      
      System.out.println(mve.toDBString());

      db.replaceMatrixVE(mve);
      VocabElementEditor vee = new VocabElementEditor(dc);
      //db.replaceColumn(dc);

      JFrame jf = new JFrame();
      jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      jf.getContentPane().setLayout(new BorderLayout());
      jf.add(vee, BorderLayout.CENTER);
      jf.setSize(new Dimension(300,200));
      jf.setVisible(true);
    } catch (Exception e) {
      System.err.println(e);
      e.printStackTrace();
    }
  }
  
  public void dispose()
  {
      
  }
  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {

    variableScrollPane = new javax.swing.JScrollPane();
    variableTree = new javax.swing.JTree();

    setBackground(java.awt.Color.white);
    setLayout(new java.awt.BorderLayout());

    variableTree.setDragEnabled(true);
    variableTree.setEditable(true);
    variableScrollPane.setViewportView(variableTree);

    add(variableScrollPane, java.awt.BorderLayout.CENTER);
  }// </editor-fold>//GEN-END:initComponents
  
  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JScrollPane variableScrollPane;
  private javax.swing.JTree variableTree;
  // End of variables declaration//GEN-END:variables
  
}
