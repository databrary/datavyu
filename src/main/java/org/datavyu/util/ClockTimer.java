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
package org.datavyu.util;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Keeps multiple streams in periodic sync and does not play beyond the boundaries of a stream.
 */
public final class ClockTimer {

    /** Synchronization threshold in milliseconds */
    public static final long SYNC_THRESHOLD = 1500L; // 1.5 sec  (because some plugins are not very precise in seek)

    /** Clock tick period in milliseconds */
    private static final long CLOCK_SYNC_INTERVAL = 1500L;

    /** Clock initial delay in milliseconds */
    private static final long CLOCK_SYNC_DELAY = 0L;

    /** Convert nanoseconds to milliseconds */
    private static final long NANO_IN_MILLI = 1000000L;

    private static final long CHECK_BOUNDARY_INTERVAL = 100L; // milliseconds

    private static final long CHECK_BOUNDARY_DELAY = 0L;

    private static final long SEEK_PLAYBACK_INTERVAL = 31L; // milliseconds

    private static final long SEEK_PLAYBACK_DELAY = 0L;

    /** Minimum time for the clock in milliseconds */
    private long minTime;

    /** Maximum time for the clock in milliseconds */
    private long maxTime;

    /** Current time of the clock in milliseconds */
    private double clockTime;

    /** Last time in nanoseconds; it is used to calculate the elapsed */
    private double lastTime;

    /** Is the clock stopped */
    private boolean isStopped;

    /** The rate factor for the clock updates */
    private float rate = 1F;

    /** Listeners of this clock */
    private Set<ClockListener> clockListeners = new HashSet<>();

