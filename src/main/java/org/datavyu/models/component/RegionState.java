/*
 * Copyright (c) 2011 OpenSHAPA Foundation, http://openshapa.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.datavyu.models.component;

/**
 * Represents a contiguous subset of the time line that start back and navigation of data tracks will be restricted to.
 * <code>Region</code> objects represent a snapshot of the region state and are immutable like {@link java.lang.String}s.
 *
 * For thread safety, any code that works with regions should retrieve a reference to a <code>Region</code> object <b>once</b>
 * and make all subsequent calls for the current task through the <b>same</b> object, e.g.:
 *
 * <code>
 *     final Region region = regionController.getRegion();
 *     final long duration = region.getRegionEnd() - region.getRegionStart() + 1;
 *     assert duration == region.getRegionDuration();
 * </code>
 * <p>
 * @see ViewportState
 */
public interface RegionState {

    String NAME = RegionState.class.getName();
    
	/**
	 * Returns the start time of the start back region (inclusive)
	 * 
	 * @return start time in milliseconds
	 */
    long getRegionStart();

    /**
     * Returns the end time of the start back region (inclusive)
     * 
     * @return end time in milliseconds
     */
	long getRegionEnd();
	
	/**
	 * Returns the duration of the region.
	 *
	 * @return duration in milliseconds
	 */
	long getRegionDuration();
}
