package org.datavyu.models.db;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Tests for the TextCellValue Interface
 */
public class TextCellValueTest {

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
        var = ds.createVariable("test", Argument.Type.TEXT);
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
    public void testClear() {
        assertTrue(model.isEmpty());

        model.set("test");
        assertFalse(model.isEmpty());

        model.clear();
        assertTrue(model.isEmpty());
    }


}
