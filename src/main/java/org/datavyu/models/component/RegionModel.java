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

public interface RegionModel extends MixerComponentModel {
	/**
	 * Returns the current start back region.
	 * 
	 * @return current start back region
	 */
	RegionState getRegion();
	
	/**
	 * Resets the start back region to the entire data track.
	 */
	void resetPlaybackRegion();
	
	/**
	 * Sets the start back region.
	 * 
	 * @param regionStart start time of the region in milliseconds, inclusive. Must be <= regionEnd.
	 * @param regionEnd end time of the region in milliseconds, inclusive. Must be >= regionStart.
	 */
    void setPlaybackRegion(final long regionStart, final long regionEnd);

    /**
     * Moves the start marker for the start back region, while keeping the end marker in the same position.
     * 
     * @param regionStart new start time of the region in milliseconds, inclusive. Must be <= {@link RegionState#getRegionEnd()}.
     */
    void setPlaybackRegionStart(final long regionStart);

    /**
     * Moves the end marker for the start back region, while keeping the start marker in the same position.
     * 
     * @param regionEnd new end time of the region in milliseconds, inclusive. Must be >= {@link RegionState#getRegionStart()}.
     */
    void setPlaybackRegionEnd(final long regionEnd);
}
