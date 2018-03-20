package org.datavyu.plugins.nativeosx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.Datavyu;
import org.datavyu.util.NativeLibraryLoader;

import java.awt.*;
import java.io.File;

public class NativeOSXPlayer extends Canvas {

    private static Logger logger = LogManager.getLogger(NativeOSXPlayer.class);

    private static int playerCount = 0;

    static {
        try {
            if (Datavyu.getPlatform() == Datavyu.Platform.MAC) {
                NativeLibraryLoader.extractAndLoad("NativeOSXCanvas");
            }
        } catch (Exception e) {
            logger.error("Unable to load the native library: ", e);
        }
    }

    public final int id;

    private File sourceFile;

    public static void incPlayerCount() {
        playerCount++;
    }

    public static void decPlayerCount() {
        playerCount--;
    }

    public NativeOSXPlayer(File fileToLoad) {
        this.id = NativeOSXPlayer.playerCount;
        incPlayerCount();
        this.sourceFile = fileToLoad;
    }

    public void addNotify() {
        super.addNotify();
        logger.info("Opening video file: " + sourceFile.toURI().getPath());
        try {
            addNativeOSXCoreAnimationLayer("file://" + sourceFile.toURI().getPath());
        } catch (Exception e) {
            logger.error("Error when opening native core animation layer ", e);
        }
    }

    // This method is implemented in native code. See NativeOSXCanvas.m
    public native void addNativeOSXCoreAnimationLayer(String path);

    public native void stop(int id);

    public native void play(int id);

    public native void setTime(long time, int id);

    public native void setTimePrecise(long time, int id);

    public native void setTimeModerate(long time, int id);

    public native void setVolume(float time, int id);

    public native void release(int id);

    public native double getMovieHeight(int id);

    public native double getMovieWidth(int id);

    public native long getCurrentTime(int id);

    public native long getDuration(int id);

    public native float getRate(int id);

    public native void setRate(float rate, int id);

    public native boolean isPlaying(int id);

    public native float getFPS(int id);

}