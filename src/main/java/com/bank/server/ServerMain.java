package com.bank.server;

import com.bank.server.sync.SyncListener;
import com.bank.common.model.Account;
import com.bank.common.model.Transaction;

public class ServerMain {
    public static void main(String[] args) {
        boolean headless = false;
        for (String arg : args) {
            if ("--headless".equalsIgnoreCase(arg) || "--cli".equalsIgnoreCase(arg)) {
                headless = true;
                break;
            }
        }

        if (headless) {
            runHeadless(args);
        } else {
            ServerApp.main(args);
        }
    }

    private static void runHeadless(String[] args) {
        ServerConfig config = ServerConfig.createServer1();
        for (int i = 0; i < args.length; i++) {
            if ("--server2".equalsIgnoreCase(args[i])) {
                config = ServerConfig.createServer2();
            } else if ("--id".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                config.setServerId(args[i + 1]);
            } else if ("--port".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                config.setPort(Integer.parseInt(args[i + 1]));
            } else if ("--peerPort".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                config.setPeerPort(Integer.parseInt(args[i + 1]));
            } else if ("--peerHost".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                config.setPeerHost(args[i + 1]);
            } else if ("--db".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                config.setDbFile(args[i + 1]);
            }
        }

        System.out.println("=================================================");
        System.out.println("   KHỞI ĐỘNG RMI SERVER (HEADLESS MODE)");
        System.out.println("   Server ID: " + config.getServerId());
        System.out.println("   Port: " + config.getPort());
        System.out.println("   Peer: " + config.getPeerHost() + ":" + config.getPeerPort());
        System.out.println("=================================================");

        ServerController controller = new ServerController(config);
        controller.setUiListener(new SyncListener() {
            @Override
            public void onLog(String level, String message) {
                System.out.println("[" + level + "] " + message);
            }

            @Override
            public void onPeerStatusChanged(boolean connected, String message) {
                System.out.println("[PEER_STATUS] " + (connected ? "CONNECTED: " : "DISCONNECTED: ") + message);
            }

            @Override
            public void onDataChanged(Account account, Transaction transaction) {
                System.out.println("[DATA_UPDATE] Dữ liệu đã thay đổi.");
            }
        });

        try {
            controller.startServer();
            Runtime.getRuntime().addShutdownHook(new Thread(controller::stopServer));
            // Keep alive
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
