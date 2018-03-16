package org.datavyu.plugins.mplayer;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.models.Identifier;
import org.datavyu.plugins.StreamViewerDialog;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.concurrent.CountDownLatch;


public class MPlayerDataViewerDialog extends StreamViewerDialog {

    private static Logger logger = LogManager.getLogger(MPlayerDataViewerDialog.class);

    /**
     * Boolean to keep track of whether or not we are isPlaying
     */
    private boolean playing;


    /**
     * The last jog position, making sure we are only calling jog once
     * VLC has issues when trying to go to the same spot multiple times
     */
    private JDialog dialog = new JDialog();
    private MPlayerApplication mPlayerApp;
    private boolean assumedFPS = false;

    MPlayerDataViewerDialog(final Identifier identifier, final File sourceFile, final Frame parent, final boolean modal) {
        super(identifier, parent, modal);
        mPlayerApp = new MPlayerApplication(sourceFile);
        setSourceFile(sourceFile);
    }

    static void runAndWait(final Runnable action) {
        if (action == null)
            throw new NullPointerException("action");

        // run synchronously on JavaFX thread
        if (Platform.isFxApplicationThread()) {
            logger.info("Javax thread running action.");
            action.run();
            return;
        }

        // queue on JavaFX thread and wait for completion
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.info("Running action " + (Platform.isFxApplicationThread() ? " as JavaFx thread." : "."));
                    action.run();
                    logger.info("Action ran successful.");
                } catch (Exception e) {
                    logger.error("Exception occurred when running action.", e);
                }
            }
        });
    }

    @Override
    protected void setPlayerVolume(float volume) {
        mPlayerApp.setVolume(volume);
    }

    @Override
    public JDialog getParentJDialog() {
        return dialog;
    }

    @Override
    public float getFramesPerSecond() {
        return mPlayerApp.getFrameRate();
    }

    public void setFramesPerSecond(float framesPerSecond) {
        assumedFPS = false;
    }

    @Override
    public void setViewerVisible(final boolean isVisible) {
        mPlayerApp.setVisible(isVisible);
        this.isVisible = isVisible;
    }

    @Override
    public void setSourceFile(final File sourceFile) {

        logger.info("Set source file: " + sourceFile.getAbsolutePath());

        final CountDownLatch latch = new CountDownLatch(1);
        Platform.setImplicitExit(false);

        mPlayerApp = new MPlayerApplication(sourceFile);

        logger.info("Is event dispatch thread? " + (SwingUtilities.isEventDispatchThread() ? "Yes" : "No") + ".");
        logger.info("Is FX application thread? " + (Platform.isFxApplicationThread() ? "Yes" : "No") + ".");


        runAndWait(new Runnable() {
            @Override
            public void run() {
                mPlayerApp.start(new Stage());
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (Exception e) {
            logger.error("Await latch failed. Error: ", e);
        }

        while (!mPlayerApp.isInit()) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                logger.error("Waited for thread. Error: ", e);
            }
        }

        logger.info("Finished setting source: " + sourceFile);
        logger.info("Duration is: " + mPlayerApp.getDuration());

        dialog.setVisible(false); // Hide our fake dialog box

        // TODO Add in function to guess frame rate
    }

    /**
     * Scales the video to the desired ratio.
     *
     * @param scale The new ratio to scale to, where 1.0 = original size, 2.0 = 200% zoom, etc.
     */
    @Override
    protected void resizeVideo(final float scale) {
        mPlayerApp.setScale(scale);

        notifyChange();
    }

    @Override
    protected Dimension getOriginalVideoSize() {
        return null;
    }

    @Override
    protected float getPlayerFramesPerSecond() {
        return getFramesPerSecond();
    }

    @Override
    public long getDuration() {
        return mPlayerApp.getDuration();
    }

    @Override
    public long getCurrentTime() {
        return mPlayerApp.getCurrentTime();
    }

    @Override
    public void setCurrentTime(final long time) {
        mPlayerApp.seek(time);
    }

    @Override
    public boolean isPlaying() {
        return playing;
    }

    @Override
    public void stop() {
        playing = false;
        mPlayerApp.pause();
    }

    @Override
    public void setRate(final float rate) {
        mPlayerApp.setRate(rate);
    }

    @Override
    public void start() {
        playing = true;
        mPlayerApp.play();
    }

    @Override
    protected void cleanUp() {

    }

    @Override
    public void close() {
        stop();
        mPlayerApp.setVisible(false);
        mPlayerApp.closeAndDestroy();
    }

    public boolean isAssumedFramesPerSecond() {
        return assumedFPS;
    }
}
