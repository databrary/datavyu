package org.openshapa.plugins.spectrum.engine;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.nio.ShortBuffer;

import org.gstreamer.Buffer;
import org.gstreamer.Element;

import org.gstreamer.elements.AppSink;
import org.gstreamer.elements.AppSink.NEW_BUFFER;

import org.openshapa.plugins.spectrum.models.StereoAmplitudeData;

import com.sun.jna.Pointer;


/**
 * Processes data in a buffer, picking a fixed amount of high and low points.
 */
public class FixedHiLoBufferProcessor implements NEW_BUFFER {

    /** The sink to pull buffers from. */
    private final AppSink sink;

    /** Number of audio channels in the buffer. */
    private final int mediaChannels;

    /** Buffer to store picked points in. */
    private final StereoAmplitudeData data;

    /** Current time of the index in the buffer. */
    private long cur;

    /** End time of the current interval. */
    private long next;

    /** Timestamp of the previous buffer. */
    private long prevBufTime;

    /** Time interval between processing intervals. */
    private long interval;

    /** Highest value for the current interval. Channel one (left). */
    private double curHighL;

    /** Lowest value for the current interval. Channel one (left). */
    private double curLowL;

    /** Highest value for the current interval. Channel one (right). */
    private double curHighR;

    /** Lowest value for the current interval. Channel one (right). */
    private double curLowR;

    /**
     * Constructs a new buffer processor.
     *
     * @param sink
     *            The sink to pull data from.
     * @param mediaChannels
     *            Number of audio channels.
     * @param data
     *            Result buffer.
     * @param numSamples
     *            Number of samples to extract.
     */
    public FixedHiLoBufferProcessor(final AppSink sink, final int mediaChannels,
        final StereoAmplitudeData data, final int numSamples) {

        if (mediaChannels <= 0) {
            throw new IllegalArgumentException("Expecting mediaChannels > 0");
        }

        if (data == null) {
            throw new IllegalArgumentException("Expecting data != null");
        }

        if (numSamples <= 0) {
            throw new IllegalArgumentException("Expecting numSamples > 0");
        }

        this.sink = sink;
        this.mediaChannels = mediaChannels;
        this.data = data;

        cur = prevBufTime = NANOSECONDS.convert(data.getDataTimeStart(),
                    data.getDataTimeUnit());

        // prevBufTime = 0;

        long length = NANOSECONDS.convert(data.getDataTimeEnd()
                - data.getDataTimeStart(), data.getDataTimeUnit());

        interval = (long) (length / (double) numSamples);
        next = cur + interval;

        curHighL = Double.MIN_VALUE;
        curLowL = Double.MAX_VALUE;

        curHighR = Double.MIN_VALUE;
        curLowR = Double.MAX_VALUE;
    }

    @Override public void newBuffer(final Element elem,
        final Pointer userData) {
        Buffer buf = sink.pullBuffer();

        if (buf == null) {
            return;
        }

        /*
         * Divide by two to convert from byte buffer length
         * to short buffer length.
         */
        int size = buf.getSize() / 2;

        ShortBuffer sb = buf.getByteBuffer().asShortBuffer();

        // System.out.println("New buffer time=" +
        // buf.getTimestamp().toNanos());

        // 1. Calculate the time interval between each magnitude value.
        double bufInterval = ((buf.getTimestamp().toNanos() - prevBufTime)
                / ((double) size / mediaChannels));

        int i = 0;

        while ((i < size) && (cur < next)) {
            double left = sb.get(i);
            double right = 0;

            if (mediaChannels >= 2) {
                right = sb.get(i + 1);
            }

            // 2. Update the high and low values for the current interval.
            if (left > curHighL) {
                curHighL = left;
            }

            if (left < curLowL) {
                curLowL = left;
            }

            if (right > curHighR) {
                curHighR = right;
            }

            if (right < curLowR) {
                curLowR = right;
            }

            // 3. Update cur to be the next data time in the buffer
            cur += bufInterval;

            // 4. Finished with the current interval, update.
            if (cur >= next) {
                // System.out.println("Adding: cur=" + cur + " next=" + next);

                next += interval;

                // 5. Record high and low values for current interval.
                data.addDataL(curHighL);
                data.addDataL(curLowL);

                data.addDataR(curHighR);
                data.addDataR(curLowR);

                // 6. Reset high and low values.
                curHighL = Double.MIN_VALUE;
                curLowL = Double.MAX_VALUE;

                curHighR = Double.MIN_VALUE;
                curLowR = Double.MAX_VALUE;
            }

            // 7. Move index forwards.
            i += mediaChannels;
        }

        prevBufTime = buf.getTimestamp().toNanos();

        // System.out.println("Buffer done");

    }

}