package org.datavyu.plugins.javafx;

import javafx.application.Platform;
import javafx.scene.media.MediaException;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.models.Identifier;
import org.datavyu.plugins.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.concurrent.CountDownLatch;


public class JfxStreamViewerDialog extends StreamViewerDialog {

    private static Logger logger = LogManager.getFormatterLogger(JfxStreamViewerDialog.class);

    /** Data to visualize */
    private File sourceFile;

    /** FPS of the video, calculated on launch */
    private float fps;

    private JDialog dialog = new JDialog();

    private JfxApplication jfxApplication;

    private boolean assumedFPS = false;

    JfxStreamViewerDialog(Identifier identifier, File sourceFile, final Frame parent, final boolean modal) {
        super(identifier, parent, modal);
        jfxApplication = new JfxApplication(sourceFile);
        setSourceFile(sourceFile);
    }

    private static void runAndWait(final Runnable action) {
        if (action == null)
            throw new NullPointerException("action");

        // run synchronously on JavaFX thread
        if (Platform.isFxApplicationThread()) {
            logger.info("Javax thread running action.");
            action.run();
            return;
        }

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
        jfxApplication.setVolume(volume);
    }

    @Override
    public JDialog getParentJDialog() {
        return dialog;
    }

    @Override
    public float getFramesPerSecond() {
        return jfxApplication.getFrameRate();
    }

    public void setFramesPerSecond(float framesPerSecond) {
        fps = framesPerSecond;
        assumedFPS = false;
    }

    @Override
    public void setViewerVisible(final boolean isVisible) {
        jfxApplication.setVisible(isVisible);
        this.isVisible = isVisible;
    }

    @Override
    public File getSourceFile() {
        return sourceFile;
    }

    @Override
    public void setSourceFile(final File sourceFile) {
        logger.info("Set source file: " + sourceFile.getAbsolutePath());

        final CountDownLatch latch = new CountDownLatch(1);
        this.sourceFile = sourceFile;
        Platform.setImplicitExit(false);

        jfxApplication = new JfxApplication(sourceFile);

        logger.info("Is event dispatch thread? " + (SwingUtilities.isEventDispatchThread() ? "Yes" : "No") + ".");
        logger.info("Is FX application thread? " + (Platform.isFxApplicationThread() ? "Yes" : "No") + ".");

        runAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    jfxApplication.start(new Stage());
                } catch (MediaException me) {
                    // TODO: Possibly open a dialog with an error that the file format is not supported
                    logger.warn("Could not open media file: " + sourceFile + "\nException: " + me);
                }
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (Exception e) {
            logger.error("Latch await failed. Error: ", e);
        }

        while (!jfxApplication.isInit()) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                logger.info("Waiting for jfx application to initialize", e);
            }
        }

        logger.info("Finished setting source: " + sourceFile);
        logger.info("Duration is: " + jfxApplication.getDuration());

        // Hide our fake dialog box
        dialog.setVisible(false);
        // TODO Add in function to guess frame rate
    }


    /**
     * Scales the video to the desired ratio.
     *
     * @param scale The new ratio to scale to, where 1.0 = original size, 2.0 = 200% zoom, etc.
     */
    @Override
    protected void resizeVideo(final float scale) {
        logger.info("Resize with scale %2.2f", scale);
        jfxApplication.setScale(scale);
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
        return jfxApplication.getDuration();
    }

    @Override
    public long getCurrentTime() {
        return jfxApplication.getCurrentTime();
    }

    @Override
    public void setCurrentTime(final long time) {
        jfxApplication.seek(time);
    }

    @Override
    public boolean isPlaying() {
        return jfxApplication.isPlaying();
    }

    @Override
    public void stop() {
        jfxApplication.pause();
    }

    @Override
    public void setRate(final float rate) {
        logger.info("Set the rate to: " + rate);
        super.setRate(rate);
        jfxApplication.setRate(rate);
    }

    @Override
    public void start() {
        jfxApplication.play();
    }

    @Override
    protected void cleanUp() { }

    @Override
    public void close() {
        stop();
        jfxApplication.setVisible(false);
        jfxApplication.closeAndDestroy();
    }

    @Override
    public boolean isSeekPlaybackEnabled() {
        return playBackRate > 2F || playBackRate < 0F;
    }
}
