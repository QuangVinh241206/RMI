@chcp 65001 >nul
@echo off
title RMI SERVER 1 (Port 1099)
cd /d "%~dp0"

echo =======================================================
echo    KHOI DONG MAY CHU RMI 1 (SERVER 1)
echo =======================================================
echo Port: 1099 ^| Peer: 1100
echo.

java --module-path "target\lib;target\classes" --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "target\banking-rmi-sync-1.0.0.jar;target\lib\*" com.bank.server.ServerApp --id SERVER_1 --port 1099 --peerPort 1100 --db bank_server1.db

pause
