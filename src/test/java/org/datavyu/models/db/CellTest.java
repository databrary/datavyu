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
package org.datavyu.models.db;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static junit.framework.Assert.*;
import org.datavyu.Datavyu;
import org.datavyu.controllers.project.ProjectController;
import org.datavyu.models.project.Project;
import static org.mockito.Mockito.*;

/**
 * Tests for the cell interface
 */
public class CellTest {

    /**
     * The parent datastore for the cell we are testing.
     */
    private DataStore ds;

    /**
     * The parent variable for the cell we are testing.
     */
    private Variable var;

    /**
     * The model we are testing.
     */
    private Cell model;

    /**
     * the modelListener we are testing.
     */
    private CellListener modelListener;

    @BeforeMethod
    public void setUp() throws UserWarningException {
        ds = DataStoreFactory.newDataStore();
        Datavyu.setProjectController(new ProjectController(new Project(), ds));
        var = ds.createVariable("test", Argument.Type.TEXT);
        model = var.createCell();

        modelListener = mock(CellListener.class);
        model.addListener(modelListener);
    }

    @AfterMethod
    public void tearDown() {
        model.removeListener(modelListener);
        modelListener = null;
        model = null;
        var = null;
        ds = null;
    }

    @Test
    public void testIsSelected() {
        assertTrue(model.isSelected());
        model.setSelected(false);
        assertFalse(model.isSelected());

        verify(modelListener).selectionChange(false);
        verify(modelListener, times(0)).highlightingChange(true);
        verify(modelListener, times(0)).offsetChanged(0);
        verify(modelListener, times(0)).onsetChanged(0);
        verify(modelListener, times(0)).valueChange(null);
    }

    @Test
    public void testIsHighlighted() {
        assertTrue(model.isHighlighted());
        model.setHighlighted(false);
        assertFalse(model.isHighlighted());

        verify(modelListener).highlightingChange(false);
        verify(modelListener, times(0)).selectionChange(true);
        verify(modelListener, times(0)).offsetChanged(0);
        verify(modelListener, times(0)).onsetChanged(0);
        verify(modelListener, times(0)).valueChange(null);
    }

    @Test
    public void testSetOffset() {
//        assertEquals(model.getOffset(), 0);
        assertEquals(model.getOffsetString(), "00:00:00:000");

        model.setOffset(10);
//        assertEquals(model.getOffset(), 10);
        assertEquals(model.getOffsetString(), "00:00:00:010");
        verify(modelListener).offsetChanged(10);
        verify(modelListener, times(0)).onsetChanged(10);
        verify(modelListener, times(0)).highlightingChange(true);
        verify(modelListener, times(0)).selectionChange(true);
        verify(modelListener, times(0)).valueChange(null);

        model.setOffset("00:12:01:050");
//        assertEquals(model.getOffset(), 721050);
        assertEquals(model.getOffsetString(), "00:12:01:050");

        model.setOffset(7092113);
//        assertEquals(model.getOffset(), 7092113);
        assertEquals(model.getOffsetString(), "01:58:12:113");
    }

    @Test
    public void testSetOnset() {
//        assertEquals(model.getOnset(), 0);
        assertEquals(model.getOnsetString(), "00:00:00:000");

        model.setOnset(20);
//        assertEquals(model.getOnset(), 20);
        assertEquals(model.getOnsetString(), "00:00:00:020");
        verify(modelListener).onsetChanged(20);
        verify(modelListener, times(0)).offsetChanged(20);
        verify(modelListener, times(0)).highlightingChange(true);
        verify(modelListener, times(0)).selectionChange(true);
        verify(modelListener, times(0)).valueChange(null);

        model.setOnset("00:13:04:890");
//        assertEquals(model.getOnset(), 784890);
        assertEquals(model.getOnsetString(), "00:13:04:890");

        model.setOnset(17999999);
//        assertEquals(model.getOnset(), 17999999);
        assertEquals(model.getOnsetString(), "04:59:59:999");
    }
}
