package org.openshapa.plugins.spectrum.engine;


import java.io.File;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.gstreamer.Bin;
import org.gstreamer.Bus;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.GhostPad;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;
import org.gstreamer.Structure;

import org.gstreamer.elements.DecodeBin;
import org.gstreamer.elements.PlayBin;

import org.openshapa.plugins.spectrum.SpectrumConstants;
import org.openshapa.plugins.spectrum.events.TimestampListener;
import org.openshapa.plugins.spectrum.swing.Spectrum;
import org.openshapa.plugins.spectrum.swing.SpectrumDialog;

import com.usermetrix.jclient.Logger;
import com.usermetrix.jclient.UserMetrix;

import static org.gstreamer.Element.linkMany;


/**
 * Audio playback engine.
 */
public final class PlaybackEngine extends Thread implements TimestampListener {

    private static final Logger LOGGER = UserMetrix.getLogger(
            PlaybackEngine.class);

    /** Number of microseconds in one millisecond. */
    private static final long MILLISECOND = 1000;

    /** Frame seeking tolerance. */
    private static final long TOLERANCE = 5 * MILLISECOND;

    /** Current engine state. */
    private volatile EngineState engineState;

    /** Current time being played. */
    private long currentTime;

    /** Seek time. */
    private long newTime;

    /** Queue for engine commands. */
    private volatile BlockingQueue<EngineState> commandQueue;

    /** Audio file being handled. */
    private File audioFile;


    /** Dialog for showing the spectral data. */
    private SpectrumDialog dialog;

    /** Audio playback speed. */
    private double playbackSpeed;

    /** The pre-calculated audio FPS. */
    private double audioFPS;

    /** Output pipeline. */
    private Pipeline pipeline;

    /**
     * Creates a new engine thread.
     *
     * @param audioFile
     *            The audio file to handle.
     * @param dialog
     *            The dialog used to display the spectrum.
     */
    public PlaybackEngine(final File audioFile, final SpectrumDialog dialog) {
        this.audioFile = audioFile;
        commandQueue = new LinkedBlockingQueue<EngineState>();
        commandQueue.add(EngineState.INITIALIZING);

        setDaemon(true);
        setName("AudioEngine-" + getName());

        this.dialog = dialog;
    }

