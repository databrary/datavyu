package au.com.nicta.openshapa.disc.spreadsheet;
/*
 * Spreadsheet.java
 *
 * Created on Feb 5, 2007, 3:50 PM
 */

import java.util.*;
import au.com.nicta.openshapa.*;
import au.com.nicta.openshapa.db.*;
import au.com.nicta.openshapa.disc.*;
import au.com.nicta.openshapa.disc.editors.*;


/**
 *
 * @author  FGA
 */
public class Spreadsheet
    extends     DiscreteDataViewer
{
  public final static int LINEAR_VIEW        = 1;
  public final static int SEMI_TEMPORAL_VIEW = 2;
  public final static int TEMPORAL_VIEW      = 3;

  protected Executive executive;
  protected Database  database;
  
  protected Vector<SpreadsheetColumn> columns = new Vector<SpreadsheetColumn>();

  protected boolean spreadsheetChanged = false;

  protected long lastMinTimeStamp = Long.MAX_VALUE;
  protected long lastMaxTimeStamp = Long.MIN_VALUE;

  protected int spreadsheetView = LINEAR_VIEW;

  /** Creates new form Spreadsheet */
  public Spreadsheet()
  {
    initComponents();
  }

  public Spreadsheet(Executive exec, Database db)
  {
    this();
    this.setExecutive(exec);
    this.setDatabase(db);
  }

  public void setExecutive(Executive exec)
  {
    this.executive = exec;
  }

  public void setDatabase(Database db)
  {
    this.database = db;
  }

  public Executive getExecutive()
  {
    return (this.executive);
  }

  public Database getDatabase()
  {
    return (this.database);
  }

  public void addColumn(SpreadsheetColumn col)
    throws SystemErrorException
  {
    this.columns.addElement(col);
    this.add(col);
    this.updateSpreadsheet();
  }
  
  public void removeColumn(SpreadsheetColumn col)
    throws SystemErrorException
  {
    this.columns.removeElement(col);
    this.remove(col);
    this.updateSpreadsheet();
  }

  public long getScale()
  {
    return (1);
  }

  public long getMinTimeStamp()
    throws SystemErrorException
  {
    if (!spreadsheetChanged) {
      return (lastMinTimeStamp);
    }

    long min = Long.MAX_VALUE;
    long lm;

    for (int i=0; i<this.columns.size(); i++) {
      lm = columns.elementAt(i).getMinOnset();
      if (lm < min) {
        min = lm;
      }
    }

    this.lastMinTimeStamp = min;
    return (min);
  }

  public long getMaxTimeStamp()
    throws SystemErrorException
  {
    if (!spreadsheetChanged) {
      return (lastMaxTimeStamp);
    }

    long max = Long.MIN_VALUE;
    long lm;

    for (int i=0; i<this.columns.size(); i++) {
      lm = columns.elementAt(i).getMinOnset();
      if (lm > max) {
        max = lm;
      }
    }

    this.lastMaxTimeStamp = max;
    return (max);
  }

  public int getColumnHeight()
    throws SystemErrorException
  {
    long diff = (this.getMaxTimeStamp()-this.getMinTimeStamp())/this.getScale();
    if (diff >= Integer.MAX_VALUE) {
      return (Integer.MAX_VALUE-1);
    }

    return ((int)diff);
  }

  public void updateSpreadsheet()
    throws SystemErrorException
  {
    this.spreadsheetChanged = true;
    this.getMinTimeStamp();
    this.getMaxTimeStamp();
    this.spreadsheetChanged = false;
    this.repaint();
  }

  public int getSpreadsheetView()
  {
    return (this.spreadsheetView);
  }

  public void setSpreadsheetView(int view)
  {
    if ((view>=1) && (view<=3)) {
      this.spreadsheetView = view;
    }
  }

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
  private void initComponents()
  {

    org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(0, 400, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(0, 300, Short.MAX_VALUE)
    );
  }// </editor-fold>//GEN-END:initComponents
  
  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables
  
}
