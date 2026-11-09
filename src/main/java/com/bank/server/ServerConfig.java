package com.bank.server;

public class ServerConfig {
    private String serverId = "SERVER_1";
    private String host = "localhost";
    private int port = 1099;
    private String peerHost = "localhost";
    private int peerPort = 1100;
    private String dbFile = "bank_server1.db";

    public ServerConfig() {}

    public ServerConfig(String serverId, String host, int port, String peerHost, int peerPort, String dbFile) {
        this.serverId = serverId;
        this.host = host;
        this.port = port;
        this.peerHost = peerHost;
        this.peerPort = peerPort;
        this.dbFile = dbFile;
    }

    public static ServerConfig createServer1() {
        String lanIp = com.bank.server.util.NetworkUtil.getLocalLanIp();
        return new ServerConfig("SERVER_1", lanIp, 1099, "localhost", 1100, "bank_server1.db");
    }

    public static ServerConfig createServer2() {
        String lanIp = com.bank.server.util.NetworkUtil.getLocalLanIp();
        return new ServerConfig("SERVER_2", lanIp, 1100, "localhost", 1099, "bank_server2.db");
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

    public String getPeerHost() {
        return peerHost;
    }

    public void setPeerHost(String peerHost) {
        this.peerHost = peerHost;
    }

    public int getPeerPort() {
        return peerPort;
    }

    public void setPeerPort(int peerPort) {
        this.peerPort = peerPort;
    }

    public String getDbFile() {
        return dbFile;
    }

    public void setDbFile(String dbFile) {
        this.dbFile = dbFile;
    }
}
