@chcp 65001 >nul
@echo off
title RMI SERVER 2 (Port 1100)
cd /d "%~dp0"

echo =======================================================
echo    KHOI DONG MAY CHU RMI 2 (SERVER 2)
echo =======================================================
echo Port: 1100 ^| Peer: 1099
echo.

java --module-path "target\lib;target\classes" --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "target\banking-rmi-sync-1.0.0.jar;target\lib\*" com.bank.server.ServerApp --id SERVER_2 --port 1100 --peerPort 1099 --db bank_server2.db

pause
