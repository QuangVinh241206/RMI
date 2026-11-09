package com.bank.client.gui;

import com.bank.client.ClientRmiManager;
import com.bank.client.util.FormatUtil;
import com.bank.common.model.Account;
import com.bank.common.model.ServerStatus;
import com.bank.common.model.Transaction;
import com.bank.common.model.TransactionType;
import com.bank.common.model.TransferResult;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

public class DashboardView extends VBox {
    private final ClientApp app;
    private final ClientRmiManager rmiManager = ClientRmiManager.getInstance();

    private Account account;
    private Label lblBalanceValue;
    private boolean isBalanceHidden = false;
    private Label lblAccountVersion;
    private Label lblConnectedServerBadge;
    private Button btnSwitchServer;

    // Transfer fields
    private TextField txtTransferToAcc;
    private Label lblReceiverLookup;
    private TextField txtTransferAmount;
    private TextField txtTransferMsg;
    private PasswordField txtTransferPin;
    private Label lblTransferError;

    // Deposit / Withdraw fields
    private TextField txtDepositAmount;
    private TextField txtWithdrawAmount;
    private PasswordField txtWithdrawPin;
    private Label lblDepositMsg;
    private Label lblWithdrawMsg;

    // Transaction table
    private ObservableList<Transaction> transactionMasterList = FXCollections.observableArrayList();
    private FilteredList<Transaction> filteredTransactions;
    private TableView<Transaction> tableTxn;

    // Monitor labels
    private Label lblMonitorS1;
    private Label lblMonitorS2;
    private Label lblMonitorSyncDesc;

    public DashboardView(ClientApp app) {
        this.app = app;
        this.account = rmiManager.getCurrentAccount();

        setSpacing(15);
        setPadding(new Insets(15));
        setStyle("-fx-background-color: #f8fafc;");

        // Top Navigation Bar
        HBox topNav = createTopNav();

        // Split Main Content: Left Column (Card & Stats), Right Column (Tabs)
        HBox mainContent = new HBox(15);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        VBox leftCol = createLeftColumn();
        leftCol.setPrefWidth(320);

        TabPane rightTabPane = createRightTabPane();
        HBox.setHgrow(rightTabPane, Priority.ALWAYS);

        mainContent.getChildren().addAll(leftCol, rightTabPane);

        getChildren().addAll(topNav, mainContent);

        // Load initial data
        refreshAccountData();
        refreshTransactions();
    }

