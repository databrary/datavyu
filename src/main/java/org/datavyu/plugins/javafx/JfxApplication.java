package org.datavyu.plugins.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.Datavyu;
import org.datavyu.util.FrameRate;
import org.datavyu.views.VideoController;

import java.io.File;

public class JfxApplication extends Application {

    private static Logger logger = LogManager.getFormatterLogger(JfxApplication.class);
    private File dataFile;
    private boolean init = false;
    private MediaPlayer mp;
    private MediaView mv;
    private Stage stage;
    private long duration = -1;
    private long lastSeekTime = -1;

    public JfxApplication(File file) {
        dataFile = file;
    }

    public void seek(long time) {
        if (lastSeekTime != time) {
            logger.info("Seeking to: " + time);
            double rate = 0;
            if (mp.getRate() != 0) {
                mp.pause();
                rate = mp.getRate();
            }
            // NOTE: JavaFX only seems to be able to setCurrentTime accurately in 2.2 when the rate != 0, so lets fake that here.
            mp.setRate(1);
            mp.seek(Duration.millis(time));
            mp.setRate(rate);
            lastSeekTime = time;
        }
    }

    public void pause() {
        logger.info("Pausing at time: " + mp.getCurrentTime());
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
        return FrameRate.createDefaultFrameRate(dataFile).getFPS();
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

    public boolean isPlaying() { return mp.getStatus() == MediaPlayer.Status.PLAYING; }

    public void setScale(double scale) {
        logger.info("Setting scale to %2.2f", scale);
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

        final Media m = new Media(dataFile.toURI().toString());
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

                final Scene scene = new Scene(root, mp.getMedia().getWidth(), mp.getMedia().getHeight());
                scene.setFill(Color.BLACK);

                primaryStage.setScene(scene);
                primaryStage.setTitle(dataFile.getName());
                primaryStage.show();

                double aspect = ((double)mp.getMedia().getWidth()) / mp.getMedia().getHeight();
                System.out.println(aspect);
                stage.minWidthProperty().bind(stage.heightProperty().multiply(aspect));
                stage.maxWidthProperty().bind(stage.heightProperty().multiply(aspect));

                init = true;
                handler();
            }
        });
    }

    private void handler(){
        stage.getScene().setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                VideoController videoController = Datavyu.getVideoController();
                switch (event.getCode()){
                    case DIVIDE: {
                        if(Datavyu.getPlatform().equals(Datavyu.Platform.MAC)){
                            videoController.pressShowTracksSmall();
                        }else{
                            videoController.pressPointCell();
                        }
                        break;
                    }
                    case EQUALS:{
                        if(Datavyu.getPlatform().equals(Datavyu.Platform.MAC)){
                            videoController.pressPointCell();
                        }
                        break;
                    }
                    case MULTIPLY:{
                        if (!Datavyu.getPlatform().equals(Datavyu.Platform.MAC)) {
                            videoController.pressShowTracksSmall();
                        }
                        break;
                    }
                    case NUMPAD7:{
                        videoController.pressSetCellOnset();
                        break;
                    }
                    case NUMPAD8:{
                        videoController.pressPlay();
                        break;
                    }
                    case NUMPAD9:{
                        videoController.pressSetCellOffsetNine();
                        break;
                    }
                    case NUMPAD4:{
                        videoController.pressShuttleBack();
                        break;
                    }
                    case NUMPAD5:{
                        videoController.pressStop();
                        break;
                    }
                    case NUMPAD6:{
                        videoController.pressShuttleForward();
                        break;
                    }
                    case NUMPAD1:{
                        videoController.jogBackAction();
                        break;
                    }
                    case NUMPAD2:{
                        videoController.pressPause();
                        break;
                    }
                    case NUMPAD3:{
                        videoController.jogForwardAction();
                        break;
                    }
                    case NUMPAD0:{
                        videoController.pressCreateNewCellSettingOffset();
                        break;
                    }
                    case DECIMAL:{
                        videoController.pressSetCellOffsetPeriod();
                        break;
                    }
                    case SUBTRACT:{
                        if(event.getCode() == KeyCode.CONTROL){
                            videoController.clearRegionOfInterestAction();
                        } else {
                            videoController.pressGoBack();
                        }
                    }
                    case ADD:{
                        if (event.getCode() == KeyCode.SHIFT){
                            videoController.pressFind();
                            videoController.findOffsetAction();
                        } else if(event.getCode() == KeyCode.CONTROL){
                            videoController.pressFind();
                            videoController.setRegionOfInterestAction();
                        } else {
                            videoController.pressFind();
                        }
                    }
                    case ENTER:{
                        videoController.pressCreateNewCell();
                        break;
                    }
                    default:{
                        break;
                    }
                }
            }
        });
    }
}
