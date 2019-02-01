package org.datavyu.models.db;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import org.datavyu.Datavyu;
import org.datavyu.controllers.project.ProjectController;
import org.datavyu.models.project.Project;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Tests for the Variable Interface
 */
public class VariableTest {
    /**
     * The parent datastore for the variable.
     */
    private DataStore ds;

    /**
     * The model we are testing.
     */
    private Variable model;

    /**
     * the modelListener we are testing.
     */
    private VariableListener modelListener;

    @BeforeMethod
    public void setUp() throws UserWarningException {
        ds = DataStoreFactory.newDataStore();
        Datavyu.setProjectController(new ProjectController(new Project(), ds));        
        model = ds.createVariable("test", Argument.Type.TEXT);
        modelListener = mock(VariableListener.class);
        model.addListener(modelListener);
        ds.markAsUnchanged();
    }

    @AfterMethod
    public void tearDown() {
        model.removeListener(modelListener);
        modelListener = null;
        model = null;
    }

    @Test
    public void testSetName() throws UserWarningException {
        assertEquals(model.getName(), "test");
        assertFalse(ds.isChanged());
        model.setName("test2");
        assertTrue(ds.isChanged());
        assertEquals(model.getName(), "test2");
        assertEquals(ds.getVariable("test"), null);
        assertEquals(ds.getVariable("test2"), model);
        verify(modelListener).nameChanged("test2");
        verify(modelListener, times(0)).visibilityChanged(true);
        verify(modelListener, times(0)).cellInserted(null);
        verify(modelListener, times(0)).cellRemoved(null);
    }

    @Test
    public void testIsHidden() {
        assertFalse(model.isHidden());
        assertFalse(ds.isChanged());
        model.setHidden(true);
        assertTrue(ds.isChanged());
        assertTrue(model.isHidden());
        verify(modelListener).visibilityChanged(true);
        verify(modelListener, times(0)).nameChanged(null);
        verify(modelListener, times(0)).cellInserted(null);
        verify(modelListener, times(0)).cellRemoved(null);
    }

    @Test
    public void testIsSelected() {
        List<Variable> vars = new ArrayList<>();
        vars.add(model);
        assertTrue(model.isSelected());
        assertEquals(ds.getSelectedVariables(), vars);
        assertFalse(ds.isChanged());
        model.setSelected(false);
        assertFalse(model.isSelected());
        verify(modelListener, times(0)).visibilityChanged(true);
        verify(modelListener, times(0)).nameChanged(null);
        verify(modelListener, times(0)).cellInserted(null);
        verify(modelListener, times(0)).cellRemoved(null);
    }

    @Test
    public void testGetVariableType() {
        assertEquals(model.getRootNode().type, Argument.Type.TEXT);
    }

    @Test
    public void testCreateCell() {
        List<Cell> cells = new ArrayList<>();
        assertFalse(ds.isChanged());
        Cell c = model.createCell();
        assertTrue(ds.isChanged());
        cells.add(c);
        assertTrue(model.contains(c));
        assertEquals(ds.getVariable(c), model);
        assertEquals(model.getCells(), cells);
        verify(modelListener).cellInserted(c);
        verify(modelListener, times(0)).visibilityChanged(true);
        verify(modelListener, times(0)).nameChanged(null);
        verify(modelListener, times(0)).cellRemoved(null);
    }

    @Test
    public void testRemoveCell() {
        Cell c = model.createCell();
        ds.markAsUnchanged();
        assertFalse(ds.isChanged());
        ds.removeCell(c);
        assertTrue(ds.isChanged());

        assertFalse(model.contains(c));
        assertEquals(model.getCells().size(), 0);
        assertEquals(model.getCellsTemporally().size(), 0);

        verify(modelListener).cellRemoved(c);
        verify(modelListener).cellInserted(c);
        verify(modelListener, times(0)).nameChanged(null);
        verify(modelListener, times(0)).visibilityChanged(true);
    }

    @Test
    public void testRemoveCell2() {
        Cell c = model.createCell();
        ds.markAsUnchanged();
        assertFalse(ds.isChanged());
        model.removeCell(c);
        assertTrue(ds.isChanged());

        assertFalse(model.contains(c));
        assertEquals(model.getCells().size(), 0);
        assertEquals(model.getCellsTemporally().size(), 0);

        verify(modelListener).cellRemoved(c);
        verify(modelListener).cellInserted(c);
        verify(modelListener, times(0)).nameChanged(null);
        verify(modelListener, times(0)).visibilityChanged(true);
    }

    @Test
    public void testTemporalOrder() {
        List<Cell> cells = new ArrayList<>();
        List<Cell> orderedCells = new ArrayList<>();
        Cell c1 = model.createCell();
        Cell c2 = model.createCell();

        cells.add(c1);
        cells.add(c2);

        orderedCells.add(c2);
        orderedCells.add(c1);

        c1.setOnset(100);

        assertEquals(model.getCellsTemporally(), orderedCells);
        assertEquals(model.getCellTemporally(0), c2);
    }

    @Test(expectedExceptions = UserWarningException.class)
    public void uniqueVariableNames() throws UserWarningException {
        ds.createVariable("test", Argument.Type.TEXT);
    }

    @Test(expectedExceptions = UserWarningException.class)
    public void nameWithCharacters() throws UserWarningException {
        ds.createVariable("", Argument.Type.TEXT);
    }

    @Test(expectedExceptions = UserWarningException.class)
    public void badCharacterSpace() throws UserWarningException {
        ds.createVariable(" blah ", Argument.Type.TEXT);
    }


    @Test(expectedExceptions = UserWarningException.class)
    public void badCharacter1() throws UserWarningException {
        ds.createVariable("as)ds", Argument.Type.TEXT);
    }

    @Test(expectedExceptions = UserWarningException.class)
    public void badCharacter2() throws UserWarningException {
        ds.createVariable("ac(dc", Argument.Type.TEXT);
    }

    @Test(expectedExceptions = UserWarningException.class)
    public void badCharacter3() throws UserWarningException {
        ds.createVariable("ac>dc", Argument.Type.TEXT);
    }

    @Test(expectedExceptions = UserWarningException.class)
    public void badCharacter4() throws UserWarningException {
        ds.createVariable("ac<dc", Argument.Type.TEXT);
    }

    @Test(expectedExceptions = UserWarningException.class)
    public void badCharacter5() throws UserWarningException {
        ds.createVariable("ac,dc", Argument.Type.TEXT);
    }

    @Test(expectedExceptions = UserWarningException.class)
    public void badCharacter6() throws UserWarningException {
        ds.createVariable("ac\"dc", Argument.Type.TEXT);
    }

    @Test(expectedExceptions = UserWarningException.class)
    public void startsWithNumber() throws UserWarningException {
        ds.createVariable("1acdc", Argument.Type.TEXT);
    }
}
