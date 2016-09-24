package com.yondu.controller;

import com.yondu.App;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static com.yondu.model.AppConfigConstants.*;

/** JavaFx controller mapped to or-capture.fxml
 *
 *  @author m1d0rf33d
 */
public class OrCaptureController implements Initializable{


    @FXML
    public Button captureButton;
    @FXML
    public Pane capturePane;


    private static double xOffset = 0;
    private static double yOffset = 0;


    public void captureOrDimension() {

        Stage stage = (Stage) this.captureButton.getScene().getWindow();
        Double posX = stage.getX(),
                posY = stage.getY(),
                width = stage.getWidth(),
                height = stage.getHeight();

        App.appContextHolder.setOrNumberPosX(posX.intValue());
        App.appContextHolder.setOrNumberPosY(posY.intValue());
        App.appContextHolder.setOrNumberWidth(width.intValue());
        App.appContextHolder.setOrNumberHeight(height.intValue());

        Stage resultStage = new Stage();
        Parent root = null;
        try {
            root = FXMLLoader.load(App.class.getResource(CAPTURE_RESULT_FXML));
        } catch (IOException e) {
            e.printStackTrace();
        }
        resultStage.setScene(new Scene(root, 300,200));
        resultStage.setX(600);
        resultStage.setY(200);
        resultStage.resizableProperty().setValue(false);
        resultStage.show();

        ((Stage) this.captureButton.getScene().getWindow()).close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        capturePane.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                xOffset = ((Stage) captureButton.getScene().getWindow()).getX() - event.getScreenX();
                yOffset = ((Stage) captureButton.getScene().getWindow()).getY() - event.getScreenY();
            }
        });
        capturePane.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                ((Stage) captureButton.getScene().getWindow()).setX(event.getScreenX() + xOffset);
                ((Stage) captureButton.getScene().getWindow()).setY(event.getScreenY() + yOffset);
            }
        });
        captureButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                captureOrDimension();
            }
        });
    }
}
