package org.datavyu.views;

import com.google.common.collect.Lists;

import java.util.*;

/**
 * Class to control rates.
 */
final class RateController {

    private SortedSet<Float> rates;

    /**
     * Rate controller for the rates.
     *
     * @param rates The rates for this controller.
     */
    private RateController(float[] rates) {
        this.rates = new TreeSet<>();
        for (float rate : rates) {
            this.rates.add(rate);
        }
    }

    /**
     * Default rate controller with rates:
     * -32, -16, -8, -4, -2, -1, -1/2, -1/4, -1/8, -1/16, -1/32, 0, 1/32, 1/16, 1/8, 1/4, 1/2, 1, 2, 4, 8, 16, 32
     */
    RateController() {
        this(new float[]{-32F, -16F, -8F, -4F, -2F, -1F, -1/2F, -1/4F, -1/8F, -1/16F, -1/32F,
                           0F, 1/32F, 1/16F, 1/8F, 1/4F, 1/2F, 1F, 2F, 4F, 8F, 16F, 32F});
    }

    /**
     * Get the next larger rate. If this is the largest rate, it returns this largest rate.
     *
     * @param rate The current rate.
     *
     * @return Next larger rate.
     */
    private List<Float> largerRates(float rate) {
        SortedSet<Float> tailSet = rates.tailSet(rate);
        List<Float> larger = new ArrayList<>(tailSet);
        larger.remove(0);
        return larger;
    }

    /**
     * Get the next smaller rate. If this is the smallest rate, it returns this smallest rate.
     *
     * @param rate The current rate.
     *
     * @return Next smaller rate.
     */
    private List<Float> smallerRates(float rate) {
        return Lists.reverse(new ArrayList<>(rates.headSet(rate)));
    }

    /**
     * Get the next rate for the current rate.
     * @param currentRate The current rate.
     * @param jump The number of jumps to the next currentRate.
     *
     * @return Next rate for the number of signed jumps taking the current rate as a reference.
     */
    float nextRate(float currentRate, int jump) {
        List<Float> rates = jump < 0 ? smallerRates(currentRate) : largerRates(currentRate);
        float nextRate = currentRate;
        Iterator<Float> rate = rates.iterator();
        for (int jumps = 0; rate.hasNext() && jumps < Math.abs(jump); ++jumps) {
            nextRate = rate.next();
        }
        return nextRate;
    }
}
