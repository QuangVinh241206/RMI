package com.bank.common.model;

import java.io.Serializable;
import java.util.List;

public class SyncPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    private SyncAction action;
    private Account account;
    private Account targetAccount; // if transfer affects both
    private Transaction transaction;
    private List<Account> allAccounts;
    private List<Transaction> allTransactions;
    private String sourceServerId;
    private long timestamp;

    public SyncPayload() {
        this.timestamp = System.currentTimeMillis();
    }

    public static SyncPayload forAccountCreated(Account account, String sourceServerId) {
        SyncPayload p = new SyncPayload();
        p.setAction(SyncAction.ACCOUNT_CREATED);
        p.setAccount(account);
        p.setSourceServerId(sourceServerId);
        return p;
    }

    public static SyncPayload forTransaction(Transaction transaction, Account sender, Account receiver, String sourceServerId) {
        SyncPayload p = new SyncPayload();
        p.setAction(SyncAction.TRANSACTION_SYNC);
        p.setTransaction(transaction);
        p.setAccount(sender);
        p.setTargetAccount(receiver);
        p.setSourceServerId(sourceServerId);
        return p;
    }

    public static SyncPayload forFullSync(List<Account> accounts, List<Transaction> transactions, String sourceServerId) {
        SyncPayload p = new SyncPayload();
        p.setAction(SyncAction.FULL_SYNC);
        p.setAllAccounts(accounts);
        p.setAllTransactions(transactions);
        p.setSourceServerId(sourceServerId);
        return p;
    }

    public SyncAction getAction() {
        return action;
    }

    public void setAction(SyncAction action) {
        this.action = action;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Account getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(Account targetAccount) {
        this.targetAccount = targetAccount;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public List<Account> getAllAccounts() {
        return allAccounts;
    }

    public void setAllAccounts(List<Account> allAccounts) {
        this.allAccounts = allAccounts;
    }

    public List<Transaction> getAllTransactions() {
        return allTransactions;
    }

    public void setAllTransactions(List<Transaction> allTransactions) {
        this.allTransactions = allTransactions;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public void setSourceServerId(String sourceServerId) {
        this.sourceServerId = sourceServerId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
