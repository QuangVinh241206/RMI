package com.bank.common.model;

import java.io.Serializable;

public class TransferResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private Transaction transaction;
    private double newBalance;
    private String processedByServer;

    public TransferResult() {}

    public TransferResult(boolean success, String message, Transaction transaction, double newBalance, String processedByServer) {
        this.success = success;
        this.message = message;
        this.transaction = transaction;
        this.newBalance = newBalance;
        this.processedByServer = processedByServer;
    }

    public static TransferResult success(String message, Transaction transaction, double newBalance, String processedByServer) {
        return new TransferResult(true, message, transaction, newBalance, processedByServer);
    }

    public static TransferResult failure(String message) {
        return new TransferResult(false, message, null, 0.0, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public double getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(double newBalance) {
        this.newBalance = newBalance;
    }

    public String getProcessedByServer() {
        return processedByServer;
    }

    public void setProcessedByServer(String processedByServer) {
        this.processedByServer = processedByServer;
    }
}
