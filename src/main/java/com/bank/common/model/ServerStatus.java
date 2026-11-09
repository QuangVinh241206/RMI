package com.bank.common.model;

import java.io.Serializable;

public class ServerStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    private String serverId;
    private String host;
    private int port;
    private boolean isOnline;
    private String peerServerId;
    private boolean peerConnected;
    private int totalAccounts;
    private int totalTransactions;
    private long uptimeMillis;

    public ServerStatus() {}

    public ServerStatus(String serverId, String host, int port, boolean isOnline,
                        String peerServerId, boolean peerConnected,
                        int totalAccounts, int totalTransactions, long uptimeMillis) {
        this.serverId = serverId;
        this.host = host;
        this.port = port;
        this.isOnline = isOnline;
        this.peerServerId = peerServerId;
        this.peerConnected = peerConnected;
        this.totalAccounts = totalAccounts;
        this.totalTransactions = totalTransactions;
        this.uptimeMillis = uptimeMillis;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getPeerServerId() {
        return peerServerId;
    }

    public void setPeerServerId(String peerServerId) {
        this.peerServerId = peerServerId;
    }

    public boolean isPeerConnected() {
        return peerConnected;
    }

    public void setPeerConnected(boolean peerConnected) {
        this.peerConnected = peerConnected;
    }

    public int getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(int totalAccounts) {
        this.totalAccounts = totalAccounts;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public long getUptimeMillis() {
        return uptimeMillis;
    }

    public void setUptimeMillis(long uptimeMillis) {
        this.uptimeMillis = uptimeMillis;
    }
}
