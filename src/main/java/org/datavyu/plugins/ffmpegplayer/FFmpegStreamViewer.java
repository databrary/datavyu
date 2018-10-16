package org.datavyu.plugins.ffmpegplayer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.models.Identifier;
import org.datavyu.plugins.StreamViewerDialog;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FFmpegStreamViewer extends StreamViewerDialog {

    /** The logger for this class */
    private static Logger logger = LogManager.getFormatterLogger(FFmpegStreamViewer.class);

    /** Previous setCurrentTime time */
    private long previousSeekTime = -1;

    /** The player this viewer is displaying */
    private FFmpegPlayer player;

    /** Currently is seeking */
    private boolean isSeeking = false;

    FFmpegStreamViewer(final Identifier identifier, final File sourceFile, final Frame parent, final boolean modal) {
        super(identifier, parent, modal);
        logger.info("Opening file: " + sourceFile.getAbsolutePath());
        player = new FFmpegPlayer(this);
        player.openFile(sourceFile.getAbsolutePath());
        setSourceFile(sourceFile);
    }

    private void launch(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeLater(task);
            } catch (Exception e) {
                logger.error("Failed task. Error: ", e);
            }
        }
    }

    @Override
    protected void setPlayerVolume(float volume) {
        player.setVolume(volume);
    }

    @Override
    protected Dimension getOriginalVideoSize() {
        Dimension videoSize = player.getOriginalVideoSize();
        logger.info("The original video size: " + videoSize);
        return player.getOriginalVideoSize();
    }

    @Override
    public void setCurrentTime(long time) {
        launch(() -> {
            try {
                logger.info("Set time to: " + time + " milliseconds.");
                if (!isSeeking) {
                    EventQueue.invokeLater(() -> {
                        isSeeking = true;
                        player.setCurrentTime(time / 1000.0);
                        isSeeking = false;
                    });
                }
            } catch (Exception e) {
                logger.error("Unable to find", e);
            }
        });
    }

    @Override
    public void start() {
        launch(() -> {
            if (!isPlaying()) {
                player.play();
            }
        });
    }

    @Override
    public void stop() {
        launch(() -> {
            if (isPlaying()) {
                player.stop();
            }
        });
    }

    @Override
    public void setRate(float speed) {
        launch(() -> {
            playBackRate = speed;
            if (speed == 0) {
                player.stop();
            } else {
                player.setPlaybackSpeed(speed);
            }
        });
    }

    @Override
    protected float getPlayerFramesPerSecond() { return (float) player.getFPS(); }

    @Override
    public long getDuration() {
        return (long) (player.getDuration() * 1000);
    }

    @Override
    public long getCurrentTime() {
        return (long) (player.getCurrentTime() * 1000);
    }

    @Override
    protected void cleanUp() {
        player.cleanUp();
    }

    @Override
    public void stepForward() {
        launch(() -> {
            player.stepForward();
        });
    }

    @Override
    public void stepBackward() {
        launch(() -> {
            player.stepBackward();
        });
    }

    @Override
    protected void resizeVideo(float scale) {
        super.resizeVideo(scale);
        logger.info("Resizing video to scale %2.2f", scale);
        player.setScale(scale);
        notifyChange();
    }

    @Override
    public boolean isStepEnabled() {
        return true;
    }

    @Override
    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    @Override
    public boolean isSeekPlaybackEnabled() {
        return false;
    }

    @Override
    protected boolean handleResizeInJava() {
        return false;
    }
}
