package com.bank.client.gui;

import com.bank.client.ClientRmiManager;
import com.bank.client.util.FormatUtil;
import com.bank.common.model.Account;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class LoginView extends VBox {
    private final ClientApp app;
    private final ClientRmiManager rmiManager = ClientRmiManager.getInstance();

    private TextField txtLoginAcc;
    private PasswordField txtLoginPin;
    private Label lblLoginError;

    private TextField txtRegName;
    private PasswordField txtRegPin;
    private TextField txtRegDeposit;
    private Label lblRegError;

    private TextField txtS1Host;
    private TextField txtS1Port;
    private TextField txtS2Host;
    private TextField txtS2Port;
    private Label lblServer1Status;
    private Label lblServer2Status;

    public LoginView(ClientApp app) {
        this.app = app;
        setSpacing(15);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(25));
        setStyle("-fx-background-color: #f1f5f9;");

        VBox card = new VBox(15);
        card.setMaxWidth(480);
        card.setPadding(new Insets(30));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);");

        // Logo & Title
        Label lblBrand = new Label("E-BANKING RMI");
        lblBrand.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #1e3a8a;");

        Label lblSub = new Label("Hệ thống chuyển tiền RMI đồng bộ 2 máy chủ");
        lblSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        VBox titleBox = new VBox(4, lblBrand, lblSub);
        titleBox.setAlignment(Pos.CENTER);

        // TabPane: Login & Register
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-tab-min-width: 180px;");

        Tab tabLogin = new Tab("Đăng Nhập", createLoginForm());
        tabLogin.setClosable(false);

        Tab tabRegister = new Tab("Mở Tài Khoản", createRegisterForm());
        tabRegister.setClosable(false);

        tabPane.getTabs().addAll(tabLogin, tabRegister);

        // Server Connection Accordion / Settings
        TitledPane serverSettingsPane = createServerSettingsPane();
        serverSettingsPane.setExpanded(false);

        card.getChildren().addAll(titleBox, tabPane, serverSettingsPane);
        getChildren().add(card);

        // Quick check servers in background
        checkServerStatusesAsync();
    }

    private VBox createLoginForm() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(20, 10, 10, 10));

        Label lblAcc = new Label("Số tài khoản:");
        lblAcc.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        txtLoginAcc = new TextField("100001");
        txtLoginAcc.setPromptText("Nhập số tài khoản (ví dụ: 100001)");
        txtLoginAcc.setStyle("-fx-padding: 8; -fx-font-size: 14px;");

        // Quick fill demo buttons
        HBox quickFillBox = new HBox(8);
        quickFillBox.setAlignment(Pos.CENTER_LEFT);
        Label lblQuick = new Label("Tài khoản mẫu:");
        lblQuick.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        Button btnAcc1 = createChipButton("100001 (A)", "100001");
        Button btnAcc2 = createChipButton("100002 (B)", "100002");
        Button btnAcc3 = createChipButton("100003 (C)", "100003");
        quickFillBox.getChildren().addAll(lblQuick, btnAcc1, btnAcc2, btnAcc3);

        Label lblPin = new Label("Mã PIN (Mật khẩu):");
        lblPin.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        txtLoginPin = new PasswordField();
        txtLoginPin.setText("123456");
        txtLoginPin.setPromptText("Nhập 6 số PIN");
        txtLoginPin.setStyle("-fx-padding: 8; -fx-font-size: 14px;");

        lblLoginError = new Label();
        lblLoginError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        lblLoginError.setWrapText(true);

        Button btnLogin = new Button("ĐĂNG NHẬP NGAY");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10; -fx-background-radius: 6; -fx-cursor: hand;");
        btnLogin.setOnAction(e -> handleLogin());

        box.getChildren().addAll(lblAcc, txtLoginAcc, quickFillBox, lblPin, txtLoginPin, lblLoginError, btnLogin);
        return box;
    }

    private Button createChipButton(String text, String accNo) {
        Button btn = new Button(text);
        btn.setStyle("-fx-font-size: 11px; -fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-padding: 3 8; -fx-background-radius: 12; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            txtLoginAcc.setText(accNo);
            txtLoginPin.setText("123456");
        });
        return btn;
    }

    private VBox createRegisterForm() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(20, 10, 10, 10));

        Label lblName = new Label("Họ và tên:");
        lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        txtRegName = new TextField();
        txtRegName.setPromptText("Ví dụ: Pham Van D");
        txtRegName.setStyle("-fx-padding: 8; -fx-font-size: 14px;");

        Label lblPin = new Label("Mã PIN (Mật khẩu):");
        lblPin.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        txtRegPin = new PasswordField();
        txtRegPin.setPromptText("Nhập mã PIN ít nhất 4 số");
        txtRegPin.setStyle("-fx-padding: 8; -fx-font-size: 14px;");

        Label lblDeposit = new Label("Số tiền gửi ban đầu (VNĐ):");
        lblDeposit.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        txtRegDeposit = new TextField("1000000");
        txtRegDeposit.setPromptText("Ví dụ: 1000000");
        txtRegDeposit.setStyle("-fx-padding: 8; -fx-font-size: 14px;");

        lblRegError = new Label();
        lblRegError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        lblRegError.setWrapText(true);

        Button btnRegister = new Button("MỞ TÀI KHOẢN MỚI");
        btnRegister.setMaxWidth(Double.MAX_VALUE);
        btnRegister.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10; -fx-background-radius: 6; -fx-cursor: hand;");
        btnRegister.setOnAction(e -> handleRegister());

        box.getChildren().addAll(lblName, txtRegName, lblPin, txtRegPin, lblDeposit, txtRegDeposit, lblRegError, btnRegister);
        return box;
    }

    private TitledPane createServerSettingsPane() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));

        // Server 1
        HBox s1Box = new HBox(8);
        s1Box.setAlignment(Pos.CENTER_LEFT);
        Label lblS1 = new Label("Server 1:");
        lblS1.setPrefWidth(60);
        txtS1Host = new TextField(rmiManager.getServer1Host());
        txtS1Host.setPrefWidth(120);
        txtS1Port = new TextField(String.valueOf(rmiManager.getServer1Port()));
        txtS1Port.setPrefWidth(60);
        lblServer1Status = new Label("Đang kiểm tra...");
        lblServer1Status.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        s1Box.getChildren().addAll(lblS1, txtS1Host, txtS1Port, lblServer1Status);

        // Server 2
        HBox s2Box = new HBox(8);
        s2Box.setAlignment(Pos.CENTER_LEFT);
        Label lblS2 = new Label("Server 2:");
        lblS2.setPrefWidth(60);
        txtS2Host = new TextField(rmiManager.getServer2Host());
        txtS2Host.setPrefWidth(120);
        txtS2Port = new TextField(String.valueOf(rmiManager.getServer2Port()));
        txtS2Port.setPrefWidth(60);
        lblServer2Status = new Label("Đang kiểm tra...");
        lblServer2Status.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        s2Box.getChildren().addAll(lblS2, txtS2Host, txtS2Port, lblServer2Status);

        Button btnApply = new Button("Lưu & Kiểm tra kết nối");
        btnApply.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 12px;");
        btnApply.setOnAction(e -> {
            try {
                rmiManager.configureServers(
                        txtS1Host.getText().trim(),
                        Integer.parseInt(txtS1Port.getText().trim()),
                        txtS2Host.getText().trim(),
                        Integer.parseInt(txtS2Port.getText().trim())
                );
                checkServerStatusesAsync();
            } catch (Exception ex) {
                lblLoginError.setText("Cổng server không hợp lệ!");
            }
        });

        box.getChildren().addAll(s1Box, s2Box, btnApply);

        TitledPane pane = new TitledPane("Cấu hình IP / Port của 2 Máy Chủ RMI", box);
        pane.setStyle("-fx-font-size: 12px;");
        return pane;
    }

    private void checkServerStatusesAsync() {
        new Thread(() -> {
            boolean s1Online = rmiManager.checkServerOnline(1);
            boolean s2Online = rmiManager.checkServerOnline(2);

            Platform.runLater(() -> {
                if (s1Online) {
                    lblServer1Status.setText("● Online");
                    lblServer1Status.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
                } else {
                    lblServer1Status.setText("○ Offline");
                    lblServer1Status.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
                }

                if (s2Online) {
                    lblServer2Status.setText("● Online");
                    lblServer2Status.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
                } else {
                    lblServer2Status.setText("○ Offline");
                    lblServer2Status.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
                }
            });
        }).start();
    }

    private void handleLogin() {
        lblLoginError.setText("");
        String acc = txtLoginAcc.getText().trim();
        String pin = txtLoginPin.getText().trim();

        if (acc.isEmpty() || pin.isEmpty()) {
            lblLoginError.setText("Vui lòng điền đầy đủ số tài khoản và mã PIN!");
            return;
        }

        new Thread(() -> {
            try {
                Account account = rmiManager.login(acc, pin);
                Platform.runLater(() -> {
                    if (account != null) {
                        rmiManager.setCurrentAccount(account);
                        app.showDashboard();
                    } else {
                        lblLoginError.setText("Số tài khoản hoặc mã PIN không đúng!");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> lblLoginError.setText("Lỗi kết nối Server: " + e.getMessage()));
            }
        }).start();
    }

    private void handleRegister() {
        lblRegError.setText("");
        String name = txtRegName.getText().trim();
        String pin = txtRegPin.getText().trim();
        String depositStr = txtRegDeposit.getText().trim();

        if (name.isEmpty() || pin.isEmpty()) {
            lblRegError.setText("Vui lòng nhập họ tên và mã PIN!");
            return;
        }

        double deposit;
        try {
            deposit = Double.parseDouble(depositStr);
            if (deposit < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            lblRegError.setText("Số tiền gửi ban đầu không hợp lệ!");
            return;
        }

        new Thread(() -> {
            try {
                Account created = rmiManager.register(name, pin, deposit);
                Platform.runLater(() -> {
                    if (created != null) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Đăng Ký Thành Công");
                        alert.setHeaderText("Tài khoản của bạn đã được tạo thành công!");
                        alert.setContentText("Số tài khoản mới: " + created.getAccountNumber() +
                                "\nChủ tài khoản: " + created.getOwnerName() +
                                "\nSố dư ban đầu: " + FormatUtil.formatCurrency(created.getBalance()));
                        alert.showAndWait();

                        txtLoginAcc.setText(created.getAccountNumber());
                        txtLoginPin.setText(pin);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> lblRegError.setText("Lỗi tạo tài khoản: " + e.getMessage()));
            }
        }).start();
    }
}
