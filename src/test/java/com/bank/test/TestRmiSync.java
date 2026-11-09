package com.bank.test;

import com.bank.common.model.Account;
import com.bank.common.model.TransferResult;
import com.bank.common.service.BankService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestRmiSync {
    public static void main(String[] args) throws Exception {
        System.out.println("=== KIỂM TRA ĐỒNG BỘ 2 SERVER QUA RMI ===");

        // Connect to Server 1
        Registry reg1 = LocateRegistry.getRegistry("localhost", 1099);
        BankService service1 = (BankService) reg1.lookup("BankService");

        // Connect to Server 2
        Registry reg2 = LocateRegistry.getRegistry("localhost", 1100);
        BankService service2 = (BankService) reg2.lookup("BankService");

        System.out.println("Kết nối thành công đến Server 1 (" + service1.getServerId() + ") và Server 2 (" + service2.getServerId() + ")");

        // Initial balance on Server 1
        Account acc1OnServer1 = service1.getAccount("100001");
        Account acc2OnServer1 = service1.getAccount("100002");
        System.out.println("Server 1 - Trước giao dịch:");
        System.out.println("  100001 (" + acc1OnServer1.getOwnerName() + "): " + acc1OnServer1.getBalance() + " VNĐ");
        System.out.println("  100002 (" + acc2OnServer1.getOwnerName() + "): " + acc2OnServer1.getBalance() + " VNĐ");

        // Perform Transfer 1,000,000 VND on SERVER 1
        System.out.println("\n>>> Đang chuyển 1,000,000 VNĐ từ 100001 sang 100002 TRÊN SERVER 1...");
        TransferResult result = service1.transfer("100001", "100002", 1000000.0, "123456", "Test RMI 2-Server Sync");
        System.out.println("Kết quả chuyển tiền: " + result.getMessage() + " (Mã GD: " + result.getTransaction().getTransactionId() + ")");

        // Wait 1 second for RMI propagation
        Thread.sleep(1000);

        // Check balance on SERVER 2
        Account acc1OnServer2 = service2.getAccount("100001");
        Account acc2OnServer2 = service2.getAccount("100002");
        System.out.println("\nServer 2 - Sau giao dịch (Kiểm tra đồng bộ):");
        System.out.println("  100001 (" + acc1OnServer2.getOwnerName() + "): " + acc1OnServer2.getBalance() + " VNĐ (Version: " + acc1OnServer2.getVersion() + ")");
        System.out.println("  100002 (" + acc2OnServer2.getOwnerName() + "): " + acc2OnServer2.getBalance() + " VNĐ (Version: " + acc2OnServer2.getVersion() + ")");

        boolean syncPassed = (acc1OnServer2.getBalance() == 9000000.0) && (acc2OnServer2.getBalance() == 6000000.0);
        if (syncPassed) {
            System.out.println("\n>>> XÁC NHẬN: DỮ LIỆU ĐÃ ĐỒNG BỘ HOÀN TOÀN SANG SERVER 2 THÀNH CÔNG RỰC RỠ! <<<");
        } else {
            System.out.println("\n>>> CẢNH BÁO: Dữ liệu chưa đồng bộ chính xác!");
        }
    }
}
