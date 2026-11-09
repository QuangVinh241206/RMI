@chcp 65001 >nul
@echo off
title RMI SERVER 1 (HEADLESS CONSOLE)
cd /d "%~dp0"

echo =======================================================
echo    KHOI DONG SERVER 1 (CHE DO CONSOLE/HEADLESS)
echo =======================================================
echo.

java --module-path "target\lib;target\classes" --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "target\banking-rmi-sync-1.0.0.jar;target\lib\*" com.bank.server.ServerMain --headless --id SERVER_1 --port 1099 --peerPort 1100 --db bank_server1.db

pause