    private HBox createTopNav() {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 15, 10, 15));
        box.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 10;");

        Label brand = new Label("E-BANKING RMI");
        brand.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 900;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lblConnectedServerBadge = new Label("Đang kết nối: SERVER " + rmiManager.getActiveServer() + " (" + rmiManager.getActiveServerAddress() + ")");
        lblConnectedServerBadge.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-padding: 6 12; -fx-background-radius: 14; -fx-font-size: 12px; -fx-font-weight: bold;");

        btnSwitchServer = new Button("Chuyển Server");
        btnSwitchServer.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;");
        btnSwitchServer.setOnAction(e -> handleSwitchServer());

        Button btnLogout = new Button("Đăng xuất");
        btnLogout.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;");
        btnLogout.setOnAction(e -> app.showLogin());

        box.getChildren().addAll(brand, spacer, lblConnectedServerBadge, btnSwitchServer, btnLogout);
        return box;
    }

    private VBox createLeftColumn() {
        VBox box = new VBox(15);

        // Bank Card UI
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #1e3a8a, #3b82f6, #6366f1); -fx-background-radius: 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 4);");

        HBox cardTop = new HBox();
        cardTop.setAlignment(Pos.CENTER_LEFT);
        Label lblCardType = new Label("PLATINUM DEBIT");
        lblCardType.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 11px; -fx-font-weight: bold;");
        Region s1 = new Region();
        HBox.setHgrow(s1, Priority.ALWAYS);
        lblAccountVersion = new Label("Sync v" + (account != null ? account.getVersion() : 1));
        lblAccountVersion.setStyle("-fx-text-fill: #fef08a; -fx-font-size: 11px; -fx-font-weight: bold;");
        cardTop.getChildren().addAll(lblCardType, s1, lblAccountVersion);

        // Account number with copy
        HBox accBox = new HBox(10);
        accBox.setAlignment(Pos.CENTER_LEFT);
        Label lblAcc = new Label(account != null ? account.getAccountNumber() : "------");
        lblAcc.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: monospace;");
        Button btnCopy = new Button("Sao chép");
        btnCopy.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-font-size: 10px; -fx-cursor: hand; -fx-padding: 3 6; -fx-background-radius: 4;");
        btnCopy.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(lblAcc.getText());
            clipboard.setContent(content);
            btnCopy.setText("Đã chép!");
        });
        accBox.getChildren().addAll(lblAcc, btnCopy);

        // Balance
        VBox balanceBox = new VBox(4);
        HBox balHeader = new HBox(8);
        balHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblBalTitle = new Label("Số dư khả dụng");
        lblBalTitle.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
        Button btnToggleEye = new Button("Ẩn/Hiện");
        btnToggleEye.setStyle("-fx-background-color: transparent; -fx-text-fill: #93c5fd; -fx-font-size: 11px; -fx-cursor: hand; -fx-underline: true;");
        btnToggleEye.setOnAction(e -> toggleBalanceVisibility());
        balHeader.getChildren().addAll(lblBalTitle, btnToggleEye);

        lblBalanceValue = new Label(FormatUtil.formatCurrency(account != null ? account.getBalance() : 0));
        lblBalanceValue.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: 800;");
        balanceBox.getChildren().addAll(balHeader, lblBalanceValue);

        // Card holder
        Label lblOwner = new Label(account != null ? account.getOwnerName().toUpperCase() : "");
        lblOwner.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        card.getChildren().addAll(cardTop, accBox, balanceBox, lblOwner);

        // Server Quick Info Box
        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(15));
        infoBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12;");

        Label lblInfoTitle = new Label("Thông Tin Kết Nối Phân Tán");
        lblInfoTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #334155;");

        Label lblS1 = new Label("Máy chủ 1: " + rmiManager.getServer1Address());
        Label lblS2 = new Label("Máy chủ 2: " + rmiManager.getServer2Address());
        lblS1.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        lblS2.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        Button btnRefreshAll = new Button("Làm mới toàn bộ dữ liệu");
        btnRefreshAll.setMaxWidth(Double.MAX_VALUE);
        btnRefreshAll.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #1e293b; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: #cbd5e1; -fx-border-radius: 6;");
        btnRefreshAll.setOnAction(e -> {
            refreshAccountData();
            refreshTransactions();
        });

        infoBox.getChildren().addAll(lblInfoTitle, lblS1, lblS2, btnRefreshAll);

        box.getChildren().addAll(card, infoBox);
        return box;
    }

    private TabPane createRightTabPane() {
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: white; -fx-background-radius: 12;");

        // Tab 1: Chuyển tiền
        Tab tabTransfer = new Tab("Chuyển Tiền 24/7", createTransferTab());
        tabTransfer.setClosable(false);

        // Tab 2: Nạp / Rút
        Tab tabDepositWithdraw = new Tab("Nạp & Rút Tiền", createDepositWithdrawTab());
        tabDepositWithdraw.setClosable(false);

        // Tab 3: Lịch sử giao dịch
        Tab tabHistory = new Tab("Lịch Sử Giao Dịch", createHistoryTab());
        tabHistory.setClosable(false);

        // Tab 4: Giám sát 2 Server & Đồng bộ
        Tab tabMonitor = new Tab("Đồng Bộ 2 Server", createServerMonitorTab());
        tabMonitor.setClosable(false);

        tabPane.getTabs().addAll(tabTransfer, tabDepositWithdraw, tabHistory, tabMonitor);
        return tabPane;
    }

    private VBox createTransferTab() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));

        Label lblTitle = new Label("Chuyển Khoản RMI Liên Máy Chủ");
        lblTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        // Receiver Account with auto-lookup
        VBox receiverBox = new VBox(6);
        Label lblTo = new Label("Tài khoản thụ hưởng:");
        lblTo.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        txtTransferToAcc = new TextField();
        txtTransferToAcc.setPromptText("Nhập số tài khoản người nhận (ví dụ: 100002)");
        txtTransferToAcc.setStyle("-fx-padding: 8; -fx-font-size: 13px;");

        lblReceiverLookup = new Label();
        lblReceiverLookup.setStyle("-fx-font-size: 12px;");

        txtTransferToAcc.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV.trim().length() >= 5) {
                checkReceiverAccountAsync(newV.trim());
            } else {
                lblReceiverLookup.setText("");
            }
        });

        receiverBox.getChildren().addAll(lblTo, txtTransferToAcc, lblReceiverLookup);

        // Amount with quick preset chips
        VBox amountBox = new VBox(6);
        Label lblAmt = new Label("Số tiền chuyển (VNĐ):");
        lblAmt.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        txtTransferAmount = new TextField();
        txtTransferAmount.setPromptText("Nhập số tiền chuyển");
        txtTransferAmount.setStyle("-fx-padding: 8; -fx-font-size: 14px;");

        HBox chipsBox = new HBox(8);
        chipsBox.setAlignment(Pos.CENTER_LEFT);
        chipsBox.getChildren().addAll(
                createAmountChip("+50.000", 50000),
                createAmountChip("+100.000", 100000),
                createAmountChip("+500.000", 500000),
                createAmountChip("+1.000.000", 1000000),
                createAmountChip("+2.000.000", 2000000)
        );
        amountBox.getChildren().addAll(lblAmt, txtTransferAmount, chipsBox);

        // Message
        VBox msgBox = new VBox(6);
        Label lblMsg = new Label("Nội dung chuyển tiền:");
        lblMsg.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        txtTransferMsg = new TextField(account != null ? (account.getOwnerName() + " chuyen tien") : "Chuyen tien");
        txtTransferMsg.setStyle("-fx-padding: 8; -fx-font-size: 13px;");
        msgBox.getChildren().addAll(lblMsg, txtTransferMsg);

        // PIN
        VBox pinBox = new VBox(6);
        Label lblPin = new Label("Mã PIN xác nhận:");
        lblPin.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        txtTransferPin = new PasswordField();
        txtTransferPin.setText("123456");
        txtTransferPin.setPromptText("Nhập mã PIN của bạn");
        txtTransferPin.setStyle("-fx-padding: 8; -fx-font-size: 13px;");
        pinBox.getChildren().addAll(lblPin, txtTransferPin);

        lblTransferError = new Label();
        lblTransferError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        lblTransferError.setWrapText(true);

        Button btnSubmit = new Button("TIẾP TỤC & CHUYỂN TIỀN");
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12; -fx-background-radius: 8; -fx-cursor: hand;");
        btnSubmit.setOnAction(e -> handleTransfer());

        box.getChildren().addAll(lblTitle, receiverBox, amountBox, msgBox, pinBox, lblTransferError, btnSubmit);
        return box;
    }

    private Button createAmountChip(String label, double val) {
        Button btn = new Button(label);
        btn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-size: 11px; -fx-padding: 4 8; -fx-background-radius: 12; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            try {
                double current = 0;
                if (!txtTransferAmount.getText().isEmpty()) {
                    current = Double.parseDouble(txtTransferAmount.getText().replaceAll("[^0-9]", ""));
                }
                txtTransferAmount.setText(String.valueOf((long)(current + val)));
            } catch (Exception ex) {
                txtTransferAmount.setText(String.valueOf((long)val));
            }
        });
        return btn;
    }

    private void checkReceiverAccountAsync(String targetAccNo) {
        if (account != null && targetAccNo.equals(account.getAccountNumber())) {
            lblReceiverLookup.setText("⚠ Đây là số tài khoản của bạn!");
            lblReceiverLookup.setStyle("-fx-text-fill: #eab308; -fx-font-size: 12px; -fx-font-weight: bold;");
            return;
        }

        new Thread(() -> {
            try {
                Account receiver = rmiManager.getAccount(targetAccNo);
                Platform.runLater(() -> {
                    if (receiver != null) {
                        lblReceiverLookup.setText("✓ Người nhận: " + receiver.getOwnerName().toUpperCase());
                        lblReceiverLookup.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 12px; -fx-font-weight: bold;");
                    } else {
                        lblReceiverLookup.setText("✗ Không tìm thấy người thụ hưởng!");
                        lblReceiverLookup.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblReceiverLookup.setText("Lỗi kết nối kiểm tra tài khoản: " + e.getMessage());
                    lblReceiverLookup.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px;");
                });
            }
        }).start();
    }

    private void handleTransfer() {
        lblTransferError.setText("");
        String toAcc = txtTransferToAcc.getText().trim();
        String amtStr = txtTransferAmount.getText().trim().replaceAll("[^0-9.]", "");
        String msg = txtTransferMsg.getText().trim();
        String pin = txtTransferPin.getText().trim();

        if (toAcc.isEmpty() || amtStr.isEmpty() || pin.isEmpty()) {
            lblTransferError.setText("Vui lòng điền đầy đủ các thông tin!");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            lblTransferError.setText("Số tiền không hợp lệ!");
            return;
        }

        new Thread(() -> {
            try {
                TransferResult res = rmiManager.transfer(account.getAccountNumber(), toAcc, amount, pin, msg);
                Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Giao Dịch Thành Công");
                        alert.setHeaderText("Chuyển tiền thành công!");
                        alert.setContentText("Mã giao dịch: " + res.getTransaction().getTransactionId() +
                                "\nSố tiền: " + FormatUtil.formatCurrency(amount) +
                                "\nNgười nhận: " + toAcc +
                                "\nServer xử lý & đồng bộ: " + res.getProcessedByServer() +
                                "\nSố dư mới: " + FormatUtil.formatCurrency(res.getNewBalance()));
                        alert.showAndWait();

                        txtTransferAmount.clear();
                        refreshAccountData();
                        refreshTransactions();
                    } else {
                        lblTransferError.setText(res.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> lblTransferError.setText("Lỗi thực hiện giao dịch: " + e.getMessage()));
            }
        }).start();
    }

    private VBox createDepositWithdrawTab() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(20));

        HBox split = new HBox(20);

        // Nạp tiền box
        VBox depBox = new VBox(12);
        depBox.setPadding(new Insets(15));
        depBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        HBox.setHgrow(depBox, Priority.ALWAYS);

        Label lblDepTitle = new Label("NẠP TIỀN VÀO TÀI KHOẢN");
        lblDepTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #15803d;");

        Label lblDepAmt = new Label("Số tiền nạp (VNĐ):");
        txtDepositAmount = new TextField();
        txtDepositAmount.setPromptText("Ví dụ: 1000000");
        txtDepositAmount.setStyle("-fx-padding: 8;");

        lblDepositMsg = new Label();
        lblDepositMsg.setStyle("-fx-font-size: 12px;");

        Button btnDep = new Button("Nạp Tiền");
        btnDep.setMaxWidth(Double.MAX_VALUE);
        btnDep.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10; -fx-cursor: hand;");
        btnDep.setOnAction(e -> handleDeposit());

        depBox.getChildren().addAll(lblDepTitle, lblDepAmt, txtDepositAmount, lblDepositMsg, btnDep);

        // Rút tiền box
        VBox withBox = new VBox(12);
        withBox.setPadding(new Insets(15));
        withBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        HBox.setHgrow(withBox, Priority.ALWAYS);

        Label lblWithTitle = new Label("RÚT TIỀN TỪ TÀI KHOẢN");
        lblWithTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #b91c1c;");

        Label lblWithAmt = new Label("Số tiền rút (VNĐ):");
        txtWithdrawAmount = new TextField();
        txtWithdrawAmount.setPromptText("Ví dụ: 500000");
        txtWithdrawAmount.setStyle("-fx-padding: 8;");

        Label lblWithPin = new Label("Mã PIN:");
        txtWithdrawPin = new PasswordField();
        txtWithdrawPin.setText("123456");
        txtWithdrawPin.setStyle("-fx-padding: 8;");

        lblWithdrawMsg = new Label();
        lblWithdrawMsg.setStyle("-fx-font-size: 12px;");

        Button btnWith = new Button("Rút Tiền");
        btnWith.setMaxWidth(Double.MAX_VALUE);
        btnWith.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10; -fx-cursor: hand;");
        btnWith.setOnAction(e -> handleWithdraw());

        withBox.getChildren().addAll(lblWithTitle, lblWithAmt, txtWithdrawAmount, lblWithPin, txtWithdrawPin, lblWithdrawMsg, btnWith);

        split.getChildren().addAll(depBox, withBox);
        box.getChildren().addAll(split);
        return box;
    }

    private void handleDeposit() {
        lblDepositMsg.setText("");
        String amtStr = txtDepositAmount.getText().trim();
        try {
            double amt = Double.parseDouble(amtStr);
            new Thread(() -> {
                try {
                    TransferResult res = rmiManager.deposit(account.getAccountNumber(), amt);
                    Platform.runLater(() -> {
                        if (res.isSuccess()) {
                            lblDepositMsg.setText("✓ " + res.getMessage());
                            lblDepositMsg.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                            txtDepositAmount.clear();
                            refreshAccountData();
                            refreshTransactions();
                        } else {
                            lblDepositMsg.setText("✗ " + res.getMessage());
                            lblDepositMsg.setStyle("-fx-text-fill: #dc2626;");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        lblDepositMsg.setText("Lỗi: " + e.getMessage());
                        lblDepositMsg.setStyle("-fx-text-fill: #dc2626;");
                    });
                }
            }).start();
        } catch (Exception e) {
            lblDepositMsg.setText("Số tiền không hợp lệ!");
            lblDepositMsg.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private void handleWithdraw() {
        lblWithdrawMsg.setText("");
        String amtStr = txtWithdrawAmount.getText().trim();
        String pin = txtWithdrawPin.getText().trim();
        try {
            double amt = Double.parseDouble(amtStr);
            new Thread(() -> {
                try {
                    TransferResult res = rmiManager.withdraw(account.getAccountNumber(), amt, pin);
                    Platform.runLater(() -> {
                        if (res.isSuccess()) {
                            lblWithdrawMsg.setText("✓ " + res.getMessage());
                            lblWithdrawMsg.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                            txtWithdrawAmount.clear();
                            refreshAccountData();
                            refreshTransactions();
                        } else {
                            lblWithdrawMsg.setText("✗ " + res.getMessage());
                            lblWithdrawMsg.setStyle("-fx-text-fill: #dc2626;");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        lblWithdrawMsg.setText("Lỗi: " + e.getMessage());
                        lblWithdrawMsg.setStyle("-fx-text-fill: #dc2626;");
                    });
                }
            }).start();
        } catch (Exception e) {
            lblWithdrawMsg.setText("Số tiền không hợp lệ!");
            lblWithdrawMsg.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private VBox createHistoryTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(15));

        // Filter Bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        Label lblFilter = new Label("Lọc loại GD:");
        ComboBox<String> cbFilter = new ComboBox<>(FXCollections.observableArrayList("Tất cả", "Chuyển tiền", "Nạp tiền", "Rút tiền"));
        cbFilter.getSelectionModel().selectFirst();

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Tìm kiếm theo mã GD / số TK...");
        txtSearch.setPrefWidth(220);

        filteredTransactions = new FilteredList<>(transactionMasterList, p -> true);

        cbFilter.setOnAction(e -> applyFilter(cbFilter.getValue(), txtSearch.getText()));
        txtSearch.textProperty().addListener((obs, oldV, newV) -> applyFilter(cbFilter.getValue(), newV));

        Button btnRefresh = new Button("Làm mới");
        btnRefresh.setStyle("-fx-background-color: #f1f5f9; -fx-cursor: hand;");
        btnRefresh.setOnAction(e -> refreshTransactions());

        filterBar.getChildren().addAll(lblFilter, cbFilter, txtSearch, btnRefresh);

        // Table
        tableTxn = new TableView<>();
        tableTxn.setItems(filteredTransactions);
        VBox.setVgrow(tableTxn, Priority.ALWAYS);

        TableColumn<Transaction, String> colId = new TableColumn<>("Mã GD");
        colId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTransactionId()));
        colId.setPrefWidth(160);

        TableColumn<Transaction, String> colType = new TableColumn<>("Loại");
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().getDisplayName()));
        colType.setPrefWidth(100);

        TableColumn<Transaction, String> colAmount = new TableColumn<>("Số Tiền");
        colAmount.setCellValueFactory(c -> {
            Transaction t = c.getValue();
            boolean isSender = account != null && account.getAccountNumber().equals(t.getSenderAccount());
            String sign = (t.getType() == TransactionType.DEPOSIT || (!isSender && t.getType() == TransactionType.TRANSFER)) ? "+" : "-";
            return new SimpleStringProperty(sign + FormatUtil.formatCurrency(t.getAmount()));
        });
        colAmount.setPrefWidth(130);

        TableColumn<Transaction, String> colFrom = new TableColumn<>("Người Gửi");
        colFrom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSenderAccount()));
        colFrom.setPrefWidth(100);

        TableColumn<Transaction, String> colTo = new TableColumn<>("Người Nhận");
        colTo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReceiverAccount()));
        colTo.setPrefWidth(100);

        TableColumn<Transaction, String> colMsg = new TableColumn<>("Nội Dung");
        colMsg.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMessage()));
        colMsg.setPrefWidth(160);

        TableColumn<Transaction, String> colServer = new TableColumn<>("Server Xử Lý");
        colServer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProcessedByServer()));
        colServer.setPrefWidth(100);

        TableColumn<Transaction, String> colTime = new TableColumn<>("Thời Gian");
        colTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFormattedTime()));
        colTime.setPrefWidth(140);

        tableTxn.getColumns().addAll(colId, colType, colAmount, colFrom, colTo, colMsg, colServer, colTime);

        box.getChildren().addAll(filterBar, tableTxn);
        return box;
    }

    private void applyFilter(String typeFilter, String keyword) {
        filteredTransactions.setPredicate(txn -> {
            if (txn == null) return false;

            // Check type
            if (typeFilter != null && !typeFilter.equals("Tất cả")) {
                if (!txn.getType().getDisplayName().equalsIgnoreCase(typeFilter)) {
                    return false;
                }
            }

            // Check keyword
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.toLowerCase().trim();
                boolean matchId = txn.getTransactionId().toLowerCase().contains(kw);
                boolean matchSender = txn.getSenderAccount() != null && txn.getSenderAccount().toLowerCase().contains(kw);
                boolean matchReceiver = txn.getReceiverAccount() != null && txn.getReceiverAccount().toLowerCase().contains(kw);
                boolean matchMsg = txn.getMessage() != null && txn.getMessage().toLowerCase().contains(kw);
                return matchId || matchSender || matchReceiver || matchMsg;
            }

            return true;
        });
    }

    private VBox createServerMonitorTab() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(20));

        Label lblTitle = new Label("Kiến Trúc Phân Tán: RMI Đồng Bộ Hóa Dữ Liệu Trên 2 Máy Chủ");
        lblTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        lblMonitorSyncDesc = new Label("Cơ chế: Mọi giao dịch (chuyển, nạp, rút, đăng ký) thực hiện trên máy chủ này đều được đồng bộ hóa tức thì sang máy chủ đối ứng qua giao thức Java RMI. Nếu một máy chủ ngừng hoạt động (offline), Client tự động chuyển đổi sang máy chủ còn lại (Failover).");
        lblMonitorSyncDesc.setWrapText(true);
        lblMonitorSyncDesc.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-line-spacing: 4;");

        // Two Server Cards
        HBox cards = new HBox(20);
        cards.setAlignment(Pos.CENTER);

        VBox s1Card = new VBox(10);
        s1Card.setPadding(new Insets(15));
        s1Card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #cbd5e1; -fx-border-radius: 8;");
        s1Card.setPrefWidth(260);

        Label s1Title = new Label("SERVER 1");
        s1Title.setStyle("-fx-font-weight: 800; -fx-font-size: 15px; -fx-text-fill: #1e3a8a;");
        Label s1Addr = new Label("Địa chỉ: " + rmiManager.getServer1Address());
        lblMonitorS1 = new Label("Đang kiểm tra...");
        s1Card.getChildren().addAll(s1Title, s1Addr, lblMonitorS1);

        Label lblArrow = new Label("◄════►\nRMI Sync");
        lblArrow.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563eb; -fx-text-alignment: center;");
        lblArrow.setAlignment(Pos.CENTER);

        VBox s2Card = new VBox(10);
        s2Card.setPadding(new Insets(15));
        s2Card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #cbd5e1; -fx-border-radius: 8;");
        s2Card.setPrefWidth(260);

        Label s2Title = new Label("SERVER 2");
        s2Title.setStyle("-fx-font-weight: 800; -fx-font-size: 15px; -fx-text-fill: #1e3a8a;");
        Label s2Addr = new Label("Địa chỉ: " + rmiManager.getServer2Address());
        lblMonitorS2 = new Label("Đang kiểm tra...");
        s2Card.getChildren().addAll(s2Title, s2Addr, lblMonitorS2);

        cards.getChildren().addAll(s1Card, lblArrow, s2Card);

        Button btnTestSwitch = new Button("Thử Nghiệm Chuyển Đổi Server Nhanh");
        btnTestSwitch.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;");
        btnTestSwitch.setOnAction(e -> handleSwitchServer());

        box.getChildren().addAll(lblTitle, lblMonitorSyncDesc, cards, btnTestSwitch);

        checkMonitorStatusesAsync();
        return box;
    }

    private void checkMonitorStatusesAsync() {
        new Thread(() -> {
            boolean s1Online = rmiManager.checkServerOnline(1);
            boolean s2Online = rmiManager.checkServerOnline(2);

            Platform.runLater(() -> {
                if (s1Online) {
                    lblMonitorS1.setText("● Hoạt động bình thường (Online)");
                    lblMonitorS1.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                } else {
                    lblMonitorS1.setText("○ Đang tắt hoặc mất kết nối (Offline)");
                    lblMonitorS1.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                }

                if (s2Online) {
                    lblMonitorS2.setText("● Hoạt động bình thường (Online)");
                    lblMonitorS2.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                } else {
                    lblMonitorS2.setText("○ Đang tắt hoặc mất kết nối (Offline)");
                    lblMonitorS2.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                }
            });
        }).start();
    }

    private void handleSwitchServer() {
        boolean ok = rmiManager.switchServer();
        if (ok) {
            lblConnectedServerBadge.setText("Đang kết nối: SERVER " + rmiManager.getActiveServer() + " (" + rmiManager.getActiveServerAddress() + ")");
            refreshAccountData();
            refreshTransactions();
            checkMonitorStatusesAsync();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Chuyển Đổi Server Thành Công");
            alert.setHeaderText("Đã chuyển kết nối sang SERVER " + rmiManager.getActiveServer() + "!");
            alert.setContentText("Dữ liệu tài khoản và giao dịch trên Server mới hoàn toàn đồng nhất với Server trước đó nhờ tính năng đồng bộ thời gian thực.");
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi Chuyển Đổi");
            alert.setHeaderText("Không thể kết nối sang Server đối ứng!");
            alert.setContentText("Server đối ứng đang tắt hoặc gặp sự cố.");
            alert.showAndWait();
        }
    }

    private void toggleBalanceVisibility() {
        isBalanceHidden = !isBalanceHidden;
        if (isBalanceHidden) {
            lblBalanceValue.setText("****** đ");
        } else {
            lblBalanceValue.setText(FormatUtil.formatCurrency(account != null ? account.getBalance() : 0));
        }
    }

    private void refreshAccountData() {
        if (account == null) return;
        new Thread(() -> {
            try {
                Account refreshed = rmiManager.getAccount(account.getAccountNumber());
                Platform.runLater(() -> {
                    if (refreshed != null) {
                        this.account = refreshed;
                        rmiManager.setCurrentAccount(refreshed);
                        if (!isBalanceHidden) {
                            lblBalanceValue.setText(FormatUtil.formatCurrency(account.getBalance()));
                        }
                        lblAccountVersion.setText("Sync v" + account.getVersion());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void refreshTransactions() {
        if (account == null) return;
        new Thread(() -> {
            try {
                List<Transaction> list = rmiManager.getTransactionHistory(account.getAccountNumber());
                Platform.runLater(() -> {
                    transactionMasterList.setAll(list);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
