package com.bank.common.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private String transactionId;
    private String senderAccount;
    private String receiverAccount;
    private double amount;
    private TransactionType type;
    private String message;
    private long timestamp;
    private String processedByServer;
    private String status; // "SUCCESS", "FAILED"

    public Transaction() {}

    public Transaction(String transactionId, String senderAccount, String receiverAccount,
                       double amount, TransactionType type, String message,
                       long timestamp, String processedByServer, String status) {
        this.transactionId = transactionId;
        this.senderAccount = senderAccount;
        this.receiverAccount = receiverAccount;
        this.amount = amount;
        this.type = type;
        this.message = message;
        this.timestamp = timestamp;
        this.processedByServer = processedByServer;
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getSenderAccount() {
        return senderAccount;
    }

    public void setSenderAccount(String senderAccount) {
        this.senderAccount = senderAccount;
    }

    public String getReceiverAccount() {
        return receiverAccount;
    }

    public void setReceiverAccount(String receiverAccount) {
        this.receiverAccount = receiverAccount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getProcessedByServer() {
        return processedByServer;
    }

    public void setProcessedByServer(String processedByServer) {
        this.processedByServer = processedByServer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFormattedTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(formatter);
    }
}
