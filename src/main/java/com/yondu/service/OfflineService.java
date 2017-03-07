package com.yondu.service;

import com.yondu.App;
import com.yondu.model.*;
import com.yondu.model.constants.AppConfigConstants;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.yondu.model.constants.ApiConstants.*;
import static com.yondu.model.constants.AppConfigConstants.*;

/**
 * Created by lynx on 2/22/17.
 */
public class OfflineService extends BaseService {

    private ApiService apiService = App.appContextHolder.apiService;
    private ObservableList<OfflineTransaction> masterData = FXCollections.observableArrayList();

    public void initialize() {
        disableMenu();
        PauseTransition pause = new PauseTransition(
                Duration.seconds(.5)
        );
        pause.setOnFinished(event -> {
            Task task = initializeWorker();
            task.setOnSucceeded((Event e) -> {

                renderOfflineTable();
                enableMenu();
            });
            new Thread(task).start();
        });
        pause.play();
    }

    public Task initializeWorker() {
        return new Task() {
            @Override
            protected Object call() throws Exception {

                loadOfflineTransactions();
                return null;
            }
        };
    }

    public void givePoints() {

        disableMenu();
        PauseTransition pause = new PauseTransition(
                Duration.seconds(.5)
        );
        pause.setOnFinished(event -> {
            Task task = givePointsWorker();
            task.setOnSucceeded((Event e) -> {
                showPrompt("Submit offline transactions complete.", "OFFLINE TRANSACTION");
                initialize();
            });
            new Thread(task).start();
        });
        pause.play();
    }

