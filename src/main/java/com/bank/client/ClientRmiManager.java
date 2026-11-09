package com.bank.client;

import com.bank.common.model.Account;
import com.bank.common.model.ServerStatus;
import com.bank.common.model.Transaction;
import com.bank.common.model.TransferResult;
import com.bank.common.service.BankService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

public class ClientRmiManager {
    private static ClientRmiManager instance;

    private String server1Host = "localhost";
    private int server1Port = 1099;

    private String server2Host = "localhost";
    private int server2Port = 1100;

    private int activeServer = 1; // 1 or 2
    private BankService bankService;
    private Account currentAccount;

    private ClientRmiManager() {}

    public static synchronized ClientRmiManager getInstance() {
        if (instance == null) {
            instance = new ClientRmiManager();
        }
        return instance;
    }

    public void configureServers(String s1Host, int s1Port, String s2Host, int s2Port) {
        this.server1Host = s1Host;
        this.server1Port = s1Port;
        this.server2Host = s2Host;
        this.server2Port = s2Port;
    }

    public synchronized boolean connect(int targetServer) {
        String host = (targetServer == 1) ? server1Host : server2Host;
        int port = (targetServer == 1) ? server1Port : server2Port;

        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            BankService service = (BankService) registry.lookup("BankService");
            service.ping();
            this.bankService = service;
            this.activeServer = targetServer;
            return true;
        } catch (Exception e) {
            System.err.println("Không thể kết nối Server " + targetServer + " (" + host + ":" + port + "): " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean autoConnect() {
        // Try Server 1 first
        if (connect(1)) {
            return true;
        }
        // Failover to Server 2
        System.out.println("Server 1 không phản hồi, tự động chuyển sang Server 2...");
        return connect(2);
    }

    public synchronized boolean switchServer() {
        int nextServer = (activeServer == 1) ? 2 : 1;
        if (connect(nextServer)) {
            // refresh current account data from new server
            if (currentAccount != null) {
                try {
                    Account refreshed = bankService.getAccount(currentAccount.getAccountNumber());
                    if (refreshed != null) {
                        this.currentAccount = refreshed;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }

    public boolean isConnected() {
        if (bankService == null) return false;
        try {
            return bankService.ping();
        } catch (Exception e) {
            return false;
        }
    }

    public Account getCurrentAccount() {
        return currentAccount;
    }

    public void setCurrentAccount(Account currentAccount) {
        this.currentAccount = currentAccount;
    }

    public int getActiveServer() {
        return activeServer;
    }

    public String getActiveServerAddress() {
        return (activeServer == 1) ? (server1Host + ":" + server1Port) : (server2Host + ":" + server2Port);
    }

    public String getServer1Address() {
        return server1Host + ":" + server1Port;
    }

    public String getServer2Address() {
        return server2Host + ":" + server2Port;
    }

    // ================= BANK OPERATIONS WITH FAILOVER =================

    public Account login(String accountNumber, String pin) throws Exception {
        return executeWithFailover(service -> service.login(accountNumber, pin));
    }

    public Account register(String ownerName, String pin, double initialDeposit) throws Exception {
        return executeWithFailover(service -> service.register(ownerName, pin, initialDeposit));
    }

    public Account getAccount(String accountNumber) throws Exception {
        return executeWithFailover(service -> service.getAccount(accountNumber));
    }

    public TransferResult transfer(String fromAccount, String toAccount, double amount, String pin, String message) throws Exception {
        return executeWithFailover(service -> service.transfer(fromAccount, toAccount, amount, pin, message));
    }

    public TransferResult deposit(String accountNumber, double amount) throws Exception {
        return executeWithFailover(service -> service.deposit(accountNumber, amount));
    }

    public TransferResult withdraw(String accountNumber, double amount, String pin) throws Exception {
        return executeWithFailover(service -> service.withdraw(accountNumber, amount, pin));
    }

    public List<Transaction> getTransactionHistory(String accountNumber) throws Exception {
        return executeWithFailover(service -> service.getTransactionHistory(accountNumber));
    }

    public ServerStatus getServerStatus() throws Exception {
        return executeWithFailover(BankService::getServerStatus);
    }

    public boolean checkServerOnline(int serverNum) {
        String host = (serverNum == 1) ? server1Host : server2Host;
        int port = (serverNum == 1) ? server1Port : server2Port;
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            BankService service = (BankService) registry.lookup("BankService");
            return service.ping();
        } catch (Exception e) {
            return false;
        }
    }

    @FunctionalInterface
    public interface RemoteAction<T> {
        T execute(BankService service) throws Exception;
    }

    private synchronized <T> T executeWithFailover(RemoteAction<T> action) throws Exception {
        if (bankService == null) {
            if (!autoConnect()) {
                throw new Exception("Không thể kết nối đến bất kỳ Server nào (Server 1 & Server 2 đều offline).");
            }
        }

        try {
            return action.execute(bankService);
        } catch (Exception ex) {
            System.err.println("Lỗi giao tiếp với Server " + activeServer + ": " + ex.getMessage());
            System.err.println("Đang kích hoạt Failover tự động sang Server đối ứng...");

            // Try the other server
            int fallbackServer = (activeServer == 1) ? 2 : 1;
            if (connect(fallbackServer)) {
                System.out.println("Failover thành công sang Server " + fallbackServer + "! Đang thực hiện lại thao tác...");
                return action.execute(bankService);
            } else {
                throw new Exception("Thao tác thất bại do máy chủ mất kết nối và máy chủ dự phòng cũng offline: " + ex.getMessage());
            }
        }
    }

    public String getServer1Host() {
        return server1Host;
    }

    public int getServer1Port() {
        return server1Port;
    }

    public String getServer2Host() {
        return server2Host;
    }

    public int getServer2Port() {
        return server2Port;
    }
}
