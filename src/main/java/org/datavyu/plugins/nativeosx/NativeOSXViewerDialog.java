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
package org.datavyu.plugins.nativeosx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.Datavyu;
import org.datavyu.models.Identifier;
import org.datavyu.plugins.StreamViewerDialog;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;


/**
 * The viewer for a quick time video file.
 * <b>Do not move this class, this is for backward compatibility with 1.07.</b>
 */
public final class NativeOSXViewerDialog extends StreamViewerDialog {

    //private long timeOfPrevSeek = 0;

    private static final int NUM_RETRY_FOR_DURATION = 2;

    /** The logger for this class */
    private static Logger logger = LogManager.getLogger(NativeOSXViewerDialog.class);

    /** The quick time native OSXPlayer */
    private NativeOSXPlayer nativeOSXPlayer;

    private boolean seeking = false;

    private long duration = 0;

    NativeOSXViewerDialog(final Identifier identifier, final File sourceFile, final Frame parent, final boolean modal) {
        super(identifier, parent, modal);

        logger.info("Set source file: "+ sourceFile.getAbsolutePath());
        nativeOSXPlayer = new NativeOSXPlayer(sourceFile);
        this.addNotify();
        this.add(nativeOSXPlayer, BorderLayout.CENTER);
        setSourceFile(sourceFile);
    }

    @Override
    protected void setPlayerVolume(final float volume) {
        if (nativeOSXPlayer != null) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    nativeOSXPlayer.setVolume(volume, nativeOSXPlayer.id);
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDuration() {
        if (nativeOSXPlayer != null) {
            for (int iRetry = 0; iRetry < NUM_RETRY_FOR_DURATION; ++iRetry) {
                duration = nativeOSXPlayer.getDuration(nativeOSXPlayer.id);
                if (duration > 0) {
                    return duration;
                }
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    logger.info("Retry " + iRetry + " of duration read out failed.");
                }
            }
        }
        return duration;
    }

    @Override
    protected Dimension getOriginalVideoSize() {
        System.err.println(nativeOSXPlayer.id);
        return new Dimension((int) nativeOSXPlayer.getMovieWidth(nativeOSXPlayer.id), (int) nativeOSXPlayer.getMovieHeight(nativeOSXPlayer.id));
    }

    @Override
    protected float getPlayerFramesPerSecond() {
        float fps = nativeOSXPlayer.getFPS(nativeOSXPlayer.id);
        if (fps <= 1) {
            try {
                Thread.sleep(2000);
                fps = nativeOSXPlayer.getFPS(nativeOSXPlayer.id);
                if(fps > 1) {
                    return fps;
                }
            } catch (InterruptedException e) {
                logger.error("Thread slept. Error: ", e);
            }
            isAssumedFramesPerSecond = true;
            return 29.97f;
        }
        return nativeOSXPlayer.getFPS(nativeOSXPlayer.id);
    }

    @Override
    public void setRate(final float rate) {
        super.setRate(rate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        logger.info("Playing at speed: " + getRate());

        try {
            if (nativeOSXPlayer != null) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        if (nativeOSXPlayer.getRate(nativeOSXPlayer.id) == 0) {
                            nativeOSXPlayer.stop(nativeOSXPlayer.id);
                        }
                        nativeOSXPlayer.setRate(getRate(), nativeOSXPlayer.id);
                    }
                });
            } else {
                logger.info("No nativeOSXPlayer loaded!");
            }
        } catch (Exception e) {
            logger.error("Unable to start! Error: ", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        logger.info("Player stopped");
        final double time = System.currentTimeMillis();
        try {

            if (nativeOSXPlayer != null) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        logger.info("STOPPING EXECUTING AT: " + (System.currentTimeMillis() - time));
                        nativeOSXPlayer.stop(nativeOSXPlayer.id);
                        logger.info("STOPPED EXECUTION AT: " + (System.currentTimeMillis() - time));
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Unable to stop", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentTime(final long time) {

/*        // Throttle the seek rate to 1/35 milliseconds or 1/0.035 Hz
        if (System.currentTimeMillis() - timeOfPrevSeek < 35) {
            return;
        }*/
        // This should be handled by the seeking == TRUE

        if (!seeking) {
            seeking = true;
            try {
                if (nativeOSXPlayer != null) {
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            logger.info("Seeking to position: " + time +" Is playing: "+ isPlaying());
                            boolean wasPlaying = isPlaying();
                            float prevRate = nativeOSXPlayer.getRate(nativeOSXPlayer.id);
                            if (isPlaying()) {
                                nativeOSXPlayer.stop(nativeOSXPlayer.id);
                            }
                            if (prevRate >= 0 && prevRate <= 8) {
                                nativeOSXPlayer.setTimePrecise(time, nativeOSXPlayer.id);
                            } else if (prevRate < 0  && prevRate > -8) {
                                nativeOSXPlayer.setTimeModerate(time, nativeOSXPlayer.id);
                            } else {
                                nativeOSXPlayer.setTime(time, nativeOSXPlayer.id);
                            }
                            if (wasPlaying) {
                                nativeOSXPlayer.setRate(prevRate, nativeOSXPlayer.id);
                            }
                            nativeOSXPlayer.repaint();
                            //timeOfPrevSeek = System.currentTimeMillis();
                            seeking = false;
                        }
                    });
                }
            } catch (Exception e) {
                logger.error("Unable to find", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCurrentTime() {
        try {
            return nativeOSXPlayer.getCurrentTime(nativeOSXPlayer.id);
        } catch (Exception e) {
            logger.error("Unable to get time", e);
        }
        return 0;
    }

    @Override
    protected void cleanUp() {
        // TODO: Check if the release is required?
//        nativeOSXPlayer.release();
    }

    @Override
    public boolean isPlaying() {
        return !nativeOSXPlayer.isPlaying(nativeOSXPlayer.id); // the native os plugin return false when is playing
    }
}
