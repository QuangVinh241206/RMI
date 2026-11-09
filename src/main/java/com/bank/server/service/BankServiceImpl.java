package com.bank.server.service;

import com.bank.common.model.*;
import com.bank.common.service.BankService;
import com.bank.common.service.SyncService;
import com.bank.server.dao.DatabaseManager;
import com.bank.server.sync.PeerSyncManager;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class BankServiceImpl extends UnicastRemoteObject implements BankService, SyncService {
    private static final long serialVersionUID = 1L;

    private final String serverId;
    private final String host;
    private final int port;
    private final DatabaseManager dbManager;
    private final PeerSyncManager peerSyncManager;
    private final long startTime;

    public BankServiceImpl(String serverId, String host, int port, DatabaseManager dbManager, PeerSyncManager peerSyncManager) throws RemoteException {
        super(port);
        this.serverId = serverId;
        this.host = host;
        this.port = port;
        this.dbManager = dbManager;
        this.peerSyncManager = peerSyncManager;
        this.startTime = System.currentTimeMillis();
    }

    // ================= CLIENT METHODS =================

    @Override
    public Account login(String accountNumber, String pin) throws RemoteException {
        Account acc = dbManager.getAccount(accountNumber);
        if (acc != null && acc.getPin().equals(pin)) {
            return acc;
        }
        return null;
    }

    @Override
    public Account register(String ownerName, String pin, double initialDeposit) throws RemoteException {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new RemoteException("Tên chủ tài khoản không được để trống!");
        }
        if (pin == null || pin.trim().length() < 4) {
            throw new RemoteException("Mã PIN phải có ít nhất 4 ký tự!");
        }
        if (initialDeposit < 0) {
            throw new RemoteException("Số tiền nạp ban đầu không hợp lệ!");
        }

        Account account = dbManager.createAccount(ownerName.trim(), pin.trim(), initialDeposit);
        if (account != null) {
            // Replicate account creation to peer server
            peerSyncManager.broadcastUpdate(SyncPayload.forAccountCreated(account, serverId));
        }
        return account;
    }

    @Override
    public Account getAccount(String accountNumber) throws RemoteException {
        return dbManager.getAccount(accountNumber);
    }

    @Override
    public TransferResult transfer(String fromAccount, String toAccount, double amount, String pin, String message) throws RemoteException {
        TransferResult result = dbManager.executeTransfer(fromAccount, toAccount, amount, pin, message, serverId);
        if (result.isSuccess()) {
            Account sender = dbManager.getAccount(fromAccount);
            Account receiver = dbManager.getAccount(toAccount);
            // Broadcast transaction and updated accounts to peer
            peerSyncManager.broadcastUpdate(SyncPayload.forTransaction(result.getTransaction(), sender, receiver, serverId));
        }
        return result;
    }

    @Override
    public TransferResult deposit(String accountNumber, double amount) throws RemoteException {
        TransferResult result = dbManager.executeDeposit(accountNumber, amount, serverId);
        if (result.isSuccess()) {
            Account account = dbManager.getAccount(accountNumber);
            peerSyncManager.broadcastUpdate(SyncPayload.forTransaction(result.getTransaction(), null, account, serverId));
        }
        return result;
    }

    @Override
    public TransferResult withdraw(String accountNumber, double amount, String pin) throws RemoteException {
        TransferResult result = dbManager.executeWithdraw(accountNumber, amount, pin, serverId);
        if (result.isSuccess()) {
            Account account = dbManager.getAccount(accountNumber);
            peerSyncManager.broadcastUpdate(SyncPayload.forTransaction(result.getTransaction(), account, null, serverId));
        }
        return result;
    }

    @Override
    public List<Transaction> getTransactionHistory(String accountNumber) throws RemoteException {
        return dbManager.getTransactionsForAccount(accountNumber);
    }

    @Override
    public String getServerId() throws RemoteException {
        return serverId;
    }

    @Override
    public ServerStatus getServerStatus() throws RemoteException {
        return new ServerStatus(
                serverId,
                host,
                port,
                true,
                "PEER",
                peerSyncManager.isPeerConnected(),
                dbManager.getAccountCount(),
                dbManager.getTransactionCount(),
                System.currentTimeMillis() - startTime
        );
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    // ================= PEER SYNC METHODS =================

    @Override
    public boolean receiveReplication(SyncPayload payload) throws RemoteException {
        if (payload == null) return false;
        return peerSyncManager.handleIncomingReplication(payload);
    }

    @Override
    public SyncPayload requestFullSyncSnapshot() throws RemoteException {
        return peerSyncManager.createFullSnapshotPayload();
    }

    @Override
    public boolean pingSync(String fromServerId) throws RemoteException {
        return true;
    }
}
