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
package org.datavyu.plugins.qtkitplayer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.models.Identifier;
import org.datavyu.plugins.StreamViewerDialog;

import java.awt.*;
import java.io.File;


/**
 * The viewer for a quicktime video file.
 * <b>Do not move this class, this is for backward compatibility with 1.07.</b>
 */
public final class QtKitViewerDialog extends StreamViewerDialog {

    /** The logger for this class */
    private static Logger logger = LogManager.getLogger(QtKitViewerDialog.class);

    /** Last setCurrentTime time */
    private long lastSeekTime = -1;

    /** The player this viewer is displaying */
    private QtKitPlayer player;

    QtKitViewerDialog(final Identifier identifier, final File sourceFile, final Frame parent, final boolean modal) {
        super(identifier, parent, modal);
        setPlayerSourceFile(sourceFile);
    }

    @Override
    protected void setPlayerVolume(final float volume) {
        if (player != null) {
            EventQueue.invokeLater(() -> player.setVolume(volume, player.id));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDuration() {
        return player.getDuration(player.id);
    }

    private void setPlayerSourceFile(final File sourceFile) {
        // Ensure that the native hierarchy is set up
        this.addNotify();
        player = new QtKitPlayer(sourceFile);
        this.add(player, BorderLayout.CENTER);
        EventQueue.invokeLater(() -> {
            try {
                // Make sure that the player is loaded by setting the volume
                player.setVolume(0.7F, player.id);
            } catch (Exception e) {
                // Oops! Back out
                QtKitPlayer.decPlayerCount();
                throw e;
            }
        });
    }

    @Override
    protected Dimension getOriginalVideoSize() {
        System.err.println(player.id);
        return new Dimension((int) player.getMovieWidth(player.id),
                             (int) player.getMovieHeight(player.id));
    }

    @Override
    protected float getPlayerFramesPerSecond() {
        return player.getFPS(player.id);
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
            if (player != null) {
                EventQueue.invokeLater(() -> {
                    if (player.getRate(player.id) != 0) {
                        player.stop(player.id);
                    }
                    player.setRate(getRate(), player.id);
                });
            }
        } catch (Exception e) {
            logger.error("Unable to start", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        logger.info("HIT STOP");
        final double time = System.currentTimeMillis();
        try {
            if (player != null) {
                EventQueue.invokeLater(() -> {
                    logger.info("EXECUTING STOPPING AT TIME: " + (System.currentTimeMillis() - time));
                    player.stop(player.id);
                    logger.info("STOPPED AT TIME: " + (System.currentTimeMillis() - time));
                });
            }
        } catch (Exception e) {
            logger.error("Failed to stop! Error: ", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentTime(final long time) {
        try {
            if (player != null && (lastSeekTime != time)) {
                lastSeekTime = time;
                logger.info("Seeking position: " + time);
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        boolean wasPlaying = isPlaying();
                        float prevRate = getRate();
                        if (isPlaying())
                            player.stop(player.id);
                        player.setTime(time, player.id);
                        if (wasPlaying) {
                            player.setRate(prevRate, player.id);
                        }
                    }
                });

            }
        } catch (Exception e) {
            logger.error("Unable to setCurrentTime! Error: ", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCurrentTime() {
        try {
            return player.getCurrentTime(player.id);
        } catch (Exception e) {
            logger.error("Unable to get current time! Error: ", e);
        }
        return 0;
    }

    @Override
    protected void cleanUp() {
        // TODO: Do we need to release the player here?
//        player.release();
    }

    @Override
    public boolean isPlaying() {
        return player.isPlaying(player.id);
    }
}
