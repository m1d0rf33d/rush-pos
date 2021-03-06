package com.yondu.controller;

import com.sun.javafx.scene.control.skin.FXVK;
import com.yondu.App;
import com.yondu.model.constants.AppConfigConstants;
import com.yondu.model.constants.AppState;
import com.yondu.service.LoginService;
import com.yondu.utils.PropertyBinder;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

import static com.yondu.model.constants.AppConfigConstants.*;

/**
 * Created by lynx on 2/24/17.
 */
public class LoginOfflineController extends BaseController implements Initializable {
    @FXML
    public Button givePointsButton;
    @FXML
    public TextField mobileTextField;
    @FXML
    public TextField receiptTextField;
    @FXML
    public TextField amountTextField;
    @FXML
    public Button ocrButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        if (!readOcrConfig()) {
            ocrButton.setVisible(false);
        }
        //Event handlers for clickable nodes
        mobileTextField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue)
            {
                if (newPropertyValue)
                    FXVK.detach();
            }

        });
        //Event handlers for clickable nodes
        receiptTextField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue)
            {
                if (newPropertyValue)
                    FXVK.detach();
            }

        });
        //Event handlers for clickable nodes
        amountTextField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue)
            {
                if (newPropertyValue)
                    FXVK.detach();
            }

        });

        PropertyBinder.bindAmountOnly(amountTextField);
        PropertyBinder.addComma(amountTextField);

        givePointsButton.setOnMouseClicked((MouseEvent e) -> {
            saveOfflineTransaction();
        });

        ocrButton.setOnMouseClicked((MouseEvent e) -> {
            App.appContextHolder.ocrService.triggerOCR();
        });
    }


    private void saveOfflineTransaction() {
        try {

            String valid = validateFields();
            if (valid.isEmpty()) {
                File file = new File(RUSH_HOME+ DIVIDER + "temp.txt");
                if (!file.exists()) {
                    file.createNewFile();
                }
                SimpleDateFormat df  = new SimpleDateFormat("MM/dd/YYYY");
                String date = df.format(new Date());

                PrintWriter fstream = new PrintWriter(new FileWriter(file,true));
                String line = "mobileNumber=" + mobileTextField.getText().replace(":", "")+
                        ":totalAmount=" + amountTextField.getText().replace(":", "") +
                        ":orNumber=" + receiptTextField.getText().replace(":", "") +
                        ":date=" + date +
                        ":status=Pending:message= ";
                byte[] encodedBytes = org.apache.commons.codec.binary.Base64.encodeBase64(line.getBytes());
                fstream.println(new String(encodedBytes));
                fstream.flush();
                fstream.close();

                mobileTextField.setText(null);
                amountTextField.setText(null);
                receiptTextField.setText(null);
                valid = "Offline transaction saved.";
            }
            Text text = new Text(valid);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
            alert.setTitle(AppConfigConstants.APP_TITLE);
            alert.initStyle(StageStyle.UTILITY);
            alert.initOwner(givePointsButton.getScene().getWindow());
            alert.getDialogPane().setPadding(new javafx.geometry.Insets(10,10,10,10));
            alert.getDialogPane().setContent(text);
            alert.getDialogPane().setPrefWidth(400);
            alert.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String validateFields() {
        String mobileNumber = mobileTextField.getText();
        String orNumber = receiptTextField.getText();
        String amount = amountTextField.getText();

        String errorMessage = "";
        if (mobileNumber == null || (mobileNumber != null && mobileNumber.isEmpty())) {
            errorMessage = "Mobile number is required.";
        }

        if (orNumber == null || (orNumber != null && orNumber.isEmpty())) {
            errorMessage = "Receipt number is required.";
        }
        if (amount == null || (amount != null && amount.isEmpty())) {
            errorMessage = "Amount is required";
        }
        return errorMessage;
    }

}
