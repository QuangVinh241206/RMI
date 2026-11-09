package com.bank.common.service;

import com.bank.common.model.SyncPayload;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SyncService extends Remote {

    /**
     * Replicate a real-time event (account creation, deposit, withdraw, transfer).
     */
    boolean receiveReplication(SyncPayload payload) throws RemoteException;

    /**
     * Request full snapshot from peer for initial or catch-up synchronization.
     */
    SyncPayload requestFullSyncSnapshot() throws RemoteException;

    /**
     * Heartbeat / Ping method between servers.
     */
    boolean pingSync(String fromServerId) throws RemoteException;
}
