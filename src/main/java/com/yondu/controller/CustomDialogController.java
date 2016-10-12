package com.yondu.controller;

import com.yondu.App;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by erwin on 10/11/2016.
 */
public class CustomDialogController implements Initializable{

    @FXML
    public Label messageLbl;
    @FXML
    public ImageView rushLogo;
    @FXML
    public Button okayBtn;

    private String message;
    private Pane overlayPane;

    public CustomDialogController(String message, Pane overlayPane) {
        this.message = message;
        this.overlayPane = overlayPane;
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        messageLbl.setText(message);
        messageLbl.setWrapText(true);
        this.rushLogo.setImage(new Image(App.class.getResource("/app/images/rush_logo.png").toExternalForm()));

        okayBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            overlayPane.setVisible(false);
            ((Stage)okayBtn.getScene().getWindow()).close();
        });

    }
}