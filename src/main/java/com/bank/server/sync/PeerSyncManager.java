package com.bank.server.sync;

import com.bank.common.model.Account;
import com.bank.common.model.SyncAction;
import com.bank.common.model.SyncPayload;
import com.bank.common.model.Transaction;
import com.bank.common.service.SyncService;
import com.bank.server.dao.DatabaseManager;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerSyncManager {
    private final String localServerId;
    private final String peerHost;
    private final int peerPort;
    private final DatabaseManager dbManager;
    private final List<SyncListener> listeners = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService syncExecutor = Executors.newFixedThreadPool(4);

    private final AtomicBoolean peerConnected = new AtomicBoolean(false);
    private SyncService peerSyncService = null;
    private boolean initialSyncDone = false;

    public PeerSyncManager(String localServerId, String peerHost, int peerPort, DatabaseManager dbManager) {
        this.localServerId = localServerId;
        this.peerHost = peerHost;
        this.peerPort = peerPort;
        this.dbManager = dbManager;
    }

    public void addListener(SyncListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SyncListener listener) {
        listeners.remove(listener);
    }

    public void start() {
        log("INFO", "Khởi động PeerSyncManager kết nối tới Peer [" + peerHost + ":" + peerPort + "]...");
        // Schedule heartbeat every 3 seconds
        heartbeatExecutor.scheduleWithFixedDelay(this::checkPeerConnection, 1, 3, TimeUnit.SECONDS);
    }

    public void stop() {
        heartbeatExecutor.shutdownNow();
        syncExecutor.shutdown();
        log("INFO", "Đã dừng PeerSyncManager.");
    }

    public boolean isPeerConnected() {
        return peerConnected.get();
    }

    private synchronized void checkPeerConnection() {
        try {
            Registry registry = LocateRegistry.getRegistry(peerHost, peerPort);
            SyncService syncService = (SyncService) registry.lookup("SyncService");
            boolean pingOk = syncService.pingSync(localServerId);

            if (pingOk) {
                this.peerSyncService = syncService;
                boolean prevStatus = peerConnected.getAndSet(true);
                if (!prevStatus) {
                    log("SUCCESS", "Đã kết nối thành công tới Peer Server [" + peerHost + ":" + peerPort + "]!");
                    notifyPeerStatus(true, "Đã kết nối tới Peer (" + peerHost + ":" + peerPort + ")");
                    // Catch-up / initial sync
                    triggerAutoCatchUpSync();
                }
            }
        } catch (Exception e) {
            boolean prevStatus = peerConnected.getAndSet(false);
            this.peerSyncService = null;
            if (prevStatus) {
                log("WARNING", "Mất kết nối với Peer Server [" + peerHost + ":" + peerPort + "]: " + e.getMessage());
                notifyPeerStatus(false, "Không thể kết nối Peer (" + peerHost + ":" + peerPort + ")");
            }
        }
    }

    public void triggerAutoCatchUpSync() {
        syncExecutor.submit(() -> {
            try {
                if (peerSyncService != null) {
                    log("SYNC", "Bắt đầu yêu cầu đồng bộ snapshot toàn bộ từ Peer Server...");
                    SyncPayload snapshot = peerSyncService.requestFullSyncSnapshot();
                    if (snapshot != null) {
                        applyFullSyncSnapshot(snapshot);
                        log("SYNC", "Đồng bộ snapshot thành công! (Accounts: " +
                                (snapshot.getAllAccounts() != null ? snapshot.getAllAccounts().size() : 0) +
                                ", Txns: " +
                                (snapshot.getAllTransactions() != null ? snapshot.getAllTransactions().size() : 0) + ")");
                        initialSyncDone = true;
                    }
                }
            } catch (Exception e) {
                log("ERROR", "Lỗi trong quá trình catch-up sync: " + e.getMessage());
            }
        });
    }

    /**
     * Broadcast an update to the peer server in real-time.
     */
    public void broadcastUpdate(SyncPayload payload) {
        syncExecutor.submit(() -> {
            if (!peerConnected.get() || peerSyncService == null) {
                log("WARNING", "Peer Server đang offline. Dữ liệu sẽ được đồng bộ khi Peer khởi động lại.");
                return;
            }

            try {
                log("SYNC", "Đang gửi gói tin đồng bộ RMI (" + payload.getAction().getDescription() + ") tới Peer...");
                boolean ok = peerSyncService.receiveReplication(payload);
                if (ok) {
                    log("SYNC", "Đồng bộ thành công tới Peer Server (" + payload.getAction().name() + ").");
                } else {
                    log("WARNING", "Peer Server báo lỗi khi nhận đồng bộ.");
                }
            } catch (Exception e) {
                log("ERROR", "Thất bại khi gửi đồng bộ tới Peer: " + e.getMessage());
                peerConnected.set(false);
                notifyPeerStatus(false, "Mất kết nối với Peer: " + e.getMessage());
            }
        });
    }

    /**
     * Handle incoming replication payload received from Peer.
     */
    public synchronized boolean handleIncomingReplication(SyncPayload payload) {
        log("SYNC", "Nhận dữ liệu đồng bộ RMI từ Server [" + payload.getSourceServerId() + "] - Loại: " + payload.getAction());

        try {
            switch (payload.getAction()) {
                case ACCOUNT_CREATED:
                    if (payload.getAccount() != null) {
                        dbManager.upsertAccount(payload.getAccount());
                        log("SYNC", "-> Đã đồng bộ tài khoản mới: " + payload.getAccount().getAccountNumber() + " (" + payload.getAccount().getOwnerName() + ")");
                        notifyDataChanged(payload.getAccount(), null);
                    }
                    break;

                case ACCOUNT_UPDATED:
                    if (payload.getAccount() != null) {
                        dbManager.upsertAccount(payload.getAccount());
                        log("SYNC", "-> Đã cập nhật tài khoản: " + payload.getAccount().getAccountNumber());
                        notifyDataChanged(payload.getAccount(), null);
                    }
                    break;

                case TRANSACTION_SYNC:
                    if (payload.getTransaction() != null) {
                        dbManager.saveTransaction(payload.getTransaction());
                    }
                    if (payload.getAccount() != null) {
                        dbManager.upsertAccount(payload.getAccount());
                    }
                    if (payload.getTargetAccount() != null) {
                        dbManager.upsertAccount(payload.getTargetAccount());
                    }
                    log("SYNC", "-> Đã đồng bộ giao dịch [" + (payload.getTransaction() != null ? payload.getTransaction().getTransactionId() : "") + "] thành công.");
                    notifyDataChanged(payload.getAccount(), payload.getTransaction());
                    break;

                case FULL_SYNC:
                    applyFullSyncSnapshot(payload);
                    break;
            }
            return true;
        } catch (Exception e) {
            log("ERROR", "Lỗi xử lý đồng bộ từ Peer: " + e.getMessage());
            return false;
        }
    }

    public synchronized void applyFullSyncSnapshot(SyncPayload snapshot) {
        if (snapshot == null) return;
        List<Account> accounts = snapshot.getAllAccounts();
        List<Transaction> transactions = snapshot.getAllTransactions();
        dbManager.applyFullSnapshot(accounts, transactions);
        notifyDataChanged(null, null);
    }

    public SyncPayload createFullSnapshotPayload() {
        List<Account> accounts = dbManager.getAllAccounts();
        List<Transaction> transactions = dbManager.getAllTransactions();
        return SyncPayload.forFullSync(accounts, transactions, localServerId);
    }

    public void manualSync() {
        triggerAutoCatchUpSync();
    }

    private void log(String level, String msg) {
        System.out.println("[" + level + "] [" + localServerId + "] " + msg);
        for (SyncListener l : listeners) {
            l.onLog(level, msg);
        }
    }

    private void notifyPeerStatus(boolean connected, String msg) {
        for (SyncListener l : listeners) {
            l.onPeerStatusChanged(connected, msg);
        }
    }

    private void notifyDataChanged(Account account, Transaction transaction) {
        for (SyncListener l : listeners) {
            l.onDataChanged(account, transaction);
        }
    }
}
