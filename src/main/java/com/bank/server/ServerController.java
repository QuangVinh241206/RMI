package com.bank.server;

import com.bank.common.model.Account;
import com.bank.common.model.Transaction;
import com.bank.server.dao.DatabaseManager;
import com.bank.server.service.BankServiceImpl;
import com.bank.server.sync.PeerSyncManager;
import com.bank.server.sync.SyncListener;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class ServerController implements SyncListener {
    private final ServerConfig config;
    private DatabaseManager dbManager;
    private PeerSyncManager peerSyncManager;
    private BankServiceImpl bankService;
    private Registry registry;
    private boolean isRunning = false;
    private SyncListener uiListener;

    public ServerController(ServerConfig config) {
        this.config = config;
    }

    public void setUiListener(SyncListener uiListener) {
        this.uiListener = uiListener;
    }

    public synchronized void startServer() throws Exception {
        if (isRunning) return;

        // Ensure java.rmi.server.hostname is set for multi-machine networking
        if (config.getHost() != null && !config.getHost().isEmpty()) {
            System.setProperty("java.rmi.server.hostname", config.getHost());
        }

        dbManager = new DatabaseManager(config.getDbFile());
        peerSyncManager = new PeerSyncManager(config.getServerId(), config.getPeerHost(), config.getPeerPort(), dbManager);
        peerSyncManager.addListener(this);

        bankService = new BankServiceImpl(config.getServerId(), config.getHost(), config.getPort(), dbManager, peerSyncManager);

        try {
            registry = LocateRegistry.createRegistry(config.getPort());
            onLog("INFO", "Tạo mới RMI Registry tại cổng " + config.getPort());
        } catch (Exception e) {
            registry = LocateRegistry.getRegistry(config.getPort());
            onLog("INFO", "Sử dụng RMI Registry đã có tại cổng " + config.getPort());
        }

        registry.rebind("BankService", bankService);
        registry.rebind("SyncService", bankService);

        peerSyncManager.start();
        isRunning = true;
        onLog("SUCCESS", "Server [" + config.getServerId() + "] đã khởi động thành công trên cổng " + config.getPort() + "!");
    }

    public synchronized void stopServer() {
        if (!isRunning) return;

        if (peerSyncManager != null) {
            peerSyncManager.stop();
        }

        try {
            if (registry != null && bankService != null) {
                registry.unbind("BankService");
                registry.unbind("SyncService");
                UnicastRemoteObject.unexportObject(bankService, true);
            }
        } catch (Exception e) {
            onLog("WARNING", "Lỗi khi unbind services: " + e.getMessage());
        }

        isRunning = false;
        onLog("INFO", "Server [" + config.getServerId() + "] đã dừng hoạt động.");
    }

    public void triggerManualSync() {
        if (peerSyncManager != null) {
            peerSyncManager.manualSync();
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public ServerConfig getConfig() {
        return config;
    }

    public DatabaseManager getDbManager() {
        return dbManager;
    }

    public PeerSyncManager getPeerSyncManager() {
        return peerSyncManager;
    }

    public List<Account> getAccounts() {
        return dbManager != null ? dbManager.getAllAccounts() : List.of();
    }

    public List<Transaction> getTransactions() {
        return dbManager != null ? dbManager.getAllTransactions() : List.of();
    }

    @Override
    public void onLog(String level, String message) {
        if (uiListener != null) {
            uiListener.onLog(level, message);
        }
    }

    @Override
    public void onPeerStatusChanged(boolean connected, String message) {
        if (uiListener != null) {
            uiListener.onPeerStatusChanged(connected, message);
        }
    }

    @Override
    public void onDataChanged(Account account, Transaction transaction) {
        if (uiListener != null) {
            uiListener.onDataChanged(account, transaction);
        }
    }
}
