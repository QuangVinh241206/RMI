@chcp 65001 >nul
@echo off
title RMI SERVER 2 (HEADLESS CONSOLE)
cd /d "%~dp0"

echo =======================================================
echo    KHOI DONG SERVER 2 (CHE DO CONSOLE/HEADLESS)
echo =======================================================
echo.

java --module-path "target\lib;target\classes" --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "target\banking-rmi-sync-1.0.0.jar;target\lib\*" com.bank.server.ServerMain --headless --id SERVER_2 --port 1100 --peerPort 1099 --db bank_server2.db

pause
