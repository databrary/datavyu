package org.datavyu.models.db;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Tests for the TextCellValue Interface
 */
public class MatrixCellValueTest {

    /**
     * The parent DataStore for the TextCellValue we are testing.
     */
    private DataStore ds;

    /**
     * The parent variable for the TextCellValue we are testing.
     */
    private Variable var;

    /**
     * The parent cell for the TextCellValue we are testing.
     */
    private Cell cell;

    /**
     * The value that we are testing.
     */
    private CellValue model;

    @BeforeMethod
    public void setUp() throws UserWarningException {
        ds = DataStoreFactory.newDataStore();
        var = ds.createVariable("test", Argument.Type.MATRIX);
        cell = var.createCell();
        model = cell.getCellValue();
    }

    @AfterMethod
    public void tearDown() {
        model = null;
        cell = null;
        var = null;
        ds = null;
    }

    @Test
    public void testAddArgument() {
        assertEquals(var.getRootNode().childArguments.size(), 1);
        assertEquals(var.getRootNode().childArguments.get(0).name, "code01");
        assertEquals(((MatrixCellValue) cell.getCellValue()).getArguments().size(), 1);

        var.addArgument(Argument.Type.NOMINAL);

        assertEquals(var.getRootNode().childArguments.size(), 2);
        assertEquals(var.getRootNode().childArguments.get(0).name, "code01");
        assertEquals(var.getRootNode().childArguments.get(1).name, "code02");
        assertEquals(((MatrixCellValue) cell.getCellValue()).getArguments().size(), 2);
    }

    @Test
    public void testMoveArgument() {
        var.addArgument(Argument.Type.NOMINAL);
        var.addArgument(Argument.Type.NOMINAL);

        cell.setMatrixValue(0, "foo1");
        cell.setMatrixValue(1, "foo2");
        cell.setMatrixValue(2, "foo3");

        assertEquals(var.getRootNode().childArguments.size(), 3);
        assertEquals(var.getRootNode().childArguments.get(0).name, "code01");
        assertEquals(var.getRootNode().childArguments.get(1).name, "code02");
        assertEquals(var.getRootNode().childArguments.get(2).name, "code03");
        assertEquals(cell.getMatrixValue(0).toString(), "foo1");
        assertEquals(cell.getMatrixValue(1).toString(), "foo2");
        assertEquals(cell.getMatrixValue(2).toString(), "foo3");
        assertEquals(((MatrixCellValue) cell.getCellValue()).getArguments().size(), 3);

        var.moveArgument("code01", 1);

        assertEquals(var.getRootNode().childArguments.size(), 3);
        assertEquals(var.getRootNode().childArguments.get(0).name, "code02");
        assertEquals(var.getRootNode().childArguments.get(1).name, "code01");
        assertEquals(var.getRootNode().childArguments.get(2).name, "code03");
        assertEquals(cell.getMatrixValue(0).toString(), "foo2");
        assertEquals(cell.getMatrixValue(1).toString(), "foo1");
        assertEquals(cell.getMatrixValue(2).toString(), "foo3");
        assertEquals(((MatrixCellValue) cell.getCellValue()).getArguments().size(), 3);

        var.moveArgument("code03", 1);

        assertEquals(var.getRootNode().childArguments.size(), 3);
        assertEquals(var.getRootNode().childArguments.get(0).name, "code02");
        assertEquals(var.getRootNode().childArguments.get(1).name, "code03");
        assertEquals(var.getRootNode().childArguments.get(2).name, "code01");
        assertEquals(cell.getMatrixValue(0).toString(), "foo2");
        assertEquals(cell.getMatrixValue(1).toString(), "foo3");
        assertEquals(cell.getMatrixValue(2).toString(), "foo1");
        assertEquals(((MatrixCellValue) cell.getCellValue()).getArguments().size(), 3);
    }

    @Test
    public void testSetArgument() {

        cell.setMatrixValue(0, "foo");
        assertEquals(cell.getMatrixValue(0).toString(), "foo");
    }

    @Test
    public void testRemoveArgument() {
        assertEquals(var.getRootNode().childArguments.size(), 1);
        assertEquals(var.getRootNode().childArguments.get(0).name, "code01");
        assertEquals(((MatrixCellValue) cell.getCellValue()).getArguments().size(), 1);

        var.addArgument(Argument.Type.NOMINAL);

        assertEquals(var.getRootNode().childArguments.size(), 2);
        assertEquals(var.getRootNode().childArguments.get(0).name, "code01");
        assertEquals(var.getRootNode().childArguments.get(1).name, "code02");
        assertEquals(((MatrixCellValue) cell.getCellValue()).getArguments().size(), 2);

        var.removeArgument("code01");
        assertEquals(var.getRootNode().childArguments.size(), 1);
        assertEquals(var.getRootNode().childArguments.get(0).name, "code02");
        assertEquals(((MatrixCellValue) cell.getCellValue()).getArguments().size(), 1);

    }

    @Test
    public void testClearArgument() {
        assertTrue(cell.getMatrixValue(0).isEmpty());

        cell.setMatrixValue(0, "foo");
        assertFalse(cell.getMatrixValue(0).isEmpty());

        cell.getMatrixValue(0).clear();
        assertTrue(cell.getMatrixValue(0).isEmpty());
    }


}
