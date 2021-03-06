package com.yondu.controller;

import com.yondu.App;
import com.yondu.service.*;
import com.yondu.utils.PropertyBinder;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by lynx on 2/10/17.
 */
public class PayWithPointsController  extends BaseController implements Initializable {
    @FXML
    public Label nameLabel;
    @FXML
    public Label memberIdLabel;
    @FXML
    public Label mobileNumberLabel;
    @FXML
    public Label membershipDateLabel;
    @FXML
    public Label genderLabel;
    @FXML
    public Label birthdateLabel;
    @FXML
    public Label emailLabel;
    @FXML
    public Label pointsLabel;
    @FXML
    public Label pesoValueLabel;
    @FXML
    public TextField pointsTextField;
    @FXML
    public TextField receiptTextField;
    @FXML
    public TextField amountTextField;
    @FXML
    public Button clearButton;
    @FXML
    public Button submitButton;
    @FXML
    public Button ocrButton;
    @FXML
    public Button exitButton;

    private RouteService routeService = App.appContextHolder.routeService;
    private OcrService ocrService = App.appContextHolder.ocrService;
    private CommonService commonService = App.appContextHolder.commonService;
    public PayWithPointsService payWithPointsService = App.appContextHolder.payWithPointsService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        PropertyBinder.bindVirtualKeyboard(pointsTextField);
        PropertyBinder.bindVirtualKeyboard(receiptTextField);
        PropertyBinder.bindVirtualKeyboard(amountTextField);

        payWithPointsService.initialize();


        if (!readOcrConfig()) {
            ocrButton.setVisible(false);
        }

        exitButton.setOnMouseClicked((MouseEvent e) -> {
            commonService.exitMember();
        });


        PropertyBinder.bindAmountOnly(amountTextField);
        PropertyBinder.addComma(amountTextField);

        PropertyBinder.bindNumberOnly(pointsTextField);

        submitButton.setOnMouseClicked((MouseEvent e) -> {
            routeService.loadPinScreen();
        });

        ocrButton.setOnMouseClicked((MouseEvent e) -> {
            ocrService.triggerOCR();
        });

        clearButton.setOnMouseClicked((MouseEvent e) -> {
            amountTextField.setText(null);
            receiptTextField.setText(null);
            pointsTextField.setText(null);
        });
    }


}
