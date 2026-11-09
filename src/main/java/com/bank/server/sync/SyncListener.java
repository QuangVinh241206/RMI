package com.bank.server.sync;

import com.bank.common.model.Account;
import com.bank.common.model.Transaction;

public interface SyncListener {
    void onLog(String level, String message);
    void onPeerStatusChanged(boolean connected, String message);
    void onDataChanged(Account account, Transaction transaction);
}
