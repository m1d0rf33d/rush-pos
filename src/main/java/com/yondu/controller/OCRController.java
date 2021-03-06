package com.yondu.controller;

import com.yondu.App;
import com.yondu.model.OcrConfig;
import com.yondu.model.constants.AppConfigConstants;
import com.yondu.service.OcrService;
import com.yondu.service.RouteService;
import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;

import static com.yondu.model.constants.AppConfigConstants.*;

/**
 * Created by lynx on 2/15/17.
 */
public class OCRController implements Initializable {

    @FXML
    public ImageView orPosImageView;
    @FXML
    public ImageView amountPosImageView;
    @FXML
    public Button saveButton;
    @FXML
    public ImageView orPreviewImageView;
    @FXML
    public ImageView amountPreviewImageView;
    @FXML
    public ImageView previewImageView;
    @FXML
    public Label previewLabel;
    @FXML
    public Label errorLabel;
    @FXML
    public ToggleButton onToggle;
    @FXML
    public ToggleButton offToggle;

    private Double orPosX;
    private Double orPosY;
    private Double orWidth;
    private Double orHeight;
    private Double amountPosX;
    private Double amountPosY;
    private Double amountWidth;
    private Double amountHeight;

    private Double savedOrPosX;
    private Double savedOrPosY;
    private Double savedOrWidth;
    private Double savedOrHeight;
    private Double savedAmountPosX;
    private Double savedAmountPosY;
    private Double savedAmountWidth;
    private Double savedAmountHeight;

    private ToggleGroup toggleGroup = new ToggleGroup();
    private RouteService routeService = App.appContextHolder.routeService;
    private OcrService ocrService = App.appContextHolder.ocrService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        File file = new File(RUSH_HOME + DIVIDER + OCR_CONFIG);
        if (!file.exists()) {
            try {
                file.createNewFile();
                PrintWriter fstream = new PrintWriter(new FileWriter(file));
                fstream.println("enabled=" + "on");
                fstream.flush();
                fstream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        onToggle.setUserData("on");
        offToggle.setUserData("off");
        onToggle.setToggleGroup(toggleGroup);
        offToggle.setToggleGroup(toggleGroup);
        toggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>(){
            public void changed(ObservableValue<? extends Toggle> ov,
                                Toggle toggle, Toggle newToggle) {

                if(newToggle == null && toggle == onToggle) {
                    toggleGroup.selectToggle(offToggle);
                } else if (newToggle == null && toggle == offToggle){
                    toggleGroup.selectToggle(onToggle);
                }

                if (onToggle.isSelected()) {
                    onToggle.setStyle("-fx-background-color: #ffb600");
                    offToggle.setStyle("-fx-background-color: #333");
                }
                if (offToggle.isSelected()) {
                    offToggle.setStyle("-fx-background-color: #ffb600");
                    onToggle.setStyle("-fx-background-color: #333");
                }
            }
        });

        String ocrEnabled = readOcrConfig();
        if (ocrEnabled.equals("on")) {
            toggleGroup.selectToggle(onToggle);
        } else {
            toggleGroup.selectToggle(offToggle);
        }

        errorLabel.setVisible(false);
        loadSavedConfig();

        orPosImageView.setImage(new javafx.scene.image.Image(App.class.getResource("/app/images/edit.png").toExternalForm()));
        amountPosImageView.setImage(new javafx.scene.image.Image(App.class.getResource("/app/images/edit.png").toExternalForm()));
        orPreviewImageView.setImage(new javafx.scene.image.Image(App.class.getResource("/app/images/preview.png").toExternalForm()));
        amountPreviewImageView.setImage(new javafx.scene.image.Image(App.class.getResource("/app/images/preview.png").toExternalForm()));


        orPosImageView.setOnMouseClicked((MouseEvent e) -> {

            if (App.appContextHolder.getOrCaptureStage() != null) {
                orPosImageView.setImage(new javafx.scene.image.Image(App.class.getResource("/app/images/edit.png").toExternalForm()));
                Stage stage = App.appContextHolder.getOrCaptureStage();
                orPosX = stage.getX();
                orPosY = stage.getY();
                orWidth = stage.getWidth();
                orHeight = stage.getHeight();
                String pos = orPosX + " " + orPosY + " "  + orWidth + " " + orHeight;

                App.appContextHolder.getOrCaptureStage().close();
                App.appContextHolder.setOrCaptureStage(null);

            } else {
                orPosImageView.setImage(new javafx.scene.image.Image(App.class.getResource("/app/images/save.png").toExternalForm()));
                routeService.loadOrCaptureScreen();
            }
        });

        amountPosImageView.setOnMouseClicked((MouseEvent e) -> {

            if (App.appContextHolder.getSalesCaptureStage() != null) {
                amountPosImageView.setImage(new javafx.scene.image.Image(App.class.getResource("/app/images/edit.png").toExternalForm()));
                Stage stage = App.appContextHolder.getSalesCaptureStage();
                amountPosX = stage.getX();
                amountPosY = stage.getY();
                amountWidth = stage.getWidth();
                amountHeight = stage.getHeight();
                String pos = amountPosX + " " + amountPosY + " "  + amountWidth + " " + amountHeight;

                App.appContextHolder.getSalesCaptureStage().close();
                App.appContextHolder.setSalesCaptureStage(null);

            } else {
                amountPosImageView.setImage(new javafx.scene.image.Image(App.class.getResource("/app/images/save.png").toExternalForm()));
                routeService.loadSalesCaptureScreen();
            }
        });

        saveButton.setOnMouseClicked((MouseEvent e) -> {
            App.appContextHolder.getRootContainer().setOpacity(.50);
            for (Node n : App.appContextHolder.getRootContainer().getChildren()) {
                n.setDisable(true);
            }
            PauseTransition pause = new PauseTransition(
                    Duration.seconds(.01)
            );
            pause.setOnFinished(event -> {
                saveDimensions();
                saveOcrConfig();
                App.appContextHolder.getRootContainer().setOpacity(1);
                for (Node n : App.appContextHolder.getRootContainer().getChildren()) {
                    n.setDisable(false);
                }

            });
            pause.play();
        });

        orPreviewImageView.setOnMouseClicked((MouseEvent e) -> {
            Double x = orPosX != null ? orPosX : savedOrPosX;
            Double y = orPosY != null ? orPosY : savedOrPosY;
            Double width = orWidth != null ? orWidth : savedOrWidth;
            Double height = orHeight != null ? orHeight : savedOrHeight;
            App.appContextHolder.getRootContainer().setOpacity(.50);

            OcrConfig config = new OcrConfig();
            config.setOrNumberHeight(height.intValue());
            config.setOrNumberPosX(x.intValue());
            config.setOrNumberPosY(y.intValue());
            config.setOrNumberWidth(width.intValue());
            ocrService.preview(config, "or");

        });

        amountPreviewImageView.setOnMouseClicked((MouseEvent e) -> {
            Double x = amountPosX != null ? amountPosX : savedAmountPosX;
            Double y = amountPosY != null ? amountPosY : savedAmountPosY;
            Double width = amountWidth != null ? amountWidth : savedAmountWidth;
            Double height = amountHeight != null ? amountHeight : savedAmountHeight;

            OcrConfig config = new OcrConfig();
            config.setSalesHeight(height.intValue());
            config.setSalesPosX(x.intValue());
            config.setSalesPosY(y.intValue());
            config.setSalesWidth(width.intValue());
            ocrService.preview(config, "sales");

        });
    }

