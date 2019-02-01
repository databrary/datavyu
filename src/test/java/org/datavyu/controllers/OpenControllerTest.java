package org.datavyu.controllers;

import org.apache.logging.log4j.LogManager;
import org.datavyu.models.db.Argument;
import org.datavyu.models.db.Cell;
import org.datavyu.models.db.DataStore;
import org.datavyu.models.db.Variable;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Tests for opening Datavyu project and CSV files.
 */
public class OpenControllerTest {

    // The location of the test files.
    private static final String TEST_FOLDER = System.getProperty("testPath");

    @BeforeClass
    public void spinUp() {
        LogManager.getLogger();
    }

    @AfterClass
    public void spinDown() {
    }

    @Test
    public void testLoadCSV() {
        File demoFile = new File(TEST_FOLDER + "IO/simple1.csv");
        OpenController openc = new OpenController();
        openc.openDataStore(demoFile);

        DataStore ds = openc.getDataStore();
        List<Variable> vars = ds.getAllVariables();
        assertEquals(vars.size(), 1);
        assertEquals(vars.get(0).getName(), "TestColumn");
        assertEquals(vars.get(0).getRootNode().type, Argument.Type.TEXT);
        assertEquals(vars.get(0).isHidden(), false);

        List<Cell> cells = vars.get(0).getCells();
        assertEquals(cells.size(), 1);
        assertEquals(cells.get(0).getOnsetString(), "00:01:00:000");
        assertEquals(cells.get(0).getOffsetString(), "00:02:00:000");
        assertEquals(cells.get(0).getValueAsString(), "This is a test cell.");
    }

    @Test
    public void testLoadOPF() {
        File demoFile = new File(TEST_FOLDER + "IO/simple2.opf");
        OpenController openc = new OpenController();
        openc.openProject(demoFile);

        DataStore ds = openc.getDataStore();
        List<Variable> vars = ds.getAllVariables();
        assertEquals(vars.size(), 1);
        assertEquals(vars.get(0).getName(), "TestColumn");
        assertEquals(vars.get(0).getRootNode().type, Argument.Type.TEXT);
        assertEquals(vars.get(0).isHidden(), false);

        List<Cell> cells = vars.get(0).getCells();
        assertEquals(cells.size(), 1);
        assertEquals(cells.get(0).getOnsetString(), "00:01:00:000");
        assertEquals(cells.get(0).getOffsetString(), "00:02:00:000");
        assertEquals(cells.get(0).getValueAsString(), "This is a test cell.");
    }

    @Test
    public void testLoadOPF2() {
        File demoFile = new File(TEST_FOLDER + "IO/simple3.opf");
        OpenController openc = new OpenController();
        openc.openProject(demoFile);

        DataStore ds = openc.getDataStore();
        List<Variable> vars = ds.getAllVariables();
        assertEquals(vars.size(), 4);
        
        // Sort the columns by name to match up the order with what we expect.
        vars.sort((v1, v2) -> (v1.getName().compareTo(v2.getName())));
        
        Variable var = vars.get(0);
        assertEquals(var.getName(), "hiddenColumn");
        assertEquals(var.getRootNode().type, Argument.Type.TEXT);
        assertEquals(var.isHidden(), true);
        List<Cell> cells = var.getCells();
        assertEquals(cells.size(), 0);
        
        var = vars.get(1);
        assertEquals(var.getName(), "testColumn");
        assertEquals(var.getRootNode().type, Argument.Type.TEXT);
        assertEquals(var.isHidden(), false);
        cells = var.getCells();
        assertEquals(cells.size(), 1);
        assertEquals(cells.get(0).getValueAsString(), "cellA");

        var = vars.get(2);
        assertEquals(var.getName(), "testColumn2");
        assertEquals(var.getRootNode().type, Argument.Type.NOMINAL);
        assertEquals(var.isHidden(), false);
        cells = var.getCells();
        assertEquals(cells.size(), 1);
        assertEquals(cells.get(0).getValueAsString(), "cellB");

        var = vars.get(3);
        assertEquals(var.getName(), "testColumn3");
        assertEquals(var.getRootNode().type, Argument.Type.MATRIX);
        assertEquals(var.isHidden(), false);
        cells = var.getCells();
        assertEquals(cells.size(), 1);
        assertEquals(cells.get(0).getValueAsString(), "(cellC)");
    }

    @Test
    public void testLoadOPF3() {
        File demoFile = new File(TEST_FOLDER + "IO/simple4.opf");
        OpenController openc = new OpenController();
        openc.openProject(demoFile);

        DataStore ds = openc.getDataStore();
        List<Variable> vars = ds.getAllVariables();
        assertEquals(vars.size(), 1);
        assertEquals(vars.get(0).getName(), "blah");

        List<Cell> cells = vars.get(0).getCells();
        assertEquals(cells.size(), 1);
    }
}
