package com.bank.common.model;

import java.io.Serializable;
import java.util.Objects;

public class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private String accountNumber;
    private String ownerName;
    private String pin; // hashed or 6-digit PIN
    private double balance;
    private long version;
    private long lastUpdated;

    public Account() {}

    public Account(String accountNumber, String ownerName, String pin, double balance) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.pin = pin;
        this.balance = balance;
        this.version = 1;
        this.lastUpdated = System.currentTimeMillis();
    }

    public Account(String accountNumber, String ownerName, String pin, double balance, long version, long lastUpdated) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.pin = pin;
        this.balance = balance;
        this.version = version;
        this.lastUpdated = lastUpdated;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(accountNumber, account.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountNumber='" + accountNumber + '\'' +
                ", ownerName='" + ownerName + '\'' +
                ", balance=" + balance +
                ", version=" + version +
                '}';
    }
}
