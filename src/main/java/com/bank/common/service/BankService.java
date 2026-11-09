package com.bank.common.service;

import com.bank.common.model.Account;
import com.bank.common.model.ServerStatus;
import com.bank.common.model.Transaction;
import com.bank.common.model.TransferResult;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BankService extends Remote {

    /**
     * User login with account number and PIN.
     */
    Account login(String accountNumber, String pin) throws RemoteException;

    /**
     * User registration with initial deposit.
     */
    Account register(String ownerName, String pin, double initialDeposit) throws RemoteException;

    /**
     * Get account details by account number.
     */
    Account getAccount(String accountNumber) throws RemoteException;

    /**
     * Transfer money from sender to receiver.
     */
    TransferResult transfer(String fromAccount, String toAccount, double amount, String pin, String message) throws RemoteException;

    /**
     * Deposit money to account.
     */
    TransferResult deposit(String accountNumber, double amount) throws RemoteException;

    /**
     * Withdraw money from account.
     */
    TransferResult withdraw(String accountNumber, double amount, String pin) throws RemoteException;

    /**
     * Get transaction history for an account.
     */
    List<Transaction> getTransactionHistory(String accountNumber) throws RemoteException;

    /**
     * Get current server ID (e.g. SERVER_1 or SERVER_2).
     */
    String getServerId() throws RemoteException;

    /**
     * Get detailed status of this server and peer sync connection.
     */
    ServerStatus getServerStatus() throws RemoteException;

    /**
     * Simple ping method to check connectivity.
     */
    boolean ping() throws RemoteException;
}