    /**
     * Main engine thread.
     *
     * @see java.lang.Thread#run()
     */
    @Override public void run() {

        while (true) {
            System.out.println("Command queue: " + commandQueue);

            try {

                engineState = commandQueue.take();

                switch (engineState) {

                case INITIALIZING:
                    engineInitializing();

                    break;

                case ADJUSTING_SPEED:
                    engineAdjusting();

                    break;

                case SETTING_FPS:
                    engineSettingFPS();

                    break;

                case SEEKING:

                    // Just want to seek to the latest time.
                    while (commandQueue.peek() == EngineState.SEEKING) {
                        commandQueue.take();
                    }

                    engineSeeking();

                    break;

                case PLAYING:

                    /*
                     * Don't want start-stop-start-stop playback because
                     * processing loop is interrupted.
                     */
                    while (commandQueue.peek() == EngineState.PLAYING) {
                        commandQueue.take();
                    }

                    enginePlaying();

                    break;

                case STOPPING:
                    engineStop();

                    break;

                default:
                    break;
                }
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Initialize the playback engine.
     */
    private void engineInitializing() {

        // Set up Gstreamer.
        setupGst();

        engineState = EngineState.TASK_COMPLETE;
    }

    /**
     * Set up the media reader.
     */
    private void setupGst() {
        Gst.init();

        pipeline = new Pipeline("Pipeline");

        // Decoding bin.
        DecodeBin decodeBin = new DecodeBin("Decode bin");

        // Source is from a file.
        Element fileSource = ElementFactory.make("filesrc", "Input File");
        fileSource.set("location", audioFile.getAbsolutePath());

        // Decode queue for buffering.
        Element decodeQueue = ElementFactory.make("queue", "Decode Queue");
        pipeline.addMany(fileSource, decodeQueue, decodeBin);

        if (!linkMany(fileSource, decodeQueue, decodeBin)) {
            System.err.println("Failed to link 1.");
        }

        // Audio handling bin.
        final Bin audioBin = new Bin("Audio bin");

        Element audioConvert = ElementFactory.make("audioconvert", null);

        Element audioResample = ElementFactory.make("audioresample", null);
        // audioResample.set("quality", 6);

        Element audioOutput = ElementFactory.make("autoaudiosink", "sink");

        Element spectrum = ElementFactory.make("spectrum", "spectrum");
        spectrum.set("bands", SpectrumConstants.BANDS);
        spectrum.set("threshold", SpectrumConstants.MIN_MAGNITUDE);
        spectrum.set("post-messages", true);

        Caps caps = Caps.fromString("audio/x-raw-int, rate="
                + SpectrumConstants.SAMPLE_RATE);

        audioBin.addMany(audioConvert, audioResample, spectrum, audioOutput);

        if (!linkMany(audioConvert, audioResample)) {
            LOGGER.error(getName()
                + " : Failed to link converter to resampler.");
        }

        if (!Element.linkPadsFiltered(audioResample, null, spectrum, null,
                    caps)) {
            LOGGER.error(getName()
                + " : Failed to apply audio capability filter.");
        }

        if (!linkMany(spectrum, audioOutput)) {
            LOGGER.error(getName() + " : Failed to link audio output.");
        }

        audioBin.addPad(new GhostPad("sink",
                audioConvert.getStaticPad("sink")));

        pipeline.add(audioBin);

        decodeBin.connect(new DecodeBin.NEW_DECODED_PAD() {

                @Override public void newDecodedPad(final Element element,
                    final Pad pad, final boolean last) {

                    if (pad.isLinked()) {
                        return;
                    }

                    Caps caps = pad.getCaps();
                    Structure struct = caps.getStructure(0);

                    if (struct.getName().startsWith("audio/")) {
                        System.out.println(
                            "Linking audio pad: " + struct.getName());
                        pad.link(audioBin.getStaticPad("sink"));
                    }
                }
            });

        Bus bus = pipeline.getBus();

        bus.connect(new Bus.ERROR() {
                public void errorMessage(final GstObject source, final int code,
                    final String message) {
                    LOGGER.error(
                        "PlaybackEngine Gstreamer Error: code=" + code
                        + " message=" + message);
                }
            });
        bus.connect(new Bus.EOS() {
                public void endOfStream(final GstObject source) {
                    pipeline.setState(org.gstreamer.State.NULL);
                }

            });

        final Spectrum spectrumComp = new Spectrum();
        bus.connect(spectrumComp);

        Runnable edtTask = new Runnable() {
                @Override public void run() {
                    dialog.getContentPane().removeAll();
                    dialog.setSpectrum(spectrumComp);
                }
            };
        SwingUtilities.invokeLater(edtTask);
    }

    /**
     * Handles seeking through the current audio file.
     */
    private void engineSeeking() {
        pipeline.seek(newTime, TimeUnit.MILLISECONDS);

        /*
         * Mark engine state with task complete so that isPlaying returns false
         * while we are jogging.
         */
        engineState = EngineState.TASK_COMPLETE;
    }

    /**
     * Adjust playback speed.
     */
    private void engineAdjusting() {
        engineState = EngineState.TASK_COMPLETE;
    }

    private void engineSettingFPS() {
        engineState = EngineState.TASK_COMPLETE;
    }

    /**
     * Start playing back the audio file.
     */
    private void enginePlaying() {
        pipeline.setState(org.gstreamer.State.PLAYING);
    }

    /**
     * Stop audio output.
     */
    private void engineStop() {
        pipeline.stop();
        engineState = EngineState.TASK_COMPLETE;
    }

    /**
     * Queue up a command to start audio playback.
     */
    public void startPlayback() {
        commandQueue.offer(EngineState.PLAYING);
    }

    /**
     * Queue up a command to stop audio playback.
     */
    public void stopPlayback() {
        commandQueue.offer(EngineState.STOPPING);
    }

    /**
     * @return True if the engine is initializing.
     */
    public boolean isInitializing() {
        return engineState == EngineState.INITIALIZING;
    }

    /**
     * @return True if the engine is playing back the audio file.
     */
    public boolean isPlaying() {
        return (engineState == EngineState.PLAYING)
            || (engineState == EngineState.SEEKING);
    }

    /**
     * Queue up a command to seek to the given time in milliseconds.
     *
     * @param time
     *            time to seek to in milliseconds.
     */
    public void seek(final long time) {
        newTime = time;

        if (engineState != EngineState.SEEKING) {
            commandQueue.offer(EngineState.SEEKING);
        }
    }

    public void adjustSpeed(final double speed) {
        playbackSpeed = speed;
        commandQueue.offer(EngineState.ADJUSTING_SPEED);
    }

    public void setAudioFPS(final double fps) {
        audioFPS = fps;
        commandQueue.offer(EngineState.SETTING_FPS);
    }

    /**
     * @return Current time in the audio file.
     */
    public long getCurrentTime() {

        synchronized (this) {
            return currentTime;
        }
    }

    /**
     * Notify the engine about the temporal position in the audio file being
     * played back.
     *
     * @see org.openshapa.plugins.spectrum.events.TimestampListener#notifyTime(long)
     */
    @Override public void notifyTime(final long time) {

        synchronized (this) {
            currentTime = time;
        }
    }

    /**
     * Shutdown the engine.
     */
    public void shutdown() {
    }

}
