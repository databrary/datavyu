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
 * Defines constraints of the tracks view. The properties of this object are immutable
 * <p>
 * For thread safety, any code that works with viewports should retrieve a reference to a <code>Viewport</code> object
 * <b>once</b> and make all subsequent calls for the current task through the <b>same</b> object, e.g.:
 * <p>
 * <code>
 *     final ViewportState viewport = controller.getViewport();
 *     final long viewStart = viewport.getViewStart();
 *     final long viewEnd = viewport.getViewEnd();
 *     final long timestamp = ...
 *     final boolean isTimeInViewport = viewStart <= timestamp && timestamp <= viewEnd; 
 * </code>
 * <p>
 * Viewports may change on another thread, therefore you should <b>NOT</b> write code in the following way:
 * <p>
 * <code>
 *     final long viewStart = controller.getViewport().getViewStart(); // THIS IS WRONG!!!
 *     final long viewEnd = controller.getViewport().getViewEnd();     // THIS IS WRONG!!!
 *     final long timestamp = ...
 *     final boolean isTimeInViewport = viewStart <= timestamp && timestamp <= viewEnd(); 
 * </code>
 */
public interface ViewportState {

    String NAME = ViewportState.class.getName();

    /**
     * @return Maximum valid timestamp in milliseconds. Inclusive.
     */
    long getMaxEnd();

    /**
     * @return View window start in milliseconds. Inclusive.
     */
    long getViewStart();

    /**
     * @return View window end in milliseconds. Inclusive. This may be greater
     * than {@link #getMaxEnd()}.
     */
    long getViewEnd();

    /**
     * @return Pixel width of the viewport.
     */
    double getViewWidth();

    /**
     * @return Viewport duration in milliseconds. Always >= 1.
     */
    long getViewDuration();

    /**
     * @return Milliseconds per pixel resolution. Returns {@link Double#NaN}
     * if {@link ViewportState#getViewWidth()} is zero.
     */
    double getResolution();

    /**
     * @return Zoom level in between 0 (zoomed all the way out) and 1
     *  (zoomed all the way in) inclusive.
     */
    double getZoomLevel();

    /**
     * Computes the pixel x-coordinate offset for a given time.
     *
     * @param time Time in milliseconds.
     * @return the pixel x-coordinate offset for a given time, where 0 corresponds to {@link #getViewStart()}
     */
    double computePixelXOffset(long time);

    boolean isTimeInViewport(long time);

    /**
     * Computes a time from the pixel x-coordinate offset. Not absolute time.
     *
     * @param offset
     * @return 0 based offset from {@link #getViewStart()}
     * @throw {@link IllegalArgumentException} if offset is not in the viewport
     */
    long computeTimeFromXOffset(double offset);
}
