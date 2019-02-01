package org.datavyu.models.db;

import org.testng.annotations.*;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Tests for the DataStore interface.
 */
public class DataStoreTest {

    /**
     * The model we are testing.
     */
    private DataStore model;

    /**
     * The modelListener we are testing.
     */
    private DataStoreListener modelListener;

    /**
     * The title notifier we are testing.
     */
    private TitleNotifier titleListener;

    @BeforeClass
    public void spinUp() {
    }

    @AfterClass
    public void spinDown() {
    }

    @BeforeMethod
    public void setUp() {
        model = DataStoreFactory.newDataStore();
        modelListener = mock(DataStoreListener.class);
        titleListener = mock(TitleNotifier.class);
        model.addListener(modelListener);
        model.setTitleNotifier(titleListener);
    }

    @AfterMethod
    public void tearDown() {
        model.removeListener(modelListener);
        modelListener = null;
        titleListener = null;
        model = null;
    }

    @Test
    public void testSetName() {
        model.setName("testName");
        assertEquals(model.getName(), "testName");
    }

    @Test
    public void createVariable() throws UserWarningException {
        assertFalse(model.isChanged());
        model.createVariable("foo", Argument.Type.TEXT);
        assertTrue(model.isChanged());
        Variable var = model.getVariable("foo");
        List<Variable> varList = new ArrayList<>();
        varList.add(var);

        assertNotNull(var);

        // TODO: database should be changed after adding a new variable.
        // assertTrue(model.isChanged());

        assertEquals(model.getSelectedVariables(), varList);
        assertEquals(model.getAllVariables(), varList);
        assertEquals(var.getName(), "foo");
        assertEquals(var.getRootNode().type, Argument.Type.TEXT);
        assertTrue(var.isSelected());
        assertTrue(!var.isHidden());
        assertEquals(var.getCells().size(), 0);
        verify(modelListener).variableAdded(var);
        verify(modelListener, times(0)).variableOrderChanged();
        verify(modelListener, times(0)).variableRemoved(var);
        verify(modelListener, times(0)).variableHidden(var);
        verify(modelListener, times(0)).variableNameChange(var);
        verify(modelListener, times(0)).variableVisible(var);
        verify(titleListener).updateTitle();
    }

    @Test(expectedExceptions = UserWarningException.class)
    public void unableToCreateVariable() throws UserWarningException {
        model.createVariable("foo", Argument.Type.TEXT);
        model.createVariable("foo", Argument.Type.TEXT);
    }

    @Test
    public void removeVariable() throws UserWarningException {
        Variable var = model.createVariable("foo", Argument.Type.TEXT);
        List<Variable> varList = new ArrayList<>();
        varList.add(var);

        assertEquals(model.getAllVariables(), varList);
        verify(modelListener).variableAdded(var);

        model.markAsUnchanged();
        assertFalse(model.isChanged());
        model.removeVariable(var);
        assertTrue(model.isChanged());

        assertEquals(model.getAllVariables().size(), 0);
        assertEquals(model.getSelectedVariables().size(), 0);

        verify(modelListener).variableRemoved(var);
        verify(modelListener, times(0)).variableHidden(var);
        verify(modelListener, times(0)).variableVisible(var);
        verify(modelListener, times(0)).variableNameChange(var);
        verify(modelListener, times(0)).variableOrderChanged();
    }

    @Test
    public void unchangedByDefault() throws UserWarningException {
        assertFalse(model.isChanged());
        model.createVariable("test", Argument.Type.TEXT);
        assertTrue(model.isChanged());
        model.markAsUnchanged();
        assertFalse(model.isChanged());
    }
}