    /**
     * Default constructor.
     */
    public ClockTimer() {

        // Initialize values
        clockTime = 0;
        lastTime = 0;
        minTime = 0;
        maxTime = 0;
        isStopped = true;

        // Sync timer at lower frequency
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                periodicSync();
            }
        }, CLOCK_SYNC_DELAY, CLOCK_SYNC_INTERVAL);

        // Boundary checker at higher frequency
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkBoundary();
            }
        }, CHECK_BOUNDARY_DELAY, CHECK_BOUNDARY_INTERVAL);

        // Seek playback for fast, backward playback
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                notifySeekPlayback();
            }
        }, SEEK_PLAYBACK_DELAY, SEEK_PLAYBACK_INTERVAL);
    }

    /**
     * Sets the minimum stream time
     *
     * @param minTime The minimum stream time
     */
    public synchronized void setMinTime(long minTime) {
        this.minTime = minTime;
    }

    /**
     * Sets the maximum stream time
     *
     * @param maxTime The maximum stream time
     */
    public synchronized void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }

    /**
     * Get the clock time into the range for this clock timer
     *
     * @param clockTime Clock time
     *
     * @return Clock time in range for this clock timer
     */
    public synchronized long toRange(long clockTime) {
        return Math.min(Math.max(clockTime, minTime), maxTime);
    }


    /**
     * Get the current stream time
     *
     * @return Current stream time
     */
    public synchronized double getStreamTime() {
        return (long) clockTime + minTime;
    }

    /**
     * @return Current clock rate.
     */
    public synchronized float getRate() {
        return rate;
    }

    /**
     * Set the time but don't activate any of the listeners
     *
     * All listeners will be updated through the auto sync to the new time
     *
     * Use this method if eventual synchronization is enough
     *
     * @param time The new time
     */
    public synchronized void setTime(long time) {
        if (minTime <= time && time <= maxTime) {
            clockTime = time;
            // Don't notify a sync or force a sync
            // The time will be updated by a periodic sync
        }
    }

    /**
     * Set the time and force an update of the clock time to all listeners
     *
     * Use this method if immediate synchronization is desired
     *
     * @param time The new time
     */
    public synchronized void setForceTime(long time) {
        if (minTime <= time && time <= maxTime) {
            clockTime = time;
            // Notify a force sync
            notifyForceSync();
        }
    }

    /**
     * Toggles between start/stop
     */
    public synchronized void toggle() {
        if (isStopped) {
            start();
        } else {
            stop();
        }
    }

    /**
     * Sets the update rate for the clock
     *
     * @param newRate New update rate
     */
    public synchronized void setRate(float newRate) {
        updateElapsedTime();
        rate = newRate;
        // FIRST notify about the rate change
        notifyRate();
        // SECOND start or stop
        if (Math.abs(rate) < Math.ulp(1f)) {
            stop();
        } else {
            start();
        }
    }

    /**
     * If the clock is not running, then this starts the clock and fires a notify
     * start event with the current clock time
     */
    public synchronized void start() {
        if (isStopped) {
            isStopped = false;
            lastTime = System.nanoTime();
            notifyStart();
        }
    }

    /**
     * If the clock is not stopped, then this stops the clock and fires a notify
     * stop event with the current clock time
     */
    public synchronized void stop() {
        if (!isStopped) {
            updateElapsedTime();
            isStopped = true;
            notifyStop();
        }
    }

    /**
     * @return True if clock is stopped.
     */
    public synchronized boolean isStopped() {
        return isStopped;
    }

    /**
     * Registers a clock listener
     *
     * @param listener Listener requiring clockTick updates
     */
    public synchronized void registerListener(final ClockListener listener) {
        clockListeners.add(listener);
    }

    /**
     * Update the clock time with the elapsed time since the last update
     */
    private synchronized void updateElapsedTime() {
        double newTime = System.nanoTime();
        clockTime = isStopped ? clockTime : Math.min(Math.max(
                clockTime + rate * (newTime - lastTime) / NANO_IN_MILLI, minTime), maxTime);
        lastTime = newTime;
    }

    /**
     * The "periodicSync" of the clock - updates listeners of changes in time.
     */
    private synchronized void periodicSync() {
        updateElapsedTime();
        notifyPeriodicSync();
    }

    private synchronized void checkBoundary() {
        updateElapsedTime();
        notifyCheckBoundary();
    }

    private void notifySeekPlayback() {
        // TODO: Remove this once plugin's support fast playback forward / backward playback
        // Only activate the seek playback for rates < 0 or > 2x
        //if (rate > 2F || rate < 0F) { }
        for (ClockListener clockListener : clockListeners) {
            clockListener.clockSeekPlayback(clockTime);
        }
    }

    private void notifyCheckBoundary() {
        for (ClockListener clockListener : clockListeners) {
            clockListener.clockBoundaryCheck(clockTime);
        }
    }

    /**
     * Notify clock listeners of a force periodicSync -- consumers must act on this
     */
    private void notifyForceSync() {
        for (ClockListener clockListener : clockListeners) {
            clockListener.clockForceSync(clockTime);
        }
    }

    /**
     * Notify clock listeners of a periodic periodicSync -- consumers may act on this
     */
    private void notifyPeriodicSync() {
        for (ClockListener clockListener : clockListeners) {
            clockListener.clockPeriodicSync(clockTime);
        }
    }

    /**
     * Notify clock listeners of rate update.
     */
    private void notifyRate() {
        for (ClockListener clockListener : clockListeners) {
            clockListener.clockRate(rate);
        }
    }

    /**
     * Notify clock listeners of start event.
     */
    private void notifyStart() {
        for (ClockListener clockListener : clockListeners) {
            clockListener.clockStart(clockTime);
        }
    }

    /**
     * Notify clock listeners of stop event.
     */
    private void notifyStop() {
        for (ClockListener clockListener : clockListeners) {
            clockListener.clockStop(clockTime);
        }
    }

    /**
     * Listener interface for clock 'ticks'.
     */
    public interface ClockListener {
        /**
         * @param clockTime Current time in milliseconds
         */
        void clockSeekPlayback(double clockTime);

        /**
         * @param clockTime Current time in milliseconds
         */
        void clockBoundaryCheck(double clockTime);

        /**
         * @param clockTime Current time in milliseconds
         */
        void clockForceSync(double clockTime);

        /**
         * @param clockTime Current time in milliseconds
         */
        void clockPeriodicSync(double clockTime);

        /**
         * @param clockTime Current time in milliseconds
         */
        void clockStart(double clockTime);

        /**
         * @param clockTime Current time in milliseconds
         */
        void clockStop(double clockTime);

        /**
         * @param rate Current (updated) rate.
         */
        void clockRate(float rate);
    }
}
