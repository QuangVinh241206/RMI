@chcp 65001 >nul
@echo off
title RMI BANK CLIENT - CHUYEN TIEN
cd /d "%~dp0"

echo =======================================================
echo    KHOI DONG UNG DUNG KHACH (BANK CLIENT GUI)
echo =======================================================
echo.

java --module-path "target\lib;target\classes" --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "target\banking-rmi-sync-1.0.0.jar;target\lib\*" com.bank.client.ClientMain

pause
