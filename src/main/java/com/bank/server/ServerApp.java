package com.bank.server;

import com.bank.common.model.Account;
import com.bank.common.model.Transaction;
import com.bank.server.sync.SyncListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ServerApp extends Application implements SyncListener {
    private ServerController controller;
    private ServerConfig config;

    // UI elements
    private Label lblServerStatus;
    private Label lblPeerStatus;
    private Button btnStart;
    private Button btnStop;
    private Button btnManualSync;
    private TextArea txtLogs;
    private TableView<Account> tableAccounts;
    private TableView<Transaction> tableTransactions;
    private ObservableList<Account> accountList = FXCollections.observableArrayList();
    private ObservableList<Transaction> transactionList = FXCollections.observableArrayList();

    private TextField txtServerId;
    private TextField txtServerHost;
    private TextField txtPort;
    private TextField txtPeerHost;
    private TextField txtPeerPort;

    @Override
    public void start(Stage primaryStage) {
        // Parse parameters if passed via command line
        Parameters params = getParameters();
        List<String> raw = params.getRaw();
        config = ServerConfig.createServer1();

        for (int i = 0; i < raw.size(); i++) {
            if ("--server2".equalsIgnoreCase(raw.get(i))) {
                config = ServerConfig.createServer2();
            } else if ("--id".equalsIgnoreCase(raw.get(i)) && i + 1 < raw.size()) {
                config.setServerId(raw.get(i + 1));
            } else if ("--host".equalsIgnoreCase(raw.get(i)) && i + 1 < raw.size()) {
                config.setHost(raw.get(i + 1));
            } else if ("--port".equalsIgnoreCase(raw.get(i)) && i + 1 < raw.size()) {
                config.setPort(Integer.parseInt(raw.get(i + 1)));
            } else if ("--peerPort".equalsIgnoreCase(raw.get(i)) && i + 1 < raw.size()) {
                config.setPeerPort(Integer.parseInt(raw.get(i + 1)));
            } else if ("--peerHost".equalsIgnoreCase(raw.get(i)) && i + 1 < raw.size()) {
                config.setPeerHost(raw.get(i + 1));
            } else if ("--db".equalsIgnoreCase(raw.get(i)) && i + 1 < raw.size()) {
                config.setDbFile(raw.get(i + 1));
            }
        }

        controller = new ServerController(config);
        controller.setUiListener(this);

        VBox root = new VBox(12);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #f4f6f9;");

        // Top Header
        HBox header = createHeader();
        // Server Configuration Bar
        HBox configBar = createConfigBar();
        // Main TabPane
        TabPane tabPane = createTabPane();
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        // Bottom Status Bar
        HBox statusBar = createStatusBar();

        root.getChildren().addAll(header, configBar, tabPane, statusBar);

        Scene scene = new Scene(root, 950, 680);
        primaryStage.setTitle("Hệ Thống Ngân Hàng RMI - Máy Chủ: " + config.getServerId());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            if (controller != null && controller.isRunning()) {
                controller.stopServer();
            }
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();

        // Auto start
        startServerAction();
    }

    private HBox createHeader() {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 15, 10, 15));
        box.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8;");

        Label title = new Label("QUẢN LÝ MÁY CHỦ RMI (ĐỒNG BỘ 2 SERVER)");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lblServerStatus = new Label("SERVER: STOPPED");
        lblServerStatus.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-weight: bold;");

        lblPeerStatus = new Label("PEER: DISCONNECTED");
        lblPeerStatus.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-weight: bold;");

        box.getChildren().addAll(title, spacer, lblServerStatus, lblPeerStatus);
        return box;
    }

    private HBox createConfigBar() {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        Label lblId = new Label("ID:");
        txtServerId = new TextField(config.getServerId());
        txtServerId.setPrefWidth(85);

        Label lblH = new Label("Host/IP:");
        txtServerHost = new TextField(config.getHost());
        txtServerHost.setPrefWidth(110);
        txtServerHost.setPromptText("IP máy chủ");

        Label lblP = new Label("Port:");
        txtPort = new TextField(String.valueOf(config.getPort()));
        txtPort.setPrefWidth(60);

        Label lblPH = new Label("Peer Host:");
        txtPeerHost = new TextField(config.getPeerHost());
        txtPeerHost.setPrefWidth(110);
        txtPeerHost.setPromptText("IP máy chủ 2");

        Label lblPP = new Label("Peer Port:");
        txtPeerPort = new TextField(String.valueOf(config.getPeerPort()));
        txtPeerPort.setPrefWidth(60);

        btnStart = new Button("Khởi động");
        btnStart.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnStart.setOnAction(e -> startServerAction());

        btnStop = new Button("Dừng");
        btnStop.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnStop.setOnAction(e -> stopServerAction());
        btnStop.setDisable(true);

        btnManualSync = new Button("Đồng bộ Peer");
        btnManualSync.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnManualSync.setOnAction(e -> {
            if (controller != null && controller.isRunning()) {
                controller.triggerManualSync();
            }
        });

        Button btnRefresh = new Button("Làm mới");
        btnRefresh.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-cursor: hand;");
        btnRefresh.setOnAction(e -> refreshData());

        box.getChildren().addAll(lblId, txtServerId, lblH, txtServerHost, lblP, txtPort, lblPH, txtPeerHost, lblPP, txtPeerPort,
                btnStart, btnStop, btnManualSync, btnRefresh);
        return box;
    }

    private TabPane createTabPane() {
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        // Tab 1: Live Logs
        Tab tabLogs = new Tab("Nhật Ký & Đồng Bộ (Live Logs)");
        tabLogs.setClosable(false);
        txtLogs = new TextArea();
        txtLogs.setEditable(false);
        txtLogs.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 13px; -fx-control-inner-background: #0f172a; -fx-text-fill: #38bdf8;");
        tabLogs.setContent(txtLogs);

        // Tab 2: Accounts
        Tab tabAcc = new Tab("Danh Sách Tài Khoản");
        tabAcc.setClosable(false);
        tableAccounts = new TableView<>();
        tableAccounts.setItems(accountList);

        TableColumn<Account, String> colAccNo = new TableColumn<>("Số Tài Khoản");
        colAccNo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAccountNumber()));
        colAccNo.setPrefWidth(140);

        TableColumn<Account, String> colName = new TableColumn<>("Chủ Tài Khoản");
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOwnerName()));
        colName.setPrefWidth(220);

        TableColumn<Account, String> colBalance = new TableColumn<>("Số Dư (VNĐ)");
        colBalance.setCellValueFactory(c -> new SimpleStringProperty(String.format("%,.0f VNĐ", c.getValue().getBalance())));
        colBalance.setPrefWidth(180);

        TableColumn<Account, String> colVer = new TableColumn<>("Phiên Bản (Version)");
        colVer.setCellValueFactory(c -> new SimpleStringProperty("v" + c.getValue().getVersion()));
        colVer.setPrefWidth(120);

        tableAccounts.getColumns().addAll(colAccNo, colName, colBalance, colVer);
        tabAcc.setContent(tableAccounts);

        // Tab 3: Transactions
        Tab tabTxn = new Tab("Lịch Sử Giao Dịch Đã Đồng Bộ");
        tabTxn.setClosable(false);
        tableTransactions = new TableView<>();
        tableTransactions.setItems(transactionList);

        TableColumn<Transaction, String> colTxnId = new TableColumn<>("Mã GD");
        colTxnId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTransactionId()));
        colTxnId.setPrefWidth(180);

        TableColumn<Transaction, String> colType = new TableColumn<>("Loại GD");
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().getDisplayName()));
        colType.setPrefWidth(110);

        TableColumn<Transaction, String> colSender = new TableColumn<>("Người Gửi");
        colSender.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSenderAccount()));
        colSender.setPrefWidth(120);

        TableColumn<Transaction, String> colReceiver = new TableColumn<>("Người Nhận");
        colReceiver.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReceiverAccount()));
        colReceiver.setPrefWidth(120);

        TableColumn<Transaction, String> colAmount = new TableColumn<>("Số Tiền");
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(String.format("%,.0f VNĐ", c.getValue().getAmount())));
        colAmount.setPrefWidth(140);

        TableColumn<Transaction, String> colServer = new TableColumn<>("Server Xử Lý");
        colServer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProcessedByServer()));
        colServer.setPrefWidth(120);

        TableColumn<Transaction, String> colTime = new TableColumn<>("Thời Gian");
        colTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFormattedTime()));
        colTime.setPrefWidth(150);

        tableTransactions.getColumns().addAll(colTxnId, colType, colSender, colReceiver, colAmount, colServer, colTime);
        tabTxn.setContent(tableTransactions);

        tabPane.getTabs().addAll(tabLogs, tabAcc, tabTxn);
        return tabPane;
    }

    private HBox createStatusBar() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5, 10, 5, 10));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 6;");

        Label lblInfo = new Label("Hệ thống RMI phân tán 2 Server với cơ chế sao chép dữ liệu thời gian thực và tự động khắc phục sự cố.");
        lblInfo.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        box.getChildren().add(lblInfo);
        return box;
    }

    private void startServerAction() {
        try {
            config.setServerId(txtServerId.getText().trim());
            config.setHost(txtServerHost.getText().trim());
            config.setPort(Integer.parseInt(txtPort.getText().trim()));
            config.setPeerHost(txtPeerHost.getText().trim());
            config.setPeerPort(Integer.parseInt(txtPeerPort.getText().trim()));
            config.setDbFile("bank_" + config.getServerId().toLowerCase() + ".db");

            controller = new ServerController(config);
            controller.setUiListener(this);
            controller.startServer();

            lblServerStatus.setText("SERVER: RUNNING (" + config.getHost() + ":" + config.getPort() + ")");
            lblServerStatus.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-weight: bold;");

            btnStart.setDisable(true);
            btnStop.setDisable(false);
            txtServerId.setDisable(true);
            txtServerHost.setDisable(true);
            txtPort.setDisable(true);

            refreshData();
        } catch (Exception e) {
            onLog("ERROR", "Không thể khởi động server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopServerAction() {
        if (controller != null) {
            controller.stopServer();
        }
        lblServerStatus.setText("SERVER: STOPPED");
        lblServerStatus.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-weight: bold;");

        lblPeerStatus.setText("PEER: DISCONNECTED");
        lblPeerStatus.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-weight: bold;");

        btnStart.setDisable(false);
        btnStop.setDisable(true);
        txtServerId.setDisable(false);
        txtServerHost.setDisable(false);
        txtPort.setDisable(false);
    }

    private void refreshData() {
        if (controller != null && controller.isRunning()) {
            Platform.runLater(() -> {
                accountList.setAll(controller.getAccounts());
                transactionList.setAll(controller.getTransactions());
            });
        }
    }

    @Override
    public void onLog(String level, String message) {
        Platform.runLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            txtLogs.appendText(String.format("[%s] [%s] %s\n", time, level, message));
        });
    }

    @Override
    public void onPeerStatusChanged(boolean connected, String message) {
        Platform.runLater(() -> {
            if (connected) {
                lblPeerStatus.setText("PEER: CONNECTED (" + config.getPeerPort() + ")");
                lblPeerStatus.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-weight: bold;");
            } else {
                lblPeerStatus.setText("PEER: DISCONNECTED");
                lblPeerStatus.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-weight: bold;");
            }
        });
    }

    @Override
    public void onDataChanged(Account account, Transaction transaction) {
        refreshData();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
