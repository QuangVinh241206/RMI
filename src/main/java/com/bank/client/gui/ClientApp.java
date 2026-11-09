package com.bank.client.gui;

import com.bank.client.ClientRmiManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

public class ClientApp extends Application {
    private Stage primaryStage;
    private final ClientRmiManager rmiManager = ClientRmiManager.getInstance();

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Parse command line parameters if any
        Parameters params = getParameters();
        List<String> raw = params.getRaw();
        for (int i = 0; i < raw.size(); i++) {
            if ("--s1Host".equalsIgnoreCase(raw.get(i)) && i + 1 < raw.size()) {
                rmiManager.configureServers(raw.get(i + 1), rmiManager.getServer1Port(), rmiManager.getServer2Host(), rmiManager.getServer2Port());
            } else if ("--s1Port".equalsIgnoreCase(raw.get(i)) && i + 1 < raw.size()) {
                rmiManager.configureServers(rmiManager.getServer1Host(), Integer.parseInt(raw.get(i + 1)), rmiManager.getServer2Host(), rmiManager.getServer2Port());
            } else if ("--s2Host".equalsIgnoreCase(raw.get(i)) && i + 1 < raw.size()) {
                rmiManager.configureServers(rmiManager.getServer1Host(), rmiManager.getServer1Port(), raw.get(i + 1), rmiManager.getServer2Port());
            } else if ("--s2Port".equalsIgnoreCase(raw.get(i)) && i + 1 < raw.size()) {
                rmiManager.configureServers(rmiManager.getServer1Host(), rmiManager.getServer1Port(), rmiManager.getServer2Host(), Integer.parseInt(raw.get(i + 1)));
            }
        }

        primaryStage.setTitle("Ứng Dụng Chuyển Tiền Trực Tuyến - RMI Banking Client");
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });

        showLogin();
    }

    public void showLogin() {
        LoginView loginView = new LoginView(this);
        Scene scene = new Scene(loginView, 560, 680);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public void showDashboard() {
        DashboardView dashboardView = new DashboardView(this);
        Scene scene = new Scene(dashboardView, 1100, 720);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
