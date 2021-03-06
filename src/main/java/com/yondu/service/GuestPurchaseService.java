package com.yondu.service;

import com.yondu.App;
import com.yondu.model.ApiResponse;
import com.yondu.model.Employee;
import com.yondu.model.Merchant;
import com.yondu.model.TransactionType;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.yondu.model.constants.ApiConstants.*;
/**
 * Created by lynx on 2/22/17.
 */
public class GuestPurchaseService extends BaseService {

    private ApiService apiService = App.appContextHolder.apiService;

    public void givePoints(String mobileNo, String orNumber, String amount) {

        disableMenu();
        showLoadingScreen();
        PauseTransition pause = new PauseTransition(
                Duration.seconds(.5)
        );
        pause.setOnFinished(event -> {
            Task task = givePointsWorker(mobileNo, orNumber, amount);
            task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    ApiResponse apiResponse = (ApiResponse) task.getValue();
                    if (apiResponse.isSuccess()) {
                        VBox rootVBox = App.appContextHolder.getRootContainer();
                        TextField mobileTextField = (TextField) rootVBox.getScene().lookup("#mobileTextField");
                        TextField receiptTextField = (TextField) rootVBox.getScene().lookup("#receiptTextField");
                        TextField amountTextField = (TextField) rootVBox.getScene().lookup("#amountTextField");
                        mobileTextField.setText(null);
                        receiptTextField.setText(null);
                        amountTextField.setText(null);
                    }
                    hideLoadingScreen();
                    showPrompt(apiResponse.getMessage(), "GUEST EARN POINTS", apiResponse.isSuccess());

                }
            });
            new Thread(task).start();

        });
        pause.play();
    }

    private Task givePointsWorker(String mobileNo, String orNumber, String amount) {
        return new Task() {
            @Override
            protected ApiResponse call() throws Exception {
                ApiResponse apiResponse = new ApiResponse();
                apiResponse.setSuccess(false);

                Employee employee = App.appContextHolder.getEmployee();
                Merchant merchant = App.appContextHolder.getMerchant();

                JSONObject requestBody = new JSONObject();
                requestBody.put("employee_id", employee.getEmployeeId());
                requestBody.put("merchant_type", merchant.getMerchantType());
                requestBody.put("merchant_key", merchant.getUniqueKey());
                requestBody.put("or_no", orNumber);
                requestBody.put("mobile_no", mobileNo);
                requestBody.put("amount", amount);

                String token = merchant.getToken();

                String url = CMS_URL +  GUEST_PURCHASE_ENDPOINT;
                JSONObject jsonObject = apiService.callWidget(url, requestBody.toJSONString(), "post", token);
                if (jsonObject != null) {
                    if (jsonObject.get("error_code").equals("0x0")) {
                        apiResponse.setSuccess(true);
                        apiResponse.setMessage("Earn points successful.");
                        saveTransaction(TransactionType.EARN_POINTS_GUEST, mobileNo, employee.getEmployeeName(), amount, orNumber, null);
                    } else {
                        apiResponse.setMessage((String) jsonObject.get("message"));
                    }
                } else {
                    apiResponse.setMessage("Network error.");
                }
                return apiResponse;
            }
        };
    }
}
