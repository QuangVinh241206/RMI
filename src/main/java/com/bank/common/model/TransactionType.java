package com.bank.common.model;

import java.io.Serializable;

public enum TransactionType implements Serializable {
    TRANSFER("Chuyển tiền"),
    DEPOSIT("Nạp tiền"),
    WITHDRAW("Rút tiền");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
