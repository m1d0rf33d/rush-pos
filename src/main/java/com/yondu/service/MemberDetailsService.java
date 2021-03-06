package com.yondu.service;

import com.google.gson.Gson;
import com.yondu.App;
import com.yondu.model.*;
import com.yondu.model.constants.AppState;
import com.yondu.model.dto.LoginMemberDTO;
import com.yondu.model.dto.MemberDTO;
import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.yondu.model.constants.ApiConstants.*;
/**
 * Created by lynx on 2/21/17.
 */
public class MemberDetailsService extends BaseService{

    private ApiService apiService = App.appContextHolder.apiService;

    public void initialize() {


        Task task = initializeWorker();
        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                ApiResponse apiResponse = (ApiResponse) task.getValue();
                if (apiResponse.isSuccess()) {
                    loadCustomerDetails();
                    loadActiveVouchers();
                    loadCustomerTransactions();

                    VBox vbox = App.appContextHolder.getRootContainer();
                    TabPane vouchersTabPane = (TabPane) vbox.getScene().lookup("#vouchersTabPane");
                    vouchersTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
                    for (Tab tab : vouchersTabPane.getTabs()) {
                        if (tab.getId().equals("transactionsTab")) {
                            Label label = new Label("TRANSACTIONS");
                            label.getStyleClass().add("tab-label");

                            tab.setGraphic(label);
                        } else {
                            Label label = new Label("ACTIVE VOUCHERS");
                            label.getStyleClass().add("tab-label");
                            label.getStyleClass().add("tab-label-selected");
                            tab.setGraphic(label);
                        }
                    }


                    Merchant merchant = App.appContextHolder.getMerchant();
                    Customer customer = App.appContextHolder.getCustomer();
                    if (!merchant.getMerchantClassification().equals("BASIC")) {
                        Label accountNumberLabel = (Label) vbox.getScene().lookup("#accountNumberLabel");
                        Label accountNameLabel = (Label) vbox.getScene().lookup("#accountNameLabel");
                        accountNumberLabel.setText(customer.getAccountNumber());
                        accountNameLabel.setText(customer.getAccountName());
                    }

                    App.appContextHolder.getRootContainer().getScene().setCursor(Cursor.DEFAULT);
                    enableMenu();
                    hideLoadingScreen();
                } else {
                    showPrompt(apiResponse.getMessage(), "MEMBER DETAILS", apiResponse.isSuccess());
                    App.appContextHolder.getRootContainer().getScene().setCursor(Cursor.DEFAULT);

                    hideLoadingScreen();
                }
            }
        });

        new Thread(task).start();
    }

    public void viewMember(String mobileNumber) {
        disableMenu();
        showLoadingScreen();
        App.appContextHolder.getRootContainer().getScene().setCursor(Cursor.WAIT);
        PauseTransition pause = new PauseTransition(
                Duration.seconds(.01)
        );
        pause.setOnFinished(event -> {
            Task task = viewMemberWorker(mobileNumber);
            task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    ApiResponse apiResponse = (ApiResponse) task.getValue();
                    if (apiResponse.isSuccess()) {
                        hideLoadingScreen();
                        App.appContextHolder.routeService.loadMemberDetailsScreen();
                    } else {
                        showPrompt(apiResponse.getMessage(), "MEMBER INQUIRY",apiResponse.isSuccess());
                        hideLoadingScreen();
                        App.appContextHolder.getRootContainer().getScene().setCursor(Cursor.DEFAULT);
                    }

                }
            });

            new Thread(task).start();
        });
        pause.play();
    }

    public Task viewMemberWorker(String mobileNumber) {
        return new Task() {
            @Override
            protected ApiResponse call() throws Exception {

                return loginCustomer(mobileNumber, AppState.MEMBER_INQUIRY);
            }
        };
    }

    public Task initializeWorker() {
        return new Task() {
            @Override
            protected ApiResponse call() throws Exception {
                ApiResponse apiResponse = new ApiResponse();
                apiResponse.setSuccess(false);

                Customer customer = App.appContextHolder.getCustomer();
                ApiResponse loginResponse = loginCustomer(customer.getMobileNumber(), App.appContextHolder.getCurrentState());
                if (loginResponse.isSuccess()) {
                    apiResponse.setSuccess(true);
                } else {
                    showPrompt(apiResponse.getMessage(), "MEMBER DETAILS", apiResponse.isSuccess());
                }
                return apiResponse;
            }
        };
    }

    public ApiResponse loginCustomer(String mobileNumber, AppState appState) {

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setSuccess(false);

        Employee employee = App.appContextHolder.getEmployee();
        Merchant merchant = App.appContextHolder.getMerchant();
        String token = merchant.getToken();

        JSONObject requestBody = new JSONObject();
        requestBody.put("merchant_key", merchant.getUniqueKey());
        requestBody.put("employee_id", employee.getEmployeeId());
        requestBody.put("merchant_type", merchant.getMerchantType());
        requestBody.put("mobile_no", mobileNumber);
        requestBody.put("app_state", appState.toString());

        String url = CMS_URL + VIEW_MEMBER_ENDPOINT;
        JSONObject jsonObject = apiService.callWidget(url, requestBody.toJSONString(), "post", token);
        if (jsonObject != null) {
            if (jsonObject.get("error_code").equals("0x0")) {
                JSONObject data = (JSONObject) jsonObject.get("data");
                JSONObject memberDTO = (JSONObject) data.get("member");

                Customer customer = new Customer();
                customer.setMobileNumber((String) memberDTO.get("mobile_no"));
                customer.setGender((String) memberDTO.get("gender"));
                customer.setMemberId((String) memberDTO.get("profile_id"));
                customer.setName((String) memberDTO.get("name"));
                customer.setDateOfBirth((String) memberDTO.get("birthdate"));
                customer.setEmail((String) memberDTO.get("email"));
                customer.setMemberSince((String) memberDTO.get("registration_date"));
                customer.setUuid((String) memberDTO.get("id"));
                customer.setAvailablePoints((String) memberDTO.get("points"));
                customer.setAccountNumber((String) memberDTO.get("account_number"));
                customer.setAccountName((String) memberDTO.get("account_name"));

                List<JSONObject> rewardsJSON = (ArrayList) data.get("rewards");
                if (rewardsJSON != null) {
                    List<Reward> rewards = new ArrayList<>();
                    for (JSONObject rewardJSON : rewardsJSON) {
                        Reward reward = new Reward();
                        if (rewardJSON.get("redeem_id") != null) {
                            reward.setRedeemId((String) rewardJSON.get("redeem_id"));
                        }
                        Long quantity = (Long) rewardJSON.get("quantity");
                        Long claimed = (Long) rewardJSON.get("claimed");
                        reward.setQuantity(String.valueOf(quantity - claimed));
                        reward.setDate((String) rewardJSON.get("date"));
                        reward.setDetails((String) rewardJSON.get("details"));
                        reward.setId((String) rewardJSON.get("id"));
                        reward.setImageUrl((String) rewardJSON.get("image_url"));
                        reward.setName((String) rewardJSON.get("name"));
                        reward.setPointsRequired(String.valueOf(rewardJSON.get("points_required")));
                        rewards.add(reward);
                    }
                    customer.setActiveVouchers(rewards);
                }
                JSONObject pointsRuleJSON = (JSONObject) data.get("pointsRule");
                if (pointsRuleJSON != null) {
                    PointsRule pointsRule = new PointsRule();
                    pointsRule.setEarningPeso((Long) pointsRuleJSON.get("earning_peso"));
                    pointsRule.setRedeemPeso((Long) pointsRuleJSON.get("redemption_peso"));
                    App.appContextHolder.setPointsRule(pointsRule);
                }

                if (data.get("merchantRewards") != null) {
                    List<JSONObject> merchantRewards = (ArrayList) data.get("merchantRewards");
                    List<Reward> rewards = new ArrayList<>();
                    for (JSONObject rewardJSON : merchantRewards) {
                        Reward reward = new Reward();
                        reward.setImageUrl((String) rewardJSON.get("image_url"));
                        reward.setDetails((String) rewardJSON.get("details"));
                        reward.setName((String) rewardJSON.get("name"));
                        reward.setId((String) rewardJSON.get("id"));
                        reward.setPointsRequired(String.valueOf((Long) rewardJSON.get("points_required")));
                        rewards.add(reward);
                    }
                    merchant.setRewards(rewards);
                }

                if (data.get("transactions") != null) {
                    List<JSONObject> transactionsJSON = (ArrayList) data.get("transactions");
                    List<Transaction> transactions = new ArrayList<>();
                    for (JSONObject json : transactionsJSON) {
                        Transaction transaction = new Transaction();
                        transaction.setReceiptNumber((String) json.get("receipt_no"));
                        transaction.setTransactionType((String) json.get("transaction_type"));
                        transaction.setPointsEarned((String) json.get("points_earned"));
                        transaction.setCashPaid(String.valueOf(json.get("amount_paid_with_cash")));
                        transaction.setPointsPaid(String.valueOf(json.get("amount_paid_with_points")));
                        transaction.setDate((String) json.get("date"));
                        transactions.add(transaction);
                    }
                    customer.setTransactions(transactions);
                }

                if (data.get("customer_card") != null) {
                    JSONObject customerCard = (JSONObject) data.get("customer_card");
                    CustomerCard card = new CustomerCard();

                    JSONObject promoJSON = (JSONObject) customerCard.get("promo");
                    List<JSONObject> rewJSON = (ArrayList) promoJSON.get("rewards");
                    List<Reward> rewards = new ArrayList<>();
                    for (JSONObject json : rewJSON) {
                        Reward reward = new Reward();
                        reward.setId((String) json.get("id"));
                        reward.setName((String) json.get("name"));
                        reward.setDetails((String) json.get("details"));
                        reward.setStamps(((Long) json.get("stamps")).intValue());
                        reward.setImageUrl((String) json.get("image_url"));
                        rewards.add(reward);
                    }

                    Promo promo = new Promo();
                    promo.setRewards(rewards);
                    promo.setStamps((Long) promoJSON.get("stamps"));
                    card.setPromo(promo);
                    card.setStampCount((Long) customerCard.get("stamps"));
                    customer.setCard(card);

                    List<Reward> redeemedRewards = new ArrayList<>();
                    List<JSONObject> rewardsList = (ArrayList) customerCard.get("rewards");
                    for (JSONObject json :rewardsList) {
                        Reward reward = new Reward();
                        reward.setId((String) json.get("reward_id"));
                        reward.setName((String) json.get("reward_name"));
                        reward.setDate((String) json.get("date"));
                        reward.setImageUrl((String) json.get("reward_image_url"));
                        Long status = (Long) json.get("status");
                        reward.setStatus(status == 1 ? true : false);
                        redeemedRewards.add(reward);
                    }
                    card.setRewards(redeemedRewards);

                }

                App.appContextHolder.setCustomer(customer);
                apiResponse.setSuccess(true);
                apiResponse.setMessage("Redeem reward successful.");
            } else {
                apiResponse.setMessage((String) jsonObject.get("message"));
            }

        }else {
            apiResponse.setMessage("Network error.");
        }
        return apiResponse;
    }
