package com.bank.server.dao;

import com.bank.common.model.Account;
import com.bank.common.model.Transaction;
import com.bank.common.model.TransactionType;
import com.bank.common.model.TransferResult;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {
    private final String dbUrl;

    public DatabaseManager(String dbFileName) {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.dbUrl = "jdbc:sqlite:data/" + dbFileName;
        initDatabase();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private void initDatabase() {
        String createAccountsTable = """
            CREATE TABLE IF NOT EXISTS accounts (
                account_number TEXT PRIMARY KEY,
                owner_name TEXT NOT NULL,
                pin TEXT NOT NULL,
                balance REAL NOT NULL,
                version INTEGER NOT NULL,
                last_updated INTEGER NOT NULL
            );
        """;

        String createTransactionsTable = """
            CREATE TABLE IF NOT EXISTS transactions (
                id TEXT PRIMARY KEY,
                sender TEXT,
                receiver TEXT,
                amount REAL NOT NULL,
                type TEXT NOT NULL,
                message TEXT,
                timestamp INTEGER NOT NULL,
                server_id TEXT NOT NULL,
                status TEXT NOT NULL
            );
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createAccountsTable);
            stmt.execute(createTransactionsTable);

            // Seed initial demo data if accounts table is empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM accounts");
            if (rs.next() && rs.getInt(1) == 0) {
                seedDemoAccounts(conn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void seedDemoAccounts(Connection conn) throws SQLException {
        String insertSql = "INSERT INTO accounts (account_number, owner_name, pin, balance, version, last_updated) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            // Account 1
            pstmt.setString(1, "100001");
            pstmt.setString(2, "Nguyen Van A");
            pstmt.setString(3, "123456");
            pstmt.setDouble(4, 10000000.0); // 10,000,000 VND
            pstmt.setLong(5, 1);
            pstmt.setLong(6, System.currentTimeMillis());
            pstmt.executeUpdate();

            // Account 2
            pstmt.setString(1, "100002");
            pstmt.setString(2, "Tran Thi B");
            pstmt.setString(3, "123456");
            pstmt.setDouble(4, 5000000.0); // 5,000,000 VND
            pstmt.setLong(5, 1);
            pstmt.setLong(6, System.currentTimeMillis());
            pstmt.executeUpdate();

            // Account 3
            pstmt.setString(1, "100003");
            pstmt.setString(2, "Le Van C");
            pstmt.setString(3, "123456");
            pstmt.setDouble(4, 2500000.0); // 2,500,000 VND
            pstmt.setLong(5, 1);
            pstmt.setLong(6, System.currentTimeMillis());
            pstmt.executeUpdate();
        }
    }

    public synchronized Account getAccount(String accountNumber) {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToAccount(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized Account createAccount(String ownerName, String pin, double initialBalance) {
        String accNumber = generateNextAccountNumber();
        Account account = new Account(accNumber, ownerName, pin, initialBalance);
        account.setLastUpdated(System.currentTimeMillis());
        account.setVersion(1);

        String sql = "INSERT INTO accounts (account_number, owner_name, pin, balance, version, last_updated) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account.getAccountNumber());
            pstmt.setString(2, account.getOwnerName());
            pstmt.setString(3, account.getPin());
            pstmt.setDouble(4, account.getBalance());
            pstmt.setLong(5, account.getVersion());
            pstmt.setLong(6, account.getLastUpdated());
            pstmt.executeUpdate();
            return account;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized void upsertAccount(Account account) {
        String sql = """
            INSERT INTO accounts (account_number, owner_name, pin, balance, version, last_updated)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(account_number) DO UPDATE SET
                owner_name = excluded.owner_name,
                pin = excluded.pin,
                balance = excluded.balance,
                version = excluded.version,
                last_updated = excluded.last_updated;
        """;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account.getAccountNumber());
            pstmt.setString(2, account.getOwnerName());
            pstmt.setString(3, account.getPin());
            pstmt.setDouble(4, account.getBalance());
            pstmt.setLong(5, account.getVersion());
            pstmt.setLong(6, account.getLastUpdated());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void saveTransaction(Transaction txn) {
        String sql = """
            INSERT INTO transactions (id, sender, receiver, amount, type, message, timestamp, server_id, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO NOTHING;
        """;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, txn.getTransactionId());
            pstmt.setString(2, txn.getSenderAccount());
            pstmt.setString(3, txn.getReceiverAccount());
            pstmt.setDouble(4, txn.getAmount());
            pstmt.setString(5, txn.getType().name());
            pstmt.setString(6, txn.getMessage());
            pstmt.setLong(7, txn.getTimestamp());
            pstmt.setString(8, txn.getProcessedByServer());
            pstmt.setString(9, txn.getStatus());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized TransferResult executeTransfer(String fromAcc, String toAcc, double amount, String pin, String message, String serverId) {
        if (fromAcc.equals(toAcc)) {
            return TransferResult.failure("Không thể chuyển tiền cho chính mình!");
        }
        if (amount <= 0) {
            return TransferResult.failure("Số tiền chuyển phải lớn hơn 0!");
        }

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                Account sender = getAccountWithConn(conn, fromAcc);
                if (sender == null) {
                    conn.rollback();
                    return TransferResult.failure("Tài khoản gửi không tồn tại!");
                }
                if (!sender.getPin().equals(pin)) {
                    conn.rollback();
                    return TransferResult.failure("Mã PIN không chính xác!");
                }
                if (sender.getBalance() < amount) {
                    conn.rollback();
                    return TransferResult.failure("Số dư không đủ để thực hiện giao dịch! (Hiện có: " + String.format("%,.0f", sender.getBalance()) + " VNĐ)");
                }

                Account receiver = getAccountWithConn(conn, toAcc);
                if (receiver == null) {
                    conn.rollback();
                    return TransferResult.failure("Tài khoản thụ hưởng không tồn tại!");
                }

                // Update balances
                double newSenderBalance = sender.getBalance() - amount;
                double newReceiverBalance = receiver.getBalance() + amount;
                long now = System.currentTimeMillis();

                updateBalanceWithConn(conn, fromAcc, newSenderBalance, sender.getVersion() + 1, now);
                updateBalanceWithConn(conn, toAcc, newReceiverBalance, receiver.getVersion() + 1, now);

                // Create transaction
                String txnId = "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                Transaction txn = new Transaction(txnId, fromAcc, toAcc, amount, TransactionType.TRANSFER, message, now, serverId, "SUCCESS");
                insertTransactionWithConn(conn, txn);

                conn.commit();
                return TransferResult.success("Chuyển tiền thành công!", txn, newSenderBalance, serverId);
            } catch (Exception ex) {
                conn.rollback();
                return TransferResult.failure("Lỗi hệ thống khi giao dịch: " + ex.getMessage());
            }
        } catch (SQLException e) {
            return TransferResult.failure("Lỗi kết nối database: " + e.getMessage());
        }
    }

    public synchronized TransferResult executeDeposit(String accNo, double amount, String serverId) {
        if (amount <= 0) {
            return TransferResult.failure("Số tiền nạp phải lớn hơn 0!");
        }

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                Account account = getAccountWithConn(conn, accNo);
                if (account == null) {
                    conn.rollback();
                    return TransferResult.failure("Tài khoản không tồn tại!");
                }

                double newBalance = account.getBalance() + amount;
                long now = System.currentTimeMillis();
                updateBalanceWithConn(conn, accNo, newBalance, account.getVersion() + 1, now);

                String txnId = "DEP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                Transaction txn = new Transaction(txnId, "SYSTEM", accNo, amount, TransactionType.DEPOSIT, "Nạp tiền vào tài khoản", now, serverId, "SUCCESS");
                insertTransactionWithConn(conn, txn);

                conn.commit();
                return TransferResult.success("Nạp tiền thành công!", txn, newBalance, serverId);
            } catch (Exception ex) {
                conn.rollback();
                return TransferResult.failure("Lỗi khi nạp tiền: " + ex.getMessage());
            }
        } catch (SQLException e) {
            return TransferResult.failure("Lỗi kết nối database: " + e.getMessage());
        }
    }

    public synchronized TransferResult executeWithdraw(String accNo, double amount, String pin, String serverId) {
        if (amount <= 0) {
            return TransferResult.failure("Số tiền rút phải lớn hơn 0!");
        }

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                Account account = getAccountWithConn(conn, accNo);
                if (account == null) {
                    conn.rollback();
                    return TransferResult.failure("Tài khoản không tồn tại!");
                }
                if (!account.getPin().equals(pin)) {
                    conn.rollback();
                    return TransferResult.failure("Mã PIN không chính xác!");
                }
                if (account.getBalance() < amount) {
                    conn.rollback();
                    return TransferResult.failure("Số dư không đủ! (Hiện có: " + String.format("%,.0f", account.getBalance()) + " VNĐ)");
                }

                double newBalance = account.getBalance() - amount;
                long now = System.currentTimeMillis();
                updateBalanceWithConn(conn, accNo, newBalance, account.getVersion() + 1, now);

                String txnId = "WTH-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                Transaction txn = new Transaction(txnId, accNo, "ATM/CASH", amount, TransactionType.WITHDRAW, "Rút tiền từ tài khoản", now, serverId, "SUCCESS");
                insertTransactionWithConn(conn, txn);

                conn.commit();
                return TransferResult.success("Rút tiền thành công!", txn, newBalance, serverId);
            } catch (Exception ex) {
                conn.rollback();
                return TransferResult.failure("Lỗi khi rút tiền: " + ex.getMessage());
            }
        } catch (SQLException e) {
            return TransferResult.failure("Lỗi kết nối database: " + e.getMessage());
        }
    }

    public synchronized List<Transaction> getTransactionsForAccount(String accNo) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE sender = ? OR receiver = ? ORDER BY timestamp DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accNo);
            pstmt.setString(2, accNo);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized List<Account> getAllAccounts() {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT * FROM accounts ORDER BY account_number ASC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToAccount(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized List<Transaction> getAllTransactions() {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY timestamp DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized void applyFullSnapshot(List<Account> accounts, List<Transaction> transactions) {
        if (accounts == null || transactions == null) return;
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (Account acc : accounts) {
                    upsertAccountWithConn(conn, acc);
                }
                for (Transaction txn : transactions) {
                    insertTransactionWithConn(conn, txn);
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized int getAccountCount() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM accounts")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public synchronized int getTransactionCount() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM transactions")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private Account getAccountWithConn(Connection conn, String accNo) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accNo);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToAccount(rs);
            }
        }
        return null;
    }

    private void updateBalanceWithConn(Connection conn, String accNo, double newBalance, long newVersion, long now) throws SQLException {
        String sql = "UPDATE accounts SET balance = ?, version = ?, last_updated = ? WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newBalance);
            pstmt.setLong(2, newVersion);
            pstmt.setLong(3, now);
            pstmt.setString(4, accNo);
            pstmt.executeUpdate();
        }
    }

    private void upsertAccountWithConn(Connection conn, Account account) throws SQLException {
        String sql = """
            INSERT INTO accounts (account_number, owner_name, pin, balance, version, last_updated)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(account_number) DO UPDATE SET
                owner_name = excluded.owner_name,
                pin = excluded.pin,
                balance = excluded.balance,
                version = excluded.version,
                last_updated = excluded.last_updated;
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account.getAccountNumber());
            pstmt.setString(2, account.getOwnerName());
            pstmt.setString(3, account.getPin());
            pstmt.setDouble(4, account.getBalance());
            pstmt.setLong(5, account.getVersion());
            pstmt.setLong(6, account.getLastUpdated());
            pstmt.executeUpdate();
        }
    }

    private void insertTransactionWithConn(Connection conn, Transaction txn) throws SQLException {
        String sql = """
            INSERT INTO transactions (id, sender, receiver, amount, type, message, timestamp, server_id, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO NOTHING;
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, txn.getTransactionId());
            pstmt.setString(2, txn.getSenderAccount());
            pstmt.setString(3, txn.getReceiverAccount());
            pstmt.setDouble(4, txn.getAmount());
            pstmt.setString(5, txn.getType().name());
            pstmt.setString(6, txn.getMessage());
            pstmt.setLong(7, txn.getTimestamp());
            pstmt.setString(8, txn.getProcessedByServer());
            pstmt.setString(9, txn.getStatus());
            pstmt.executeUpdate();
        }
    }

    private String generateNextAccountNumber() {
        String sql = "SELECT MAX(CAST(account_number AS INTEGER)) FROM accounts";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int max = rs.getInt(1);
                if (max >= 100000) {
                    return String.valueOf(max + 1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "100001";
    }

    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        return new Account(
                rs.getString("account_number"),
                rs.getString("owner_name"),
                rs.getString("pin"),
                rs.getDouble("balance"),
                rs.getLong("version"),
                rs.getLong("last_updated")
        );
    }

    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getString("id"),
                rs.getString("sender"),
                rs.getString("receiver"),
                rs.getDouble("amount"),
                TransactionType.valueOf(rs.getString("type")),
                rs.getString("message"),
                rs.getLong("timestamp"),
                rs.getString("server_id"),
                rs.getString("status")
        );
    }
}
