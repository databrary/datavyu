package org.datavyu.plugins.vlcfx;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.models.Identifier;
import org.datavyu.plugins.StreamViewerDialog;
import org.datavyu.plugins.VlcLibraryLoader;
import org.datavyu.views.component.DefaultTrackPainter;
import org.datavyu.views.component.TrackPainter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;


public class VlcFxDataViewerDialog extends StreamViewerDialog {

    /** Logger for this class */
    private static Logger logger = LogManager.getLogger(NativeLibraryManager.class);

    /** The last jog position, making sure we are only calling jog once VLC has issues when trying to go to the same
     * spot multiple times */
    private JDialog dialog = new JDialog();

    /** VLC application */
    private VlcApplication vlcApplication;

    VlcFxDataViewerDialog(final Identifier identifier, final File sourceFile, final Frame parent, final boolean modal) {
        super(identifier, parent, modal);
        dialog.setVisible(false);
        this.sourceFile = sourceFile;
    }

    private static void runAndWait(final Runnable action) {

        VlcLibraryLoader.load();

        // run synchronously on JavaFX thread
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        // queue on JavaFX thread and wait for completion
        final CountDownLatch doneLatch = new CountDownLatch(1);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    action.run();
                } catch (Exception e){
                    logger.error("Error when trying to run the application: ", e);
                } finally {
                    doneLatch.countDown();
                }
            }
        });

        try {
            doneLatch.await();
        } catch (InterruptedException ie) {
            logger.error("Interrupt when waiting for the latch: ", ie);
        }
    }

    protected void setPlayerVolume(float volume) {
        vlcApplication.setVolume(volume);
    }


    @Override
    public JDialog getParentJDialog() {
        return dialog;
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
    public float getFramesPerSecond() {
        return vlcApplication.getFrameRate();
    }

    public void setFramesPerSecond(float framesPerSecond) {
        // TODO: Set the fps correctly
        //fps = fpsIn;
        //assumedFPS = false;
    }

    @Override
    public TrackPainter getTrackPainter() {
        return new DefaultTrackPainter();
    }

    @Override
    public void setViewerVisible(final boolean isVisible) {
        vlcApplication.setVisible(isVisible);
    }

    @Override
    public void setVisible(final boolean isVisible) {
        vlcApplication.setVisible(isVisible);
    }


    @Override
    public void setSourceFile(final File sourceFile) {

        // Needed to init JavaFX stuff
        new JFXPanel();
        vlcApplication = new VlcApplication(sourceFile);

        runAndWait(new Runnable() {
            @Override
            public void run() {
                vlcApplication.start(new Stage());
            }
        });


        // Wait for javafx to initialize
        // TODO: We need to change this, because it can lead to hang-up (e.g. timeout)
        while (!vlcApplication.isInitialized()) {
        }

        // Hide our fake dialog box
        dialog.setVisible(false);

        // TODO: Add in function to guess framerate
    }

    @Override
    public long getDuration() { return vlcApplication.getDuration(); }

    @Override
    public long getCurrentTime() {
        return vlcApplication.getCurrentTime();
    }

    @Override
    public void setCurrentTime(final long time) {
        vlcApplication.seek(time);
    }

    @Override
    public boolean isPlaying() {
        return vlcApplication.isPlaying();
    }

    @Override
    public void stop() {
        vlcApplication.pause();
    }

    @Override
    public void setRate(final float rate) {
        vlcApplication.setRate(rate);
    }

    @Override
    public void start() {
        vlcApplication.play();
    }

    @Override
    protected void cleanUp() {
        VlcLibraryLoader.purge();
        close();
    }

    @Override
    public void close() {
        stop();
        vlcApplication.setVisible(false);
        vlcApplication.closeAndDestroy();
    }

    public boolean isAssumedFramesPerSecond() {
        return vlcApplication.isAssumedFps();
    }

    @Override
    public void storeSettings(final OutputStream os) {
        try {
            Properties settings = new Properties();
            settings.setProperty("startTime", Long.toString(getOffset()));
            settings.setProperty("volume", Float.toString(getVolume()));
            settings.setProperty("visible", Boolean.toString(vlcApplication.isVisible()));
            settings.setProperty("height", Integer.toString(vlcApplication.getHeight()));
            settings.setProperty("fps", Float.toString(vlcApplication.getFrameRate()));
            settings.store(os, null);
        } catch (IOException io) {
            logger.error("Unable to save the settings. Error: ", io);
        }
    }
}