/*

    public CustomerCard getCustomerCard() {

        Employee employee = App.appContextHolder.getEmployee();
        Customer customer = App.appContextHolder.getCustomer();

        List<NameValuePair> params = new ArrayList<>();
        String url = BASE_URL + CUSTOMER_CARD_ENDPOINT;
        url = url.replace(":employee_id", employee.getEmployeeId()).replace(":customer_id", customer.getUuid());
        JSONObject jsonObject = apiService.call(url, params, "get", MERCHANT_APP_RESOURCE_OWNER);
        if (jsonObject != null) {
            CustomerCard card = new CustomerCard();

            JSONObject data = (JSONObject) jsonObject.get("data");
            JSONObject promoJSON = (JSONObject) data.get("promo");

            Promo promo = new Promo();
            List<Reward> rewards = new ArrayList<>();
            List<JSONObject> rewardsJSON = (ArrayList) promoJSON.get("rewards");
            for (JSONObject rewardJSON : rewardsJSON) {
                Reward reward = new Reward();
                reward.setId((String) rewardJSON.get("id"));
                reward.setImageUrl((String) rewardJSON.get("image_url"));
                reward.setDetails((String) rewardJSON.get("details"));
                reward.setName((String) rewardJSON.get("name"));
                reward.setStamps(((Long) rewardJSON.get("stamps")).intValue());
                rewards.add(reward);
            }
            if (data.get("stamps") != null) {
                promo.setStamps(((Long) data.get("stamps")).intValue());
            }
            promo.setRewards(rewards);
            card.setPromo(promo);
            return card;
        }
        return null;
    }
*/



    private void loadActiveVouchers() {


        VBox rootVBox = App.appContextHolder.getRootContainer();
        Pagination activeVouchersPagination = (Pagination) rootVBox.getScene().lookup("#activeVouchersPagination");

        activeVouchersPagination.setPageCount(0);
        activeVouchersPagination.setPageFactory((Integer pageIndex) -> createActivateVoucherPage(pageIndex));
    }

    public Node createActivateVoucherPage(int pageIndex) {

        VBox box = new VBox();
        box.getChildren().addAll(buildTableView());
        return box;
    }

    private TableView<Reward> buildTableView() {
        VBox rootVBox = App.appContextHolder.getRootContainer();
        TextField searchTextField = (TextField) rootVBox.getScene().lookup("#searchTextField");
        Pagination activeVouchersPagination = (Pagination) rootVBox.getScene().lookup("#activeVouchersPagination");

        TableView<Reward> transactionsTableView = new TableView();
        transactionsTableView.setFixedCellSize(Region.USE_COMPUTED_SIZE);

        ObservableList<Reward> finalData = FXCollections.observableArrayList();
        ObservableList<Reward> textFilteredData = FXCollections.observableArrayList();
        Customer customer = App.appContextHolder.getCustomer();
        List<Reward> rewards = customer.getActiveVouchers();
        if (rewards != null) {
            if (searchTextField.getText() != null && !searchTextField.getText().isEmpty()) {
                String searchTxt = searchTextField.getText().toLowerCase();
                for (Reward reward : rewards) {
                    if (reward.getDetails().toLowerCase().contains(searchTxt)
                            || reward.getName().toLowerCase().contains(searchTxt)
                            || reward.getQuantity().toLowerCase().contains(searchTxt)) {
                        textFilteredData.add(reward);
                    }
                }
            } else {
                textFilteredData.addAll(rewards);
            }
            int maxEntries = 10;
            int pageCount = textFilteredData.size() / maxEntries;
            if (pageCount == 0) {
                pageCount = 1;
            } else {
                if (textFilteredData.size() % maxEntries > 0) {
                    pageCount++;
                }
            }

            activeVouchersPagination.setPageCount(pageCount);
            int pageIndex = activeVouchersPagination.getCurrentPageIndex();
            ObservableList<Reward> indexFilteredData = FXCollections.observableArrayList();
            for (Reward reward : textFilteredData) {
                int objIndex = textFilteredData.indexOf(reward);
                if (objIndex >= (pageIndex * maxEntries)  && objIndex < ((pageIndex + 1) * maxEntries)) {
                    indexFilteredData.add(reward);
                }
                if (objIndex > ((pageIndex + 1) * maxEntries -1)) {
                    break;
                }
            }
            finalData = indexFilteredData;
        }

        buildActiveVouchersColumns(transactionsTableView);
        transactionsTableView.setItems(finalData);
        return transactionsTableView;
    }

    private void buildActiveVouchersColumns(TableView tableView) {


        TableColumn dateCol = new TableColumn("Redemption Date");
        dateCol.setPrefWidth(200);
        dateCol.setCellValueFactory(
                new PropertyValueFactory<>("date"));


        TableColumn rewardCol = new TableColumn("Reward");
        rewardCol.setPrefWidth(200);
        rewardCol.setCellValueFactory(
                new PropertyValueFactory<>("name"));

        TableColumn mobileCol = new TableColumn("Details");
        mobileCol.setPrefWidth(400);
        mobileCol.setCellValueFactory(
                new PropertyValueFactory<>("details"));

        TableColumn orCol = new TableColumn("Quantity");
        orCol.setPrefWidth(100);
        orCol.setCellValueFactory(
                new PropertyValueFactory<>("quantity"));


        tableView.getColumns().clear();
        tableView.getColumns().addAll(dateCol,orCol, rewardCol, mobileCol);
    }


    private void loadCustomerTransactions() {


        VBox rootVBox = App.appContextHolder.getRootContainer();
        Pagination transactionsPagination = (Pagination) rootVBox.getScene().lookup("#transactionsPagination");

        transactionsPagination.setPageCount(0);
        transactionsPagination.setPageFactory((Integer pageIndex) -> createTransactionsPage(pageIndex));
    }

    public Node createTransactionsPage(int pageIndex) {

        VBox box = new VBox();
        box.getChildren().addAll(buildTransactionsTableView());
        return box;
    }

    private TableView<Transaction> buildTransactionsTableView() {
        VBox rootVBox = App.appContextHolder.getRootContainer();
        TextField searchTextField = (TextField) rootVBox.getScene().lookup("#searchTextField");
        Pagination transactionsPagination = (Pagination) rootVBox.getScene().lookup("#transactionsPagination");

        TableView<Transaction> transactionsTableView = new TableView();
        transactionsTableView.setFixedCellSize(Region.USE_COMPUTED_SIZE);

        ObservableList<Transaction> finalData = FXCollections.observableArrayList();
        ObservableList<Transaction> textFilteredData = FXCollections.observableArrayList();
        Customer customer = App.appContextHolder.getCustomer();
        List<Transaction> transactions = customer.getTransactions();
        if (transactions != null) {
            if (searchTextField.getText() != null && !searchTextField.getText().isEmpty()) {
                String searchTxt = searchTextField.getText().toLowerCase();
                for (Transaction transaction : transactions) {

                    StringBuilder sb = new StringBuilder();
                    sb.append(transaction.getDate());
                    sb.append(transaction.getTransactionType());

                    if (sb.toString().toLowerCase().contains(searchTxt)) {
                        textFilteredData.add(transaction);
                    }
                }
            } else {
                textFilteredData.addAll(transactions);
            }
            int maxEntries = 10;
            int pageCount = textFilteredData.size() / maxEntries;
            if (pageCount == 0) {
                pageCount = 1;
            } else {
                if (textFilteredData.size() % maxEntries > 0) {
                    pageCount++;
                }
            }

            transactionsPagination.setPageCount(pageCount);
            int pageIndex = transactionsPagination.getCurrentPageIndex();
            ObservableList<Transaction> indexFilteredData = FXCollections.observableArrayList();
            for (Transaction transaction : textFilteredData) {
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

        buildTransactionsColumns(transactionsTableView);
        transactionsTableView.setItems(finalData);
        return transactionsTableView;
    }

    private void buildTransactionsColumns(TableView tableView) {


        TableColumn c1 = new TableColumn("Date");
        c1.setPrefWidth(200);
        c1.setCellValueFactory(
                new PropertyValueFactory<>("date"));


        TableColumn c2 = new TableColumn("OR number");
        c2.setPrefWidth(200);
        c2.setCellValueFactory(
                new PropertyValueFactory<>("receiptNumber"));

        TableColumn c3 = new TableColumn("Transaction Type");
        c3.setPrefWidth(400);
        c3.setCellValueFactory(
                new PropertyValueFactory<>("transactionType"));

        TableColumn c4 = new TableColumn("Points earned");
        c4.setPrefWidth(100);
        c4.setCellValueFactory(
                new PropertyValueFactory<>("pointsEarned"));

        TableColumn c5 = new TableColumn("Points paid");
        c5.setPrefWidth(100);
        c5.setCellValueFactory(
                new PropertyValueFactory<>("pointsPaid"));

        TableColumn c6 = new TableColumn("Cash paid");
        c6.setPrefWidth(100);
        c6.setCellValueFactory(
                new PropertyValueFactory<>("cashPaid"));


        tableView.getColumns().clear();
        tableView.getColumns().addAll(c1,c2,c3,c4,c5,c6);
    }

}
