package org.datavyu.plugins.ffmpegplayer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.plugins.ffmpeg.*;
import org.datavyu.plugins.ffmpeg.MediaPlayer;
import org.datavyu.util.NativeLibraryLoader;

import javax.sound.sampled.AudioFormat;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.io.File;
import java.net.URI;

public class FFmpegPlayer extends JPanel {

    /** Identifier for object serialization */
    private static final long serialVersionUID = 5109839668203738974L;

    /** The logger for this class */
    private static Logger logger = LogManager.getFormatterLogger(FFmpegPlayer.class);

    /** Load the native library that interfaces to ffmpeg 4.0 */
    static {
        try {
            logger.info("Extracting libraries for ffmpeg.");
            NativeLibraryLoader.extract("avutil-56");
            NativeLibraryLoader.extract("swscale-5");
            NativeLibraryLoader.extract("swresample-3");
            NativeLibraryLoader.extract("avcodec-58");
            NativeLibraryLoader.extract("avformat-58");
            NativeLibraryLoader.extract("avfilter-7");
            NativeLibraryLoader.extract("avdevice-58");
            NativeLibraryLoader.extract("postproc-55");
            NativeLibraryLoader.extract("SDL2");
            NativeLibraryLoader.extract("FfmpegMediaPlayer");
        } catch (Exception e) {
            logger.error("Failed loading ffmpeg libraries due to error: ", e);
        }
    }

	/** The requested color space */
	private final ColorSpace reqColorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);

	/** The requested audio format */
	private final AudioFormat reqAudioFormat = AudioSoundStreamListener.getNewMonoFormat();
	
	/** The movie stream for this movie player */
	private MediaPlayerData mediaPlayer;

	/**
	 * Construct an FFmpegPlayer by creating the underlying movie stream provider
	 * and registering stream listeners for the video and audio. The stream
	 * listener for the video will show the image in this JPanel.
	 *
	 * @param viewer The ffmpeg viewer
     * @param sourceFile The source file
	 */
	FFmpegPlayer(FFmpegStreamViewer viewer, File sourceFile) {
		setLayout(new BorderLayout());
		try {
			mediaPlayer = new FfmpegMediaPlayer(sourceFile, viewer);
			mediaPlayer.init(reqAudioFormat, reqColorSpace);
		}catch (Exception e) {
			logger.error("Cannot initialize ffmpeg player due to error: ", e);
		}
	}

	/**
	 * Get the duration of the opened video/audio stream of this player in seconds.
	 *
	 * @return Duration of the opened stream.
	 */
	public double getDuration() {
		return mediaPlayer.getDuration();
	}

	/**
	 * Get the original stream size (not the size when a viewing window is used).
	 *
	 * @return Original stream size: width, height.
	 */
	public Dimension getOriginalVideoSize() {
		return new Dimension(mediaPlayer.getImageWidth(), mediaPlayer.getImageHeight());
	}

	/**
	 * Get the current time in seconds.
	 *
	 * @return Current time in seconds.
	 */
	public double getCurrentTime() {
		return mediaPlayer.getPresentationTime();
	}

	/**
	 * Seek to the position.
	 *
	 * @param position Position in seconds.
	 */
	public void setCurrentTime(double position) {
        logger.info("Seeking position: " + position);
		mediaPlayer.seek(position);
    }

	/**
	 * Clean up the player before closing.
	 */
	public void cleanUp() {
	    logger.info("Closing stream.");
		mediaPlayer.dispose();
	}

	/**
	 * Set the start back speed for this player.
	 *
	 * @param playbackSpeed The start back speed.
	 */
	public void setPlaybackSpeed(float playbackSpeed) {
		logger.info("Setting start back speed: " + playbackSpeed);
		// Not implemented yet, we are using the fake playback
//		mediaPlayer.setRate(playbackSpeed);
	}

	/**
	 * Play the video/audio.
	 */
	public void play() {
		logger.info("Starting isPlaying video");
		mediaPlayer.play();
	}

	/**
	 * Stop the video/audio.
	 */
	public void stop() {
		logger.info("Stopping the video.");
		mediaPlayer.stop();
	}

	@Deprecated
	public void setScale(float scale) {
//	    displayStreamListener.setScale(scale);
    }

	/**
	 * Instead of isPlaying a sequence of frames just step by one frame.
	 */
	public void stepForward() {
	    logger.info("Step forward.");
//		mediaPlayer.stepForward();
	}

	public void stepBackward() {
		logger.info("Step backward.");
		//TODO(Reda): Implement step backward, not coded in the native side.
//		mediaPlayer.stepBackward();
	}

	/**
	 * Set the audio volume.
	 *
	 * @param volume New volume to set.
	 */
	public void setVolume(float volume) {
		mediaPlayer.setVolume(volume);
	}

	public void setMute(final boolean newMute) {
		mediaPlayer.setMute(newMute);
	}

	public boolean isMute() {
		return mediaPlayer.getMute();
	}

	boolean isPlaying() {
	    return mediaPlayer.getState() == PlayerStateEvent.PlayerState.PLAYING;
    }

    public double getFPS() {
    return mediaPlayer.getFps();
	}
}
