package org.datavyu.views;

import java.util.HashMap;
import java.util.Map;

/**
 * Maintains the highest frame rate between the tracks and one user defined entry
 *
 * The user can always overwrite the highest frame rate
 */
public class FrameRateController {

    private static final long USER_FRAME_RATE_ID = -1;

    private Map<Long, Float> trackToFrameRate = new HashMap<>();

    private boolean hasUserFrameRate = false;

    private float userFrameRate = 0F;

    public synchronized void addUserFrameRate(float frameRate) {
        hasUserFrameRate = true;
        userFrameRate = frameRate;
    }

    public synchronized void removeUserFrameRate() {
        hasUserFrameRate = false;
        userFrameRate = 0F;
    }

    public synchronized void addFrameRate(long id, float frameRate) {
        trackToFrameRate.put(id, frameRate);
    }

    public synchronized void removeFrameRate(long id) {
        trackToFrameRate.remove(id);
    }

    private float getHighestFrameRate() {
        float highestFrameRate = 0F;
        for (float frameRate : trackToFrameRate.values()) {
            highestFrameRate = Math.max(highestFrameRate, frameRate);
        }
        return highestFrameRate;
    }

    public synchronized float getFrameRate() {
        return hasUserFrameRate ? userFrameRate : getHighestFrameRate();
    }

    public synchronized boolean isZeroRate() {
        return Math.abs(getFrameRate()) <= Math.ulp(1.0);
    }
}
