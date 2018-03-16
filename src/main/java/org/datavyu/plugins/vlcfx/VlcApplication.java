package org.datavyu.plugins.vlcfx;

import com.sun.jna.Memory;
import com.sun.prism.GraphicsPipeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.Datavyu;
import uk.co.caprica.vlcj.component.DirectMediaPlayerComponent;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.format.RV32BufferFormat;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class VlcApplication extends Application {

    /** Logger for this class */
    private static Logger logger = LogManager.getLogger(VlcApplication.class);

    static {
        Platform.setImplicitExit(false);

        // TODO: Do we need this native discovery here?
        logger.info("Native discovery.");
        new NativeDiscovery();
    }

    /** Pixel format */
    private final WritablePixelFormat<ByteBuffer> pixelFormat;

    /** Border pane */
    private final BorderPane borderPane;

    /** Lightweight JavaFX canvas to render the video */
    private final Canvas canvas;

    /** Source file for the isPlaying video */
    private File sourceFile;

    /** Variable that indicates that this player is initialized */
    private boolean isInitialized = false;

    /** Target width, unless {@link #useSourceSize} is set */
    private int width = 1920;

    /** Target height, unless {@link #useSourceSize} is set. */
    private int height = 1080;

    /** Set this to <code>true</code> to resize the display to the dimensions of the video, otherwise it will use
     * {@link #width} and {@link #height}. */
    private boolean useSourceSize = true;

    /** The vlcj direct rendering media player component */
    private TestMediaPlayerComponent mediaPlayerComponent;

    /** Direct media player */
    private DirectMediaPlayer directMediaPlayer;

    /** JavaFx stage for application */
    private Stage stage;

    /** JavaFx scene for application */
    private Scene scene;

    /** Pixel writer to update the canvas */
    private PixelWriter pixelWriter;

    /** Duration of the video file */
    private long duration = -1;

    /** */
    private long lastVlcUpdateTime = -1;

    private long lastTimeSinceVlcUpdate = -1;

    private float fps;

    private double aspect;

    private long lastSeekTime = -1;

    private boolean isAssumedFps = false;


    public VlcApplication(File file) {
        sourceFile = file;
        canvas = new Canvas();

        pixelWriter = canvas.getGraphicsContext2D().getPixelWriter();
        pixelFormat = PixelFormat.getByteBgraPreInstance();

        borderPane = new BorderPane();
        borderPane.setCenter(canvas);
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void seek(long time) {
        // Only setCurrentTime if this is a new time (not the last setCurrentTime time and not the current time)
        if (lastSeekTime != time || time != directMediaPlayer.getTime()) {
            logger.info("SEEKING TO " + time);
            directMediaPlayer.setTime(time);
            lastSeekTime = time;
            logger.info("The new time is: " + directMediaPlayer.getTime());
        }
    }

    public void pause() {
        if (directMediaPlayer.isPlaying()) {
            directMediaPlayer.pause();
        }
        logger.info("Stated of player is " + (directMediaPlayer.isPlaying() ? "isPlaying." : " not isPlaying"));
    }

    public void play() {
        directMediaPlayer.play();
    }

    public void stop() {
        directMediaPlayer.stop();
    }

    public long getCurrentTime() {
        long vlcTime = directMediaPlayer.getTime();
        if (vlcTime == lastVlcUpdateTime) {
            long currentTime = System.currentTimeMillis();
            long timeSinceVlcUpdate = lastTimeSinceVlcUpdate - currentTime;
            lastTimeSinceVlcUpdate = currentTime;
            return vlcTime + timeSinceVlcUpdate;
        } else {
            return directMediaPlayer.getTime();
        }
    }

    public float getFrameRate() {
        // TODO: Why not use the detected frame rate here?
        if (fps == 0.0f) {
            return 30.0f;
        }
        return fps;
    }

    public long getDuration() {
        if (duration < 0) {
            duration = directMediaPlayer.getLength();
        }
        return duration;
    }

    public float getRate() {
        return (float) directMediaPlayer.getRate();
    }

    public void setRate(float rate) {
        directMediaPlayer.setRate(rate);
    }

    public boolean isVisible() { return stage.isShowing(); }

    public void setVisible(final boolean visible) {
        logger.info("Visible set to: " + visible);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                logger.info("Setting " + visible);
                if (!visible) {
                    stage.hide();
                } else {
                    stage.show();
                }
            }
        });

    }

    public boolean isAssumedFps() {
        return isAssumedFps;
    }

    public void setVolume(double volume) {
        directMediaPlayer.setVolume((int) (volume * 200));
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void closeAndDestroy() {
        directMediaPlayer.release();
        mediaPlayerComponent.release();
    }

    public boolean isPlaying() {
        return directMediaPlayer.isPlaying();
    }

    public int getHeight() {
        return (int) stage.getHeight();
    }

    public int getWidth() {
        return (int) stage.getWidth();
    }

    public void start(final Stage primaryStage) {

        this.stage = primaryStage;

        stage.setTitle("Datavyu: " + sourceFile.getName());

        scene = new Scene(borderPane);

        logger.info("The name of the graphics pipeline is: " + GraphicsPipeline.getPipeline().getClass().getName());

        mediaPlayerComponent = new TestMediaPlayerComponent();
        directMediaPlayer = mediaPlayerComponent.getMediaPlayer();
        directMediaPlayer.prepareMedia(sourceFile.getAbsolutePath());

        directMediaPlayer.play();

        // TODO: Do this differently using a time out
        while (!directMediaPlayer.isPlaying()) { }

        primaryStage.setScene(scene);
        primaryStage.show();

        final ChangeListener<Number> listener = new ChangeListener<Number>() {
            final Timer timer = new Timer(); // uses a timer to call your resize method
            final long delayTime = 200; // delay that has to pass in order to consider an operation done
            TimerTask task = null; // task to execute after defined delay

            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, final Number newValue) {

                // There was already a task scheduled from a previous operation
                if (task != null) {
                    // Cancel it, we have a new size to consider
                    task.cancel();
                }

                task = new TimerTask() // create new task that calls your resize operation
                {
                    @Override
                    public void run() {
                        useSourceSize = false;
                        logger.info("Resize to " + primaryStage.getWidth() + " x " + primaryStage.getHeight());

                        if (primaryStage.getWidth() > primaryStage.getHeight()) {
                            width = (int) primaryStage.getWidth();
                            height = (int) (primaryStage.getWidth() / aspect);
                        } else {
                            width = (int) (primaryStage.getHeight() * aspect);
                            height = (int) (primaryStage.getHeight());
                        }

                        mediaPlayerComponent.resizePlayer();
                    }
                };
                // schedule a new task
                timer.schedule(task, delayTime);
            }
        };

        aspect = primaryStage.getWidth() / primaryStage.getHeight();

        primaryStage.widthProperty().addListener(listener);
        primaryStage.heightProperty().addListener(listener);

        fps = directMediaPlayer.getFps();
        isAssumedFps = fps == 0;
        pause();

        directMediaPlayer.setTime(0);

        isInitialized = true;

    }

    private class TestMediaPlayerComponent extends DirectMediaPlayerComponent {

        public TestMediaPlayerComponent() {
            super(new TestBufferFormatCallback());
        }

        @Override
        public void display(DirectMediaPlayer mediaPlayer, Memory[] nativeBuffers, BufferFormat bufferFormat) {
            Memory nativeBuffer = nativeBuffers[0];
            ByteBuffer byteBuffer = nativeBuffer.getByteBuffer(0, nativeBuffer.size());
            pixelWriter.setPixels(0, 0, width, height, pixelFormat, byteBuffer, bufferFormat.getPitches()[0]);
        }

        public void resizePlayer() {
            long timeStamp = Datavyu.getVideoController().getCurrentTime();
            boolean isPlaying = directMediaPlayer.isPlaying();
            directMediaPlayer.stop();
            directMediaPlayer.play();
            directMediaPlayer.setTime(timeStamp);

            // TODO: Need to do this differently
            while (!directMediaPlayer.isPlaying()) { }

            logger.info("Was isPlaying: " + isPlaying);
            if (!isPlaying) {
                directMediaPlayer.pause();
            }
        }
    }

    /**
     * Callback to get the buffer format to use for video playback.
     */
    private class TestBufferFormatCallback implements BufferFormatCallback {

        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            int width;
            int height;
            if (useSourceSize) {
                width = sourceWidth;
                height = sourceHeight;
            } else {
                width = VlcApplication.this.width;
                height = VlcApplication.this.height;
            }
            canvas.setWidth(width);
            canvas.setHeight(height);
            stage.setWidth(width);
            stage.setHeight(height);
            return new RV32BufferFormat(width, height);
        }
    }


}