    public String readOcrConfig() {
        try {
            Properties prop = new Properties();
            InputStream inputStream = new FileInputStream(new File(RUSH_HOME + DIVIDER + OCR_CONFIG));
            prop.load(inputStream);
            return prop.getProperty("enabled");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveOcrConfig() {
        try {
            File file = new File(RUSH_HOME + DIVIDER + OCR_CONFIG);
            PrintWriter fstream = new PrintWriter(new FileWriter(file));
            fstream.println("enabled=" + toggleGroup.getSelectedToggle().getUserData().toString());
            fstream.flush();
            fstream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveDimensions() {

        Double orX = orPosX != null ? orPosX : savedOrPosX;
        Double orY = orPosY != null ? orPosY : savedOrPosY;
        Double orW = orWidth != null ? orWidth : savedOrWidth;
        Double orH = orHeight != null ? orHeight : savedOrHeight;

        Double amountX = amountPosX != null ? amountPosX : savedAmountPosX;
        Double amountY = amountPosY != null ? amountPosY : savedAmountPosY;
        Double amountW = amountWidth != null ? amountWidth : savedAmountWidth;
        Double amountH = amountHeight != null ? amountHeight : savedAmountHeight;

        if (orX != null && amountX != null) {
            try {
                File file = new File(RUSH_HOME + DIVIDER + OCR_PROPERTIES);

                if (!file.exists()) {
                    file.createNewFile();
                }


                    PrintWriter fstream = new PrintWriter(new FileWriter(file));
                    fstream.println("sales_pos_x=" + amountX);
                    fstream.println("sales_pos_y=" + amountY);
                    fstream.println("sales_width=" + amountW);
                    fstream.println("sales_height=" + amountH);
                    fstream.println("or_pos_x=" + orX);
                    fstream.println("or_pos_y=" + orY);
                    fstream.println("or_width=" + orW);
                    fstream.println("or_height=" + orH);
                    fstream.flush();
                    fstream.close();

                    Text text = new Text("OCR settings saved.");
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
                    alert.setTitle(AppConfigConstants.APP_TITLE);
                    alert.initStyle(StageStyle.UTILITY);
                    alert.initOwner(App.appContextHolder.getRootContainer().getScene().getWindow());
                    alert.setHeaderText("OCR SETUP");
                    alert.getDialogPane().setPadding(new javafx.geometry.Insets(10,10,10,10));
                    alert.getDialogPane().setContent(text);
                    alert.getDialogPane().setPrefWidth(400);
                    alert.show();


            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            errorLabel.setVisible(true);
        }
    }

    private void loadSavedConfig() {
        try {
            Properties prop = new Properties();
            InputStream inputStream = new FileInputStream(new File(RUSH_HOME + DIVIDER + OCR_PROPERTIES));
            prop.load(inputStream);

            savedAmountPosX = prop.getProperty("sales_pos_x").isEmpty() ? null : Double.parseDouble(prop.getProperty("sales_pos_x"));
            savedAmountPosY = prop.getProperty("sales_pos_y").isEmpty() ? null : Double.parseDouble(prop.getProperty("sales_pos_y"));
            savedAmountWidth = prop.getProperty("sales_width").isEmpty() ? null : Double.parseDouble(prop.getProperty("sales_width"));
            savedAmountHeight = prop.getProperty("sales_height").isEmpty() ? null : Double.parseDouble(prop.getProperty("sales_height"));

            savedOrPosX = prop.getProperty("or_pos_x").isEmpty() ? null : Double.parseDouble(prop.getProperty("or_pos_x"));
            savedOrPosY = prop.getProperty("or_pos_y").isEmpty() ? null : Double.parseDouble(prop.getProperty("or_pos_y"));
            savedOrWidth = prop.getProperty("or_width").isEmpty() ? null : Double.parseDouble(prop.getProperty("or_width"));
            savedOrHeight = prop.getProperty("or_height").isEmpty() ? null : Double.parseDouble(prop.getProperty("or_height"));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