    public Task givePointsWorker() {
        return new Task() {
            @Override
            protected ApiResponse call() throws Exception {
                ApiResponse apiResponse = new ApiResponse();
                apiResponse.setSuccess(false);
                File file = new File(RUSH_HOME + DIVIDER + AppConfigConstants.OFFLINE_TRANSACTION_FILE);

                if (file.exists()) {
                    //Read file
                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        String line;

                        List<String> transactions = new ArrayList<>();

                        while ((line = br.readLine()) != null) {
                            byte[] decoded = org.apache.commons.codec.binary.Base64.decodeBase64(line.getBytes());
                            line = new String(decoded);
                            String[] arr = line.split(":");

                            String mobileNumber = arr[0].split("=")[1];
                            String totalAmount = arr[1].split("=")[1];
                            String orNumber = arr[2].split("=")[1];
                            String date = arr[3].split("=")[1];
                            String status = arr[4].split("=")[1];
                            String message = arr[5].split("=")[1];

                            OfflineTransaction offlineTransaction = new OfflineTransaction();
                            offlineTransaction.setAmount(totalAmount);
                            offlineTransaction.setMobileNumber(mobileNumber);
                            offlineTransaction.setOrNumber(orNumber);
                            offlineTransaction.setDate(date);
                            offlineTransaction.setStatus(status);
                            offlineTransaction.setMessage(message);

                            if (status.equalsIgnoreCase("Pending")) {
                                offlineTransaction = sendPoints(offlineTransaction);
                            }


                            String l = "mobileNumber=" + offlineTransaction.getMobileNumber()+
                                    ":totalAmount=" + offlineTransaction.getAmount() +
                                    ":orNumber=" + offlineTransaction.getOrNumber() +
                                    ":date=" + offlineTransaction.getDate() +
                                    ":status=" + offlineTransaction.getStatus() +
                                    ":message=" + offlineTransaction.getMessage();

                            transactions.add(l);

                        }

                        PrintWriter writer = new PrintWriter(file);
                        writer.print("");
                        writer.close();

                        PrintWriter fstream = new PrintWriter(new FileWriter(file,true));
                        for (String trans : transactions) {
                            byte[] encodedBytes = org.apache.commons.codec.binary.Base64.encodeBase64(trans.getBytes());
                            fstream.println(new String(encodedBytes));
                        }

                        fstream.flush();
                        fstream.close();
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return apiResponse;
            }
        };
    }


    private void renderOfflineTable() {


        VBox rootVBox = App.appContextHolder.getRootContainer();
        Pagination offlinePagination = (Pagination) rootVBox.getScene().lookup("#offlinePagination");

        offlinePagination.setPageCount(0);
        offlinePagination.setPageFactory((Integer pageIndex) -> createOfflinePage(pageIndex));
    }

    public Node createOfflinePage(int pageIndex) {

        VBox box = new VBox();
        box.getChildren().addAll(buildTableView());
        return box;
    }

    private TableView<OfflineTransaction> buildTableView() {


        VBox rootVBox = App.appContextHolder.getRootContainer();
        TextField searchTextField = (TextField) rootVBox.getScene().lookup("#searchTextField");
        Pagination offlinePagination = (Pagination) rootVBox.getScene().lookup("#offlinePagination");
        MenuButton statusMenuButton = (MenuButton) rootVBox.getScene().lookup("#statusMenuButton");

        TableView<OfflineTransaction> transactionsTableView = new TableView();
        transactionsTableView.setFixedCellSize(Region.USE_COMPUTED_SIZE);

        ObservableList<OfflineTransaction> finalData = FXCollections.observableArrayList();
        ObservableList<OfflineTransaction> textFilteredData = FXCollections.observableArrayList();

        if (masterData != null) {
            if (searchTextField.getText() != null && !searchTextField.getText().isEmpty()) {
                String searchTxt = searchTextField.getText().toLowerCase();
                for (OfflineTransaction ot : masterData) {
                    String temp = "";
                    temp = temp + (ot.getMobileNumber() != null ? ot.getMobileNumber() : "");
                    temp = temp +(ot.getStatus() != null ? ot.getStatus() : "");
                    temp = temp + (ot.getAmount() != null ? ot.getAmount() : "");
                    temp = temp + (ot.getDate() != null ? ot.getDate() : "");
                    temp = temp + (ot.getMessage() != null ? ot.getMessage() : "");
                    temp =  temp  + (ot.getOrNumber() != null ? ot.getOrNumber() : "");
                    if (temp.toLowerCase().contains(searchTxt)) {
                        textFilteredData.add(ot);
                    }
                }
            } else {
                textFilteredData.addAll(masterData);
            }


            ObservableList<OfflineTransaction> statusFilter = FXCollections.observableArrayList();
            if (!statusMenuButton.getText().equalsIgnoreCase("All")) {
                String status = statusMenuButton.getText().toLowerCase();
                for (OfflineTransaction ot : textFilteredData) {
                    if (ot.getStatus().equalsIgnoreCase(status)) {
                        statusFilter.addAll(ot);
                    }
                }
            } else {
                statusFilter = textFilteredData;
            }

            int maxEntries = 10;
            int pageCount = statusFilter.size() / maxEntries;
            if (pageCount == 0) {
                pageCount = 1;
            } else {
                if (statusFilter.size() % maxEntries > 0) {
                    pageCount++;
                }
            }

            offlinePagination.setPageCount(pageCount);
            int pageIndex = offlinePagination.getCurrentPageIndex();
            ObservableList<OfflineTransaction> indexFilteredData = FXCollections.observableArrayList();
            for (OfflineTransaction transaction : statusFilter) {
                int objIndex = textFilteredData.indexOf(transaction);
                if (objIndex >= (pageIndex * maxEntries)  && objIndex < ((pageIndex + 1) * maxEntries)) {
                    indexFilteredData.add(transaction);
                }
                if (objIndex > ((pageIndex + 1) * maxEntries -1)) {
                    break;
                }
            }
            finalData = indexFilteredData;
        }
        buildOfflineColumns(transactionsTableView);
        transactionsTableView.setItems(finalData);
        return transactionsTableView;
    }

    private void buildOfflineColumns(TableView tableView) {


        TableColumn dateCol = new TableColumn("Date");
        dateCol.setPrefWidth(100);
        dateCol.setCellValueFactory(
                new PropertyValueFactory<>("date"));

        TableColumn mobileCol = new TableColumn("Mobile Number");
        mobileCol.setPrefWidth(150);
        mobileCol.setCellValueFactory(
                new PropertyValueFactory<>("mobileNumber"));

        TableColumn orCol = new TableColumn("OR Number");
        orCol.setPrefWidth(150);
        orCol.setCellValueFactory(
                new PropertyValueFactory<>("orNumber"));


        TableColumn statusCol = new TableColumn("Status");
        statusCol.setPrefWidth(100);
        statusCol.setCellValueFactory(
                new PropertyValueFactory<>("status"));

        TableColumn messageCol = new TableColumn("Message");
        messageCol.setPrefWidth(300);
        messageCol.setCellValueFactory(
                new PropertyValueFactory<>("message"));

        TableColumn amountCol = new TableColumn("Amount");
        amountCol.setPrefWidth(100);
        amountCol.setCellValueFactory(
                new PropertyValueFactory<>("amount"));
        tableView.getColumns().clear();
        tableView.getColumns().addAll(dateCol, mobileCol,amountCol, orCol, statusCol, messageCol);
    }

    public void loadOfflineTransactions() {
        masterData = FXCollections.observableArrayList();
        File file = new File(RUSH_HOME + DIVIDER + OFFLINE_TRANSACTION_FILE);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    byte[] decoded = org.apache.commons.codec.binary.Base64.decodeBase64(line.getBytes());
                    line = new String(decoded);
                    String[] arr = line.split(":");

                    OfflineTransaction offlineTransaction = new OfflineTransaction();
                    offlineTransaction.setMobileNumber(arr[0].split("=")[1]);
                    offlineTransaction.setAmount(arr[1].split("=")[1]);
                    offlineTransaction.setOrNumber(arr[2].split("=")[1]);
                    offlineTransaction.setDate(arr[3].split("=")[1]);
                    offlineTransaction.setStatus(arr[4].split("=")[1]);
                    offlineTransaction.setMessage(arr[5].split("=")[1]);
                    masterData.add(offlineTransaction);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    public OfflineTransaction sendPoints(OfflineTransaction offlineTransaction) {

        SimpleDateFormat df  = new SimpleDateFormat("MM/dd/YYYY");
        String date = df.format(new Date());
        offlineTransaction.setDate(date);

        offlineTransaction.setMessage("");


        Employee employee = App.appContextHolder.getEmployee();

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("mobile_no", offlineTransaction.getMobileNumber()));
        String url = BASE_URL + MEMBER_LOGIN_ENDPOINT;

        url = url.replace(":employee_id", employee.getEmployeeId());
        JSONObject jsonObject = apiService.call(url, params, "post", MERCHANT_APP_RESOURCE_OWNER);
        if (jsonObject != null) {
            if (jsonObject.get("error_code").equals("0x0")) {
                JSONObject data = (JSONObject) jsonObject.get("data");
                params = new ArrayList<>();
                params.add(new BasicNameValuePair("employee_id", employee.getEmployeeId()));
                params.add(new BasicNameValuePair("or_no", offlineTransaction.getOrNumber()));
                params.add(new BasicNameValuePair("amount", offlineTransaction.getAmount().replace(",", "")));
                url = BASE_URL + GIVE_POINTS_ENDPOINT;
                url = url.replace(":customer_uuid", (String) data.get("id"));
                url = url.replace(":employee_id", employee.getEmployeeId());
                JSONObject json = apiService.call(url, params, "post", MERCHANT_APP_RESOURCE_OWNER);

                if (json != null) {
                    if (!json.get("error_code").equals("0x0")) {
                        JSONObject error = (JSONObject) json.get("errors");
                        String errorMessage = "";
                        if (error != null) {
                            if (error.get("or_no") != null) {
                                List<String> l = (ArrayList<String>) error.get("or_no");
                                errorMessage = l.get(0);
                            }
                            if (error.get("amount") != null) {
                                List<String> l = (ArrayList<String>) error.get("amount");
                                errorMessage = l.get(0);
                            }
                        }
                        if (json.get("message") != null) {
                            errorMessage = (String) json.get("message");
                        }
                        offlineTransaction.setMessage(errorMessage);
                        offlineTransaction.setStatus("Failed");
                    } else {
                        offlineTransaction.setStatus("Submitted");
                        offlineTransaction.setMessage("Points earned");
                    }
                } else {
                    offlineTransaction.setMessage((String) jsonObject.get("Network error"));
                    offlineTransaction.setStatus("Pending");
                }
            } else {
                offlineTransaction.setMessage((String) jsonObject.get("message"));
                offlineTransaction.setStatus("Failed");
            }
        } else {
            offlineTransaction.setMessage((String) jsonObject.get("Network error"));
            offlineTransaction.setStatus("Pending");
        }
        return offlineTransaction;
    }
}
