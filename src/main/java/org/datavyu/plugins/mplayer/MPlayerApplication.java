package org.datavyu.plugins.mplayer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.plugins.javafx.JfxStreamViewerDialog;

import java.io.File;

public class MPlayerApplication extends Application {

    private static Logger logger = LogManager.getLogger(JfxStreamViewerDialog.class);

    private File sourceFile;
    private boolean init = false;
    private MediaPlayer mp;
    private MediaView mv;
    private Stage stage;
    private long duration = -1;
    private long lastSeekTime = -1;

    public MPlayerApplication(final File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void seek(long time) {
        if (lastSeekTime != time) {
            logger.info("Seeking to: " + time);

            double rate = 0;
            if (mp.getCurrentRate() != 0) {
                mp.pause();
                rate = mp.getCurrentRate();
            }

            // NOTE: JavaFX only seems to be able to setCurrentTime accurately in 2.2 when the rate != 0,
            // so lets fake that here.
            mp.setRate(1);
            mp.seek(Duration.millis(time));
            mp.setRate(rate);
            lastSeekTime = time;
        }
    }

    public void pause() {
        logger.info("Pausing at time: "+  mp.getCurrentTime());
        mp.pause();
    }

    public void play() {
        mp.play();
    }

    public void stop() {
        mp.stop();
    }

    public long getCurrentTime() {
        return (long) mp.getCurrentTime().toMillis();
    }

    public float getFrameRate() {
        return (float) 30;
    }

    public long getDuration() {
        if (duration == -1) {
            duration = (long) mp.getTotalDuration().toMillis();
        }
        return duration;
    }

    public float getRate() {
        return (float) mp.getRate();
    }

    public void setRate(float rate) {
        mp.setRate((double) rate);
    }

    public double getHeight() {
        return mp.getMedia().getHeight();
    }

    public double getAspectRatio() {
        return mp.getMedia().getWidth() / mp.getMedia().getHeight();
    }

    public void setScale(double scale) {
        stage.setHeight(mp.getMedia().getHeight() * scale);
        stage.setWidth(mp.getMedia().getWidth() * scale);
    }

    public void setVisible(final boolean visible) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                mv.setVisible(visible);
                if (!visible) {
                    mp.setMute(true);
                    stage.hide();
                } else {
                    mp.setMute(false);
                    stage.show();
                }
            }
        });

    }

    public void setVolume(double volume) {
        mp.setVolume(volume);
    }

    public boolean isInit() {
        return init;
    }

    public void closeAndDestroy() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                stage.close();
            }
        });

    }

    public void start(final Stage primaryStage) {
        stage = primaryStage;

        final Media m = new Media(sourceFile.toURI().toString());
        mp = new MediaPlayer(m);
        mp.setOnReady(new Runnable() {
            @Override
            public void run() {
                logger.info("Creating a new media view.");
                mv = new MediaView(mp);


                final DoubleProperty width = mv.fitWidthProperty();
                final DoubleProperty height = mv.fitHeightProperty();

                width.bind(Bindings.selectDouble(mv.sceneProperty(), "width"));
                height.bind(Bindings.selectDouble(mv.sceneProperty(), "height"));


                mv.setPreserveRatio(true);

                StackPane root = new StackPane();
                root.getChildren().add(mv);

                final Scene scene = new Scene(root, 960, 540);
                scene.setFill(Color.BLACK);

                primaryStage.setScene(scene);
                primaryStage.setTitle(sourceFile.getName());
                primaryStage.show();

                init = true;
            }
        });


    }

}
