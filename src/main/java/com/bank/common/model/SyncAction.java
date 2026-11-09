package com.bank.common.model;

import java.io.Serializable;

public enum SyncAction implements Serializable {
    ACCOUNT_CREATED("Tạo tài khoản"),
    ACCOUNT_UPDATED("Cập nhật tài khoản"),
    TRANSACTION_SYNC("Đồng bộ giao dịch"),
    FULL_SYNC("Đồng bộ toàn bộ");

    private final String description;

    SyncAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
